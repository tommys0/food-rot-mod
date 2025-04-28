package jabkari.foodrotmod.registry;

import jabkari.foodrotmod.Foodrotmod;
import jabkari.foodrotmod.blocks.IceBoxEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Foodrotmod.MOD_ID);

    public static final RegistryObject<BlockEntityType<IceBoxEntity>> ICE_BOX_ENTITY =
            BLOCK_ENTITIES.register("ice_box_entity", () ->
                    BlockEntityType.Builder.of(IceBoxEntity::new,
                            ModBlocks.ICE_BOX.get() // Valid block(s) for this BE
                    ).build(null)); // datafixer is null for mods

    // Helper method for creating tickers safely (optional but good practice)
    // This is now generally handled directly in the Block's getTicker method,
    // but you can keep helpers like this if you prefer.
    @Nullable
    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> pServerType, BlockEntityType<E> pClientType, BlockEntityTicker<? super E> pTicker) {
        // Ensure the types match before casting the ticker
        return pClientType == pServerType ? (BlockEntityTicker<A>) pTicker : null;
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}