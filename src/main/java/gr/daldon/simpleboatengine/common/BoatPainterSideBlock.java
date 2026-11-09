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

/** Invisible structural half of a true 2x1 painting bench, not a separate item. */
public final class BoatPainterSideBlock extends Block {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public BoatPainterSideBlock(BlockBehaviour.Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        builder.add(FACING);
    }
    private BlockPos master(BlockState state,BlockPos pos){
        return pos.relative(state.getValue(FACING).getCounterClockWise());
    }
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,
                             Player player,InteractionHand hand,BlockHitResult hit){
        BlockPos base=master(state,pos);
        BlockState main=level.getBlockState(base);
        if(main.getBlock() instanceof BoatCustomizerBlock)
            return main.getBlock().use(main,level,base,player,hand,hit);
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,
                                   BlockState next,boolean moving){
        if(!state.is(next.getBlock()) && !level.isClientSide){
            BlockPos base=master(state,pos);
            if(level.getBlockState(base).is(EngineBlocks.CUSTOMIZER.get()))
                level.destroyBlock(base,true);
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
