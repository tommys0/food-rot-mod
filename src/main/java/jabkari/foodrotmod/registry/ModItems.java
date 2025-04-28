package jabkari.foodrotmod.registry;

import jabkari.foodrotmod.Foodrotmod;
import jabkari.foodrotmod.items.RottenMeatItem;
import net.minecraft.world.item.*;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Foodrotmod.MOD_ID);

    // --- Regular Items ---
    public static final RegistryObject<Item> ROTTEN_MEAT = ITEMS.register("rotten_meat",
            () -> new RottenMeatItem(new Item.Properties().food(RottenMeatItem.ROTTEN_FOOD_COMPONENT)) // Assuming RottenMeatItem takes props & food component
    );

    // --- Block Items ---
    public static final RegistryObject<Item> ICE_BOX_ITEM = ITEMS.register("ice_box",
            () -> new BlockItem(ModBlocks.ICE_BOX.get(), new Item.Properties()));

    // Method to register the ITEMS DeferredRegister
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    // Method to add items to creative tabs
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ROTTEN_MEAT);
        }

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ICE_BOX_ITEM);
        }
        // Add other items to other tabs if needed
    }
}