package gr.daldon.simpleboatengine.common;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** Places a free-standing trailer on the ground, ready for towing or loading. */
public final class BoatTrailerItem extends Item {
    public BoatTrailerItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var block = context.getClickedPos().relative(context.getClickedFace());
        Vec3 pos = Vec3.atBottomCenterOf(block);
        if (!level.isClientSide) {
            BoatTrailer trailer = EngineEntities.BOAT_TRAILER.get().create(level);
            if (trailer == null) return InteractionResult.FAIL;
            trailer.setPos(pos.x, pos.y, pos.z);
            if (context.getPlayer() != null) trailer.setYRot(context.getPlayer().getYRot());
            if (!level.noCollision(trailer, trailer.getBoundingBox().inflate(-0.03)))
                return InteractionResult.FAIL;
            level.addFreshEntity(trailer);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
                context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
