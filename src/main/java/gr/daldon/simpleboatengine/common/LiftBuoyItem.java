package gr.daldon.simpleboatengine.common;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class LiftBuoyItem extends BlockItem {
    public LiftBuoyItem(Properties properties) {
        super(EngineBlocks.LIFT_BUOY.get(), properties);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held=player.getItemInHand(hand);
        HitResult target=getPlayerPOVHitResult(level,player,ClipContext.Fluid.SOURCE_ONLY);
        if (!(target instanceof BlockHitResult hit) || target.getType()!=HitResult.Type.BLOCK)
            return InteractionResultHolder.pass(held);
        var pos=hit.getBlockPos();
        if (!level.getFluidState(pos).is(FluidTags.WATER) ||
                !level.getFluidState(pos).isSource()) return InteractionResultHolder.pass(held);
        if (!level.isClientSide) {
            if (!level.setBlock(pos,EngineBlocks.LIFT_BUOY.get().defaultBlockState()
                    .setValue(LiftBuoyBlock.WATERLOGGED,true),3))
                return InteractionResultHolder.fail(held);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(held,level.isClientSide);
    }
}
