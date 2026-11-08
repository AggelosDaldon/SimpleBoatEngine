package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
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
    public BoatStandBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoatStandBlockEntity(pos, state);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos,
                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server) ||
            !(level.getBlockEntity(pos) instanceof BoatStandBlockEntity station))
            return InteractionResult.PASS;
        ItemStack held = server.getItemInHand(hand);
        if (station.getItem(0).isEmpty() && held.getItem() instanceof SkiffBoatItem) {
            ItemStack parked = held.copy();
            parked.setCount(1);
            station.setItem(0, parked);
            if (!server.getAbilities().instabuild) held.shrink(1);
            return InteractionResult.CONSUME;
        }
        if (server.isShiftKeyDown() && held.isEmpty() && !station.getItem(0).isEmpty()) {
            ItemStack fullBoat = station.removeItemNoUpdate(0);
            if (!server.getInventory().add(fullBoat)) server.drop(fullBoat, false);
            return InteractionResult.CONSUME;
        }
        NetworkHooks.openScreen(server, station, pos);
        return InteractionResult.CONSUME;
    }

    @Override public void onRemove(BlockState oldState, Level level, BlockPos pos,
                                   BlockState next, boolean moving) {
        if (!oldState.is(next.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof BoatStandBlockEntity be)
                Containers.dropContents(level, pos, be);
            super.onRemove(oldState, level, pos, next, moving);
        }
    }
}
