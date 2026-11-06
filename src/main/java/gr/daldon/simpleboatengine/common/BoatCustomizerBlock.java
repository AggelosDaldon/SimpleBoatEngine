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

/** Physical workshop: insert a boat, edit its parts, and retrieve the same item. */
public final class BoatCustomizerBlock extends BaseEntityBlock {
    public BoatCustomizerBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoatCustomizerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BoatCustomizerBlockEntity workshop)
            || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (player.isShiftKeyDown() && !workshop.getItem(0).isEmpty()) {
            ItemStack retrieved = workshop.removeItemNoUpdate(0);
            workshop.setChanged();
            if (!player.getInventory().add(retrieved)) player.drop(retrieved, false);
            return InteractionResult.CONSUME;
        }
        ItemStack held = player.getMainHandItem();
        if (workshop.getItem(0).isEmpty() && held.getItem() instanceof SkiffBoatItem) {
            ItemStack placed = held.copy();
            placed.setCount(1);
            workshop.setItem(0, placed);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        NetworkHooks.openScreen(serverPlayer, workshop, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState,
                         boolean moving) {
        if (!oldState.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof BoatCustomizerBlockEntity workshop)
                Containers.dropContents(level, pos, workshop);
            super.onRemove(oldState, level, pos, newState, moving);
        }
    }
}
