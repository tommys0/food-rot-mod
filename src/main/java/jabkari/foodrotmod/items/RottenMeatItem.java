package jabkari.foodrotmod.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public class RottenMeatItem extends Item {

    public static final FoodProperties ROTTEN_FOOD_COMPONENT = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.1f)
            .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F)
            .meat()
            .build();

    public RottenMeatItem(Properties pProperties) {
        super(pProperties.food(ROTTEN_FOOD_COMPONENT));
    }
}