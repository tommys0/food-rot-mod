package jabkari.foodrotmod.registry;

import jabkari.foodrotmod.Foodrotmod;
import jabkari.foodrotmod.blocks.IceBox;
import jabkari.foodrotmod.blocks.IceBoxEntity; // Needed for getTicker signature
import net.minecraft.world.level.Level; // Needed for getTicker signature
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity; // Needed for getTicker signature
import net.minecraft.world.level.block.entity.BlockEntityTicker; // Needed for getTicker signature
import net.minecraft.world.level.block.entity.BlockEntityType; // Needed for getTicker signature
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState; // Needed for getTicker signature
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable; // For Nullable annotation

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Foodrotmod.MOD_ID);

    // Register the IceBox block
    public static final RegistryObject<Block> ICE_BOX = BLOCKS.register("ice_box",
            () -> new IceBox(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(2.0f)
                    .requiresCorrectToolForDrops()));

    // Method to register the BLOCKS DeferredRegister
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    // --- Ticker Provider Interface ---
    // This is implicitly implemented by extending Block, but we need the logic
    // The getTicker method is now part of the Block class itself.
    // We will override it in IceBox.java directly. No code needed here.
}