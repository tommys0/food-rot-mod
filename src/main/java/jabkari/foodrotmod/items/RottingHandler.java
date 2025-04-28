package jabkari.foodrotmod.items;

import jabkari.foodrotmod.blocks.IceBoxEntity;
import jabkari.foodrotmod.db.DatabaseService;
import jabkari.foodrotmod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A completely different approach to handling food rot tracking
 * using a combination of memory cache and modern Forge features
 */
public class RottingHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int ROT_DURATION_TICKS = 100;
    private static final int TRANSFORM_NOW = 0;

    // In-memory cache to track rot timers for items
    // Key: A unique identifier for the item
    // Value: Remaining ticks until rot
    private static final Map<String, Integer> rotTimers = new HashMap<>();

    /**
     * Generates a unique ID for an ItemStack to use as a key in our rot timer map
     */
    private static String getItemKey(ItemStack stack, Object context) {
        // Use a combination of item details and instance hashcode for uniqueness
        String contextId = context instanceof BlockEntity be ?
                "block_" + be.getBlockPos().toString() :
                context instanceof Player p ? "player_" + p.getUUID() :
                        "unknown_" + System.identityHashCode(context);

        return stack.getItem().toString() + "_" + stack.hashCode() + "_" + contextId;
    }

    /**
     * Call this method frequently for items that might be rotting.
     */
    public void handleRotTick(ItemStack stack, Level level, Object context) {
        // 1. Check if the item is susceptible to rot
        if (stack.isEmpty() || !isFoodThatRots(stack)) {
            // If it's no longer food that rots, ensure it's removed from tracking
            String key = getItemKey(stack, context);
            rotTimers.remove(key);
            return;
        }

        // 2. Determine if chilled
        boolean isChilled = false;
        BlockPos containerPos = getContainerPosForItem(stack, level, context);
        if (containerPos != null) {
            isChilled = DatabaseService.isLocationAnIceBox(level, containerPos);
        }

        // Get the key for this item
        String key = getItemKey(stack, context);

        // 3. Handle based on chilled status
        if (isChilled) {
            // If chilled, remove any existing timer
            rotTimers.remove(key);
        } else {
            // Not Chilled: Handle timer logic
            Integer currentTimer = rotTimers.get(key);

            if (currentTimer != null) {
                // Timer exists, decrement it if it's not already at transform now
                if (currentTimer > TRANSFORM_NOW) {
                    rotTimers.put(key, currentTimer - 1);
                }
            } else {
                // Timer doesn't exist, start it
                rotTimers.put(key, ROT_DURATION_TICKS);
            }
        }
    }

    /**
     * Check if an item should transform to rotten
     */
    public static boolean shouldTransformToRotten(ItemStack stack, Object context) {
        String key = getItemKey(stack, context);
        Integer timerValue = rotTimers.get(key);
        return timerValue != null && timerValue <= TRANSFORM_NOW;
    }

    /**
     * Create a rotten item stack from the original
     */
    public static ItemStack createRottenItemStack(ItemStack originalStack) {
        if (originalStack.isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(ModItems.ROTTEN_MEAT.get(), originalStack.getCount());
    }

    /**
     * Helper method to get the position of a container holding the item
     */
    @Nullable
    private BlockPos getContainerPosForItem(ItemStack stack, Level level, Object context) {
        if (context instanceof BlockEntity blockEntity) {
            return blockEntity.getBlockPos();
        } else if (context instanceof Player player) {
            // Check the player's inventory and nearby blocks
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i) == stack) {
                    return null;
                }
            }
            // Check nearby blocks for a container
            return getNearbyContainerPos(level, player.getOnPos());
        } else if (context instanceof Inventory) {
            return null;
        }
        return null;
    }

    // Helper method to find a nearby container
    private BlockPos getNearbyContainerPos(Level level, BlockPos pos) {
        for (BlockPos nearbyPos : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockEntity(nearbyPos) != null) {
                return nearbyPos;
            }
        }
        return null;
    }

    // Check if item is susceptible to rot
    private boolean isFoodThatRots(ItemStack stack) {
        return IceBoxEntity.isRotConsideredFood(stack);
    }

    /**
     * Saves the current rot timer state to a CompoundTag
     * Can be used to persist state between game sessions
     */
    public CompoundTag saveRotTimersToNBT() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : rotTimers.entrySet()) {
            tag.putInt(entry.getKey(), entry.getValue());
        }
        return tag;
    }

    /**
     * Loads rot timer state from a CompoundTag
     * Can be used to restore state between game sessions
     */
    public void loadRotTimersFromNBT(CompoundTag tag) {
        rotTimers.clear();
        for (String key : tag.getAllKeys()) {
            rotTimers.put(key, tag.getInt(key));
        }
    }
}