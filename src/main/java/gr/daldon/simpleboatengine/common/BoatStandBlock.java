package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * Rustic parking cradle with a paint station. Sneak + empty hand retrieves
 * the complete stored boat; click the bench to open its existing paint GUI.
 */
public final class BoatStandBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public BoatStandBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder){
        builder.add(FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        Direction direction=context.getHorizontalDirection().getOpposite();
        // Original stand model uses opposite front for North/South only.
        if(direction.getAxis()==Direction.Axis.Z)direction=direction.getOpposite();
        return defaultBlockState().setValue(FACING,direction);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoatStandBlockEntity(pos, state);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos,
                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server) ||
            !(level.getBlockEntity(pos) instanceof BoatStandBlockEntity station))
            return InteractionResult.PASS;
        ItemStack held = server.getItemInHand(hand);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && BoatNavigationEvents.bindParked(player, hand, station, serverLevel))
            return InteractionResult.CONSUME;
        if(held.is(EngineItems.BOAT_RECOVERY_KIT.get())) {
            if(station.hasPreview())return InteractionResult.CONSUME;
            // Recover directly into the player's inventory; the hook stays reusable.
            if(!station.getItem(0).isEmpty()) {
                ItemStack saved=station.removeItemNoUpdate(0);
                station.setChanged();
                if(!server.getInventory().add(saved))server.drop(saved,false);
            }
            // Recover items from legacy loaded hooks without losing old saves.
            if(BoatRecoveryItem.loaded(held)) {
                ItemStack previous=BoatRecoveryItem.boat(held);
                BoatRecoveryItem.put(held,ItemStack.EMPTY);
                if(!server.getInventory().add(previous))server.drop(previous,false);
            }
            return InteractionResult.CONSUME;
        }
        if(station.hasPreview() && !server.getUUID().equals(
            station.previewOwnerId()))return InteractionResult.CONSUME;
        if (station.getItem(0).isEmpty() && held.getItem() instanceof SkiffBoatItem) {
            ItemStack parked = held.copy();
            parked.setCount(1);
            station.setItem(0, parked);
            if (!server.getAbilities().instabuild) held.shrink(1);
            return InteractionResult.CONSUME;
        }
        if (server.isShiftKeyDown() && held.isEmpty() && !station.getItem(0).isEmpty()) {
            if(station.hasPreview())station.cancelPreview(server.getUUID());
            ItemStack fullBoat = station.removeItemNoUpdate(0);
            if (!server.getInventory().add(fullBoat)) server.drop(fullBoat, false);
            return InteractionResult.CONSUME;
        }
        server.displayClientMessage(net.minecraft.network.chat.Component.literal(
            "Use a nearby Boat Painting Table to customize this parked boat."), true);
        return InteractionResult.CONSUME;
    }

    @Override public void onRemove(BlockState oldState, Level level, BlockPos pos,
                                   BlockState next, boolean moving) {
        if (!oldState.is(next.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof BoatStandBlockEntity be) {
                if(be.hasPreview())be.cancelPreview(be.previewOwnerId());
                Containers.dropContents(level, pos, be);
            }
            super.onRemove(oldState, level, pos, next, moving);
        }
    }
}
