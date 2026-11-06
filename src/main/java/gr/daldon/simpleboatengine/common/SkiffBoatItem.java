package gr.daldon.simpleboatengine.common;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityType;
import java.util.function.Supplier;

/** Places the larger test hull on water without replacing vanilla boat items. */
public final class SkiffBoatItem extends EngineItem {
    private final Supplier<? extends EntityType<? extends SkiffBoat>> type;

    public SkiffBoatItem(Properties properties,
                         Supplier<? extends EntityType<? extends SkiffBoat>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (!(hit instanceof BlockHitResult blockHit)
            || hit.getType() != HitResult.Type.BLOCK
            || !level.getFluidState(blockHit.getBlockPos()).is(FluidTags.WATER)) {
            return InteractionResultHolder.pass(held);
        }

        Vec3 location = hit.getLocation();
        SkiffBoat boat = type.get().create(level);
        if (boat == null) return InteractionResultHolder.fail(held);
        boat.setPos(location.x, location.y, location.z);
        boat.setYRot(player.getYRot());
        if (!level.noCollision(boat, boat.getBoundingBox().inflate(-0.1))) {
            return InteractionResultHolder.fail(held);
        }
        if (!level.isClientSide) {
            level.addFreshEntity(boat);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }
}
