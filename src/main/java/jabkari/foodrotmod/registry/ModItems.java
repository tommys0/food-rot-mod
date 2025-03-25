package jabkari.foodrotmod.registry;

import jabkari.foodrotmod.items.RottenMeatItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "foodrotmod");

    public static final RegistryObject<Item> ROTTEN_MEAT = ITEMS.register("rotten_meat", RottenMeatItem::new);
}
