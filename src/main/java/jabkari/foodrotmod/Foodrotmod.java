package jabkari.foodrotmod;

import com.mojang.logging.LogUtils;
import jabkari.foodrotmod.registry.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.Item;
import jabkari.foodrotmod.registry.ModItems;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Foodrotmod.MOD_ID)
public class Foodrotmod
{
    // Define a mod id in the common_registry, used to identify your mod in resources
    public static final String MOD_ID = "foodrotmod";

    // Create a Deferred Register to hold Items
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    // Create the main constructor for registering the mod
    public Foodrotmod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.register(this);

        // Registering items to mod's deferred register
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the item registry
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
    }

    // This method will execute during the commonSetup phase
    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
    }

    // Used for initializing mod-specific setups
    public void onServerStarting(ServerStartingEvent event)
    {
        // This method is called when the server starts
        LogUtils.getLogger().info("Foodrotmod is starting up!");
    }

    // Used for client setup
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event)
    {
        // Some client setup code
        LogUtils.getLogger().info("Foodrotmod is starting on the client!");
    }
}
