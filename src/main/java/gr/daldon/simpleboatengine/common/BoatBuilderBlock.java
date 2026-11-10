package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;
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
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public BoatBuilderBlock(BlockBehaviour.Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        builder.add(FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        Direction facing=context.getHorizontalDirection();
        BlockPos side=context.getClickedPos().relative(facing.getClockWise());
        return context.getLevel().getBlockState(side).canBeReplaced(context)
            ?defaultBlockState().setValue(FACING,facing):null;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,
                                     @Nullable LivingEntity placer,ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        level.setBlock(pos.relative(state.getValue(FACING).getClockWise()),
            EngineBlocks.BOAT_BUILDER_SIDE.get().defaultBlockState()
                .setValue(BoatBuilderSideBlock.FACING,state.getValue(FACING)),3);
    }
    @Override public void onRemove(BlockState original,Level level,BlockPos pos,
                                   BlockState next,boolean moving){
        if(!original.is(next.getBlock())){
            BlockPos side=pos.relative(original.getValue(FACING).getClockWise());
            if(level.getBlockState(side).is(EngineBlocks.BOAT_BUILDER_SIDE.get()))
                level.removeBlock(side,false);
        }
        super.onRemove(original,level,pos,next,moving);
    }
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
