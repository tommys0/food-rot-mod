package jabkari.foodrotmod.blocks;

import com.mojang.serialization.MapCodec;
// No longer importing Foodrotmod directly if logger is local
import jabkari.foodrotmod.db.DatabaseService;
import jabkari.foodrotmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer; // Needed for temp container
import net.minecraft.world.Containers;      // Needed for dropping
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List; // Needed for loaded items


public class IceBox extends BaseEntityBlock {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final MapCodec<IceBox> CODEC = simpleCodec(IceBox::new);
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public IceBox(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.ICE_BOX_ENTITY.get().create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ICE_BOX_ENTITY.get(), IceBoxEntity::serverTick);
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IceBoxEntity iceBoxEntity) {
                player.openMenu(iceBoxEntity);
            } else {
                LOGGER.warn("Missing IceBoxEntity at {} when interacting!", pos);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && DatabaseService.isInitialized()) {
            DatabaseService.saveOrUpdateIceBox(level, pos, Collections.emptyList(), level.registryAccess());
        } else if (!level.isClientSide && !DatabaseService.isInitialized()){
            LOGGER.warn("Skipping DB record creation for IceBox at {}: DatabaseService not initialized.", pos);
        }
        // Removed hasCustomHoverName check
    }

    // --- Use onRemove instead of playerWillDestroy ---
    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Only act if the block type actually changed, not just state properties
        if (!oldState.is(newState.getBlock())) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos); // Get BE *before* calling super potentially removes it

                if (be instanceof IceBoxEntity iceBoxEntity) {
                    // Drop contents using the BE's method if it still exists
                    LOGGER.debug("IceBox block removed at {}, dropping contents from existing BE.", pos);
                    iceBoxEntity.drops(); // This method handles loading from DB if needed
                } else {
                    // If BE is already gone (or never existed), try loading from DB to drop items
                    if (DatabaseService.isInitialized() && DatabaseService.isLocationAnIceBox(level, pos)) {
                        LOGGER.warn("IceBoxEntity missing at {} during onRemove, attempting to load/drop from DB.", pos);
                        List<ItemStack> itemsToDrop = DatabaseService.loadItemsForIceBox(level, pos, 9, level.registryAccess()); // Assuming size 9
                        if (!itemsToDrop.stream().allMatch(ItemStack::isEmpty)) {
                            // Need a temporary container to use Containers.dropContents
                            SimpleContainer tempContainer = new SimpleContainer(itemsToDrop.size());
                            for(int i=0; i< itemsToDrop.size(); ++i) {
                                tempContainer.setItem(i, itemsToDrop.get(i));
                            }
                            Containers.dropContents(level, pos, tempContainer);
                            LOGGER.debug("Dropped {} items loaded from DB for missing BE at {}", itemsToDrop.stream().filter(s -> !s.isEmpty()).count(), pos);
                        }
                    }
                }

                // Always attempt to remove the database record if it exists
                if (DatabaseService.isInitialized()) {
                    // No need to re-check isLocationAnIceBox if we already did above
                    // Just attempt removal - it won't fail if record doesn't exist
                    LOGGER.debug("IceBox block removed at {}, removing DB record.", pos);
                    DatabaseService.removeIceBox(level, pos);
                } else {
                    LOGGER.warn("Skipping DB record removal check for IceBox at {}: DatabaseService not initialized.", pos);
                }
            }
            // Call super AFTER our logic, which handles BE removal
            super.onRemove(oldState, level, pos, newState, isMoving);
        } else {
            // Block state changed but type is the same, call super only
            super.onRemove(oldState, level, pos, newState, isMoving);
        }
    }

    // --- Remove the problematic playerWillDestroy method ---
     /*
    @Override // Keep commented out or delete entirely
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
       // ... code removed ...
    }
    */

    // --- Block State Properties (Example: FACING) ---
    /*
     @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    // ... etc ...
    */
}