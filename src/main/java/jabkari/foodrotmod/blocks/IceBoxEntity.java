package jabkari.foodrotmod.blocks;

import com.mojang.logging.LogUtils;
import jabkari.foodrotmod.Foodrotmod;
import jabkari.foodrotmod.db.DatabaseService;
import jabkari.foodrotmod.menu.IceBoxMenu;
import jabkari.foodrotmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class IceBoxEntity extends BlockEntity implements MenuProvider {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int INVENTORY_SIZE = 9;
    private final SimpleContainer items = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            IceBoxEntity.this.setChanged();
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(this::createUnSidedHandler);
    private boolean isDbLoaded = false;

    public IceBoxEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModBlockEntities.ICE_BOX_ENTITY.get(), pWorldPosition, pBlockState);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    private IItemHandler createUnSidedHandler() {
        return new InvWrapper(this.items);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    public void load(CompoundTag nbt) {
        super.load(nbt);
        ContainerHelper.loadAllItems(nbt, this.items.getContents());
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        ContainerHelper.saveAllItems(nbt, this.items.getContents());
    }

    public void drops(Level level, BlockPos pos) {
        SimpleContainer inventory = new SimpleContainer(items.getContainerSize());
        for (int i = 0; i < items.getContainerSize(); i++) {
            inventory.setItem(i, items.getItem(i));
        }
        Containers.dropContents(level, pos, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block." + Foodrotmod.MOD_ID + ".ice_box");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new IceBoxMenu(containerId, playerInventory, this.items, this.level, this.worldPosition);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState state, IceBoxEntity entity) {
        entity.loadFromDatabaseIfNeeded();
    }

    private void loadFromDatabaseIfNeeded() {
        if (!level.isClientSide && DatabaseService.isInitialized() && !isDbLoaded) {
            isDbLoaded = true;
            DatabaseService.getIceBoxAsync(level, worldPosition)
                    .thenAccept(optionalIceBoxData -> {
                        optionalIceBoxData.ifPresent(iceBoxData -> {
                            List<ItemStack> dbItems = iceBoxData.getItems().stream()
                                    .map(storedItem -> ItemStack.of(storedItem.getItemNbtData()))
                                    .collect(Collectors.toList());

                            for (int i = 0; i < Math.min(items.getContainerSize(), dbItems.size()); i++) {
                                items.setItem(i, dbItems.get(i));
                            }
                            this.setChanged();
                            LOGGER.debug("Loaded IceBox from DB at {}", worldPosition);
                        });
                        if (optionalIceBoxData.isEmpty()) {
                            LOGGER.warn("No IceBox data found in DB at {}", worldPosition);
                        }
                    })
                    .exceptionally(e -> {
                        LOGGER.error("Error loading IceBox from DB at {}", worldPosition, e);
                        return null;
                    });
        }
    }

    public void saveToDatabaseAsync(HolderLookup.Provider provider) {
        if (!level.isClientSide && DatabaseService.isInitialized()) {
            NonNullList<ItemStack> itemsToSave = NonNullList.withSize(this.items.getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < this.items.getContainerSize(); ++i) {
                itemsToSave.set(i, this.items.getItem(i));
            }
            DatabaseService.saveOrUpdateIceBoxAsync(this.level, this.worldPosition, itemsToSave, provider)
                    .exceptionally(e -> {
                        LOGGER.error("Async DB save failed for IceBox at {}", this.worldPosition, e);
                        return null;
                    });
        } else if (!level.isClientSide && !DatabaseService.isInitialized()) {
            LOGGER.warn("Skipping DB save for IceBox at {}: DatabaseService not initialized.", this.worldPosition);
        }
    }

    public static boolean isRotConsideredFood(ItemStack stack) {
        return stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.CHICKEN) ||
                stack.is(Items.MUTTON) || stack.is(Items.RABBIT) || stack.is(Items.COD) ||
                stack.is(Items.SALMON);
    }

    public Container getContainer() {
        return this.items;
    }
}