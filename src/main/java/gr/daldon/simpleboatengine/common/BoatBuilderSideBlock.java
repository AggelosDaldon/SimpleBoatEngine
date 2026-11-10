package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Invisible second half of the 2-block shipwright table, with shared GUI. */
public final class BoatBuilderSideBlock extends Block {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public BoatBuilderSideBlock(BlockBehaviour.Properties props){
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        builder.add(FACING);
    }
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,
                                            Player player,InteractionHand hand,BlockHitResult hit){
        BlockPos master=pos.relative(state.getValue(FACING).getCounterClockWise());
        BlockState base=level.getBlockState(master);
        if(base.getBlock() instanceof BoatBuilderBlock)
            return base.getBlock().use(base,level,master,player,hand,hit);
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,
                                   BlockState next,boolean moving){
        if(!state.is(next.getBlock()) && !level.isClientSide){
            BlockPos master=pos.relative(state.getValue(FACING).getCounterClockWise());
            if(level.getBlockState(master).is(EngineBlocks.BOAT_BUILDER.get()))
                level.destroyBlock(master,true);
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
