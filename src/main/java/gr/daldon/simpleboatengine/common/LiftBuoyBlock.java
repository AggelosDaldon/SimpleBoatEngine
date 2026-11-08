package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

/** A floating marker that retrieves one vertically linked block with one click.
 *  Sneak-right-click with a Supplementaries wrench to establish the link.
 */
public final class LiftBuoyBlock extends BaseEntityBlock {
    public LiftBuoyBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LiftBuoyBlockEntity(pos, state);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server) ||
                !(level.getBlockEntity(pos) instanceof LiftBuoyBlockEntity lift)) return InteractionResult.PASS;

        ResourceLocation heldId = ForgeRegistries.ITEMS.getKey(player.getItemInHand(hand).getItem());
        boolean wrench = heldId != null && heldId.toString().equals("supplementaries:wrench");
        if (player.isShiftKeyDown() && wrench) {
            if (lift.bindClosestUnderneath()) {
                player.displayClientMessage(Component.literal("Buoy linked to block below."), true);
                level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.7f, 1f);
            } else player.displayClientMessage(Component.literal(
                "No movable block directly underneath within 32 blocks."), true);
            return InteractionResult.CONSUME;
        }
        String message = lift.raise() ? "Linked block raised directly below buoy."
            : "Cannot lift: link missing, blocked destination, or unsupported block.";
        server.displayClientMessage(Component.literal(message), true);
        return InteractionResult.CONSUME;
    }
}
