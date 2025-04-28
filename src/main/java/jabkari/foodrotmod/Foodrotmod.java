package jabkari.foodrotmod;

import com.mojang.logging.LogUtils;
import jabkari.foodrotmod.client.IceBoxScreen;
import jabkari.foodrotmod.db.DatabaseService;
import jabkari.foodrotmod.registry.ModBlocks;
import jabkari.foodrotmod.registry.ModBlockEntities;
import jabkari.foodrotmod.registry.ModItems;
import jabkari.foodrotmod.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Foodrotmod.MOD_ID)
public class Foodrotmod {
    public static final String MOD_ID = "foodrotmod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Foodrotmod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register all registries to the mod event bus
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Initializing.");
        if (Config.logDirtBlock) {
            LOGGER.info("DIRT BLOCK >> {}", net.minecraft.world.level.block.Blocks.DIRT.getDescriptionId());
        }
        Config.onLoad(null); // Manually trigger config load
        LOGGER.info("Magic number: {}", Config.magicNumber);
        LOGGER.info("{}", Config.magicNumberIntroduction);

        event.enqueueWork(() -> {
            DatabaseService.initialize();
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server Starting.");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("{} Server Stopping, shutting down DatabaseService...", MOD_ID);
        DatabaseService.shutdown();
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Starting Client Setup for {}", MOD_ID);
            event.enqueueWork(() -> {
                MenuScreens.register(ModMenuTypes.ICE_BOX_MENU.get(), IceBoxScreen::new);
                LOGGER.info("Registered GUI Screens for {}", MOD_ID);
            });
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEventBusEvents {
        @SubscribeEvent
        public static void addCreative(BuildCreativeModeTabContentsEvent event) {
            ModItems.addCreative(event);
        }
    }
}