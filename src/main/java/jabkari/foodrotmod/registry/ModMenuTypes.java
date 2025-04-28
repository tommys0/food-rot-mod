package jabkari.foodrotmod.registry;

import jabkari.foodrotmod.Foodrotmod;
import jabkari.foodrotmod.blocks.IceBoxEntity;
import jabkari.foodrotmod.menu.IceBoxMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class ModMenuTypes {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Foodrotmod.MOD_ID);

    private static final IContainerFactory<IceBoxMenu> ICE_BOX_MENU_FACTORY =
            (windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                BlockEntity blockEntity = inv.player.level().getBlockEntity(pos);

                if (blockEntity instanceof IceBoxEntity iceBoxEntity) {
                    return new IceBoxMenu(windowId, inv, iceBoxEntity, blockEntity.getLevel(), pos);
                } else {
                    LOGGER.error("Incorrect or missing BlockEntity for IceBoxMenu at {}. Found: {}", pos, blockEntity);
                    return null;
                }
            };

    public static final RegistryObject<MenuType<IceBoxMenu>> ICE_BOX_MENU =
            MENUS.register("ice_box_menu", () -> IForgeMenuType.create(ICE_BOX_MENU_FACTORY));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}