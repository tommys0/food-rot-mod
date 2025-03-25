package jabkari.foodrotmod.items;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public class RottenMeatItem extends Item {
    public RottenMeatItem() {
        super(new Item.Properties()
                .food(new FoodProperties.Builder()
                        .nutrition(1) // Very little nutrition
                        .saturationModifier(0.1f) // Low saturation
                        .alwaysEdible()
                        .build())
        );
    }
}
