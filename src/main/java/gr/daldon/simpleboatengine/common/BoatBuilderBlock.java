package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

/** Standalone 1x1 marine construction bench; no nearby stand required. */
public final class BoatBuilderBlock extends Block {
    public BoatBuilderBlock(BlockBehaviour.Properties properties){super(properties);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,
        Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(player instanceof ServerPlayer server){
            NetworkHooks.openScreen(server,new SimpleMenuProvider(
                (id,inventory,who)->new BoatBuilderMenu(id,inventory,pos),
                Component.literal("Marine Boat Builder")),buf->buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }
}
