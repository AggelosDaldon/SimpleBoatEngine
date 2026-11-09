package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;
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
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public BoatCustomizerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder){
        builder.add(FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        Direction facing=context.getHorizontalDirection().getOpposite();
        BlockPos second=context.getClickedPos().relative(facing.getClockWise());
        return context.getLevel().getBlockState(second).canBeReplaced(context)
            ? defaultBlockState().setValue(FACING,facing):null;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,
                                      @Nullable LivingEntity placer,ItemStack item){
        super.setPlacedBy(level,pos,state,placer,item);
        level.setBlock(pos.relative(state.getValue(FACING).getClockWise()),
            EngineBlocks.CUSTOMIZER_SIDE.get().defaultBlockState()
                .setValue(BoatPainterSideBlock.FACING,state.getValue(FACING)),3);
    }

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
        // Return legacy input-slot materials from older painter versions.
        // The new graphical editor reads directly from player inventory.
        for(int slot=1;slot<4;slot++){
            ItemStack legacy=workshop.removeItemNoUpdate(slot);
            if(!legacy.isEmpty() && !player.getInventory().add(legacy))
                player.drop(legacy,false);
        }
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof SkiffBoatItem) {
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "Park this boat on a Boat Stand first, then paint it here."),true);
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
            BlockPos side=pos.relative(oldState.getValue(FACING).getClockWise());
            if(level.getBlockState(side).is(EngineBlocks.CUSTOMIZER_SIDE.get()))
                level.removeBlock(side,false);
            super.onRemove(oldState, level, pos, newState, moving);
        }
    }
}
