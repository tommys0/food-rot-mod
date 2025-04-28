package jabkari.foodrotmod.menu;

import jabkari.foodrotmod.Foodrotmod;
import jabkari.foodrotmod.db.DatabaseService;
import jabkari.foodrotmod.registry.ModMenuTypes; // Your menu type registry
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer; // If needed for checks/casting
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block; // For stillValid check
import jabkari.foodrotmod.registry.ModBlocks; // Your block registry
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;



public class IceBoxMenu extends AbstractContainerMenu {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int CONTAINER_SIZE = 9;
    private final Container container;
    private final Level level; // Need level reference for saving and validity checks
    private final BlockPos pos; // Need position reference for saving and validity checks

    // Primary constructor accepting Container, Level, and BlockPos
    public IceBoxMenu(int containerId, Inventory playerInventory, Container container, Level level, BlockPos pos) {
        super(ModMenuTypes.ICE_BOX_MENU.get(), containerId); // Use your registered MenuType
        checkContainerSize(container, CONTAINER_SIZE);
        this.container = container;
        this.level = level;
        this.pos = pos;
        container.startOpen(playerInventory.player);

        // --- Add Slots ---
        int containerRows = 3;
        int containerCols = 3;
        int slotXStart = 62; // Adjust as needed for your GUI texture
        int slotYStart = 17; // Adjust as needed

        // Ice Box Slots (0-8)
        for (int row = 0; row < containerRows; ++row) {
            for (int col = 0; col < containerCols; ++col) {
                int index = col + row * containerCols;
                this.addSlot(new Slot(container, index, slotXStart + col * 18, slotYStart + row * 18));
            }
        }

        // Player Inventory Slots (9-35)
        int playerInvXStart = 8;
        int playerInvYStart = 84; // Adjust as needed
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = col + row * 9 + 9; // Player inventory slots index start at 9
                this.addSlot(new Slot(playerInventory, index, playerInvXStart + col * 18, playerInvYStart + row * 18));
            }
        }

        // Player Hotbar Slots (36-44)
        int hotbarYStart = 142; // Adjust as needed
        for (int i = 0; i < 9; ++i) {
            int index = i; // Player hotbar slots index 0-8
            this.addSlot(new Slot(playerInventory, index, playerInvXStart + i * 18, hotbarYStart));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            final int iceBoxStart = 0;
            final int iceBoxEnd = CONTAINER_SIZE; // 9
            final int playerInvStart = CONTAINER_SIZE; // 9
            final int playerInvEnd = playerInvStart + 27; // 36
            final int hotbarStart = playerInvEnd; // 36
            final int hotbarEnd = hotbarStart + 9; // 45

            // Moving from Ice Box to Player Inv/Hotbar
            if (index >= iceBoxStart && index < iceBoxEnd) {
                if (!this.moveItemStackTo(slotStack, playerInvStart, hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // Moving from Player Inv/Hotbar to Ice Box
            else if (index >= playerInvStart && index < hotbarEnd) {
                // Optional: Check if item is valid for Ice Box (e.g., is food)
                // if (!IceBoxEntity.isRotConsideredFood(slotStack)) {
                //    return ItemStack.EMPTY;
                // }

                // Try moving to Ice Box first
                if (!this.moveItemStackTo(slotStack, iceBoxStart, iceBoxEnd, false)) {
                    // If failed, handle default player inv/hotbar shift-click logic
                    if (index >= playerInvStart && index < playerInvEnd) { // From main inv to hotbar
                        if (!this.moveItemStackTo(slotStack, hotbarStart, hotbarEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= hotbarStart && index < hotbarEnd) { // From hotbar to main inv
                        if (!this.moveItemStackTo(slotStack, playerInvStart, playerInvEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
            // Failed to move anywhere
            else {
                return ItemStack.EMPTY;
            }


            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged(); // Notify slot of change
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY; // No change occurred
            }

            slot.onTake(player, slotStack); // Trigger pickup event
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        // Check if the block at the stored position is still the correct IceBox block
        // And use the container's validity check (often distance based)
        return this.container.stillValid(player) &&
                level.getBlockState(pos).is(ModBlocks.ICE_BOX.get()); // Check if block type matches
    }

    // Save when menu is closed
    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player); // Notify container

        // Trigger save to DB (use async if desired to avoid potential lag on close)
        if (!this.level.isClientSide && DatabaseService.isInitialized()) {
            // Foodrotmod.LOGGER.debug("Saving IceBox from menu close at {}", this.pos);
            // Get current items from the container
            NonNullList<ItemStack> itemsToSave = NonNullList.withSize(this.container.getContainerSize(), ItemStack.EMPTY);
            for(int i=0; i < this.container.getContainerSize(); ++i) {
                itemsToSave.set(i, this.container.getItem(i));
            }
            // Use async save here as closing menu might happen on main thread
            DatabaseService.saveOrUpdateIceBoxAsync(this.level, this.pos, itemsToSave, this.level.registryAccess())
                    .exceptionally(e -> { // Log errors from async save
                        LOGGER.error("Async DB save failed on menu close for IceBox at {}", this.pos, e);
                        return null;
                    });
        } else if (!this.level.isClientSide && !DatabaseService.isInitialized()) {
            LOGGER.warn("Skipping DB save on menu close for IceBox at {}: DatabaseService not initialized.", this.pos);
        }
    }
}