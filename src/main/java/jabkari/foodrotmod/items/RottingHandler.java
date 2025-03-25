package jabkari.foodrotmod.items;

import jabkari.foodrotmod.registry.ModItems;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.UUID;

@Mod.EventBusSubscriber
public class RottingHandler {
    private static final HashMap<UUID, HashMap<ItemStack, Integer>> foodTimers = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer player && event.phase == TickEvent.Phase.START) {
            // Check if the player has food items in their inventory
            for (ItemStack itemStack : player.getInventory().items) {
                if (isRawMeat(itemStack) && !isInCoolingBox(player, itemStack)) {
                    // Start or increment the rotting timer for raw meat if not in cooling box
                    if (!foodTimers.containsKey(player.getUUID())) {
                        foodTimers.put(player.getUUID(), new HashMap<>());
                    }

                    HashMap<ItemStack, Integer> playerFoodTimers = foodTimers.get(player.getUUID());
                    playerFoodTimers.putIfAbsent(itemStack, 0);

                    int currentTime = playerFoodTimers.get(itemStack);
                    currentTime++;

                    // If the raw meat has rotted completely (after a certain amount of ticks)
                    if (currentTime >= 24000) {  // Example: 100 ticks = some rot time
                        // Remove the raw meat item from inventory
                        itemStack.setCount(itemStack.getCount() - 1);
                        if (itemStack.getCount() <= 0) {
                            player.getInventory().removeItem(itemStack);
                        }

                        // Add a rotten version of the meat
                        ItemStack rottenMeat = new ItemStack(ModItems.ROTTEN_MEAT.get(), 1); // Adjust if you have custom rotten meat
                        player.getInventory().add(rottenMeat);

                        // Reset the timer for this item
                        playerFoodTimers.remove(itemStack);
                    } else {
                        playerFoodTimers.put(itemStack, currentTime); // Update the timer
                    }
                } else if (isCookedMeat(itemStack)) {
                    // If it's cooked meat, no need to track rotting or storage
                    continue;
                }
            }
        }
    }

    // Check if the item is raw meat (needs cooling down and could rot)
    private static boolean isRawMeat(ItemStack itemStack) {
        return itemStack.getItem() == Items.PORKCHOP || itemStack.getItem() == Items.BEEF;  // Add more raw meat types as needed
    }

    // Check if the item is cooked meat (doesn't need cooling and doesn't rot)
    private static boolean isCookedMeat(ItemStack itemStack) {
        return itemStack.getItem() == Items.COOKED_PORKCHOP || itemStack.getItem() == Items.COOKED_BEEF;  // Add more cooked meat types as needed
    }

    // Check if the item is in the custom cooling box (like a fridge)
    private static boolean isInCoolingBox(ServerPlayer player, ItemStack itemStack) {
        // Add logic here to check if the item is inside the cooling box.
        // This can be based on a custom block or item type acting as a cooling box.
        // For example, if you have an item with a special identifier:
        // return player.getInventory().contains(new ItemStack(ModItems.COOLING_BOX));

        // For now, just returning false to simulate it's not in the box.
        return false; // Adjust this logic based on how your cooling box works
    }
}
