package gr.daldon.simpleboatengine.common;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class LiftBuoyItem extends BlockItem {
    private final int color;
    public LiftBuoyItem(Properties properties) { this(properties, 0); }
    public LiftBuoyItem(Properties properties, boolean yellow) { this(properties, yellow ? 1 : 0); }
    public LiftBuoyItem(Properties properties, int color) {
        super(EngineBlocks.LIFT_BUOY.get(), properties);
        this.color = color;
    }
    @Override public net.minecraft.network.chat.Component getName(ItemStack stack) {
        return net.minecraft.network.chat.Component.translatable(
            color==1?"item.daldonsboatadditions.yellow_lift_buoy":
            color==2?"item.daldonsboatadditions.white_lift_buoy":
                     "item.daldonsboatadditions.lift_buoy");
    }
    /** BlockItem normally handles a click on a block before Item.use(). */
    @Override public InteractionResult useOn(UseOnContext context) {
        Level level=context.getLevel();
        Player player=context.getPlayer();
        var pos=context.getClickedPos();
        if (player==null || !level.getFluidState(pos).isSource()
            || !level.getFluidState(pos).is(FluidTags.WATER))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            boolean ok=level.setBlock(pos,EngineBlocks.LIFT_BUOY.get().defaultBlockState()
                .setValue(LiftBuoyBlock.WATERLOGGED,true)
                .setValue(LiftBuoyBlock.YELLOW,color == 1)
                .setValue(LiftBuoyBlock.WHITE,color == 2),3);
            if (!ok) return InteractionResult.FAIL;
            if (!player.getAbilities().instabuild)
                context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
                    .setValue(LiftBuoyBlock.WATERLOGGED,true)
                    .setValue(LiftBuoyBlock.YELLOW,color==1)
                    .setValue(LiftBuoyBlock.WHITE,color==2),3))
                return InteractionResultHolder.fail(held);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(held,level.isClientSide);
    }
}
