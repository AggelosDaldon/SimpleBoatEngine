package gr.daldon.simpleboatengine.common;

import net.minecraft.tags.FluidTags;
import net.minecraft.nbt.CompoundTag;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.enchantment.Enchantments;

/** Places the larger test hull on water without replacing vanilla boat items. */
public final class SkiffBoatItem extends EngineItem {
    private final Supplier<? extends EntityType<? extends SkiffBoat>> type;
    private final int style;

    public SkiffBoatItem(Properties properties,
                         Supplier<? extends EntityType<? extends SkiffBoat>> type) {
        this(properties, type, 0);
    }

    public SkiffBoatItem(Properties properties,
                         Supplier<? extends EntityType<? extends SkiffBoat>> type, int style) {
        super(properties);
        this.type = type;
        this.style = style;
    }

    public int getHullStyle() { return style; }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack boat) {
        int mask = BoatPaintSessions.attachmentMask(boat);
        if (mask == 0) return Optional.empty();
        List<ItemStack> attachments = new ArrayList<>();
        for (int type = 0; type < BoatCustomizerMenu.ATTACHMENT_SLOTS; type++) {
            if ((mask & (1 << type)) == 0) continue;
            ItemStack attachment = new ItemStack(BoatCustomizerMenu.attachmentType(type));
            if (type == 3 && BoatPaintSessions.motor(boat).getBoolean("IceBreakerSilk"))
                attachment.enchant(Enchantments.SILK_TOUCH, 1);
            attachments.add(attachment);
        }
        return Optional.of(new BoatAttachmentTooltip(List.copyOf(attachments)));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity carrier,
                              int slot, boolean selected) {
        if (level instanceof net.minecraft.server.level.ServerLevel server && level.getGameTime() % 10 == 0
                && BoatNavigationData.packedId(stack) != null) {
            BoatNavigationData.get(server).observePacked(stack, server, carrier.blockPosition(), false);
        }
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
        if (boat instanceof BlueFishingBoat blue) {
            blue.setHullStyle(BoatPaint.woodStyle(held, style));
            blue.setHullPaint(BoatPaint.hull(held));
            blue.setFloorPaint(BoatPaint.floor(held));
            for (int channel = 2; channel < BoatPaint.CHANNELS; channel++)
                blue.setPaint(channel, BoatPaint.get(held, channel));
            blue.setBoatName(BoatPaint.name(held));
            blue.setNameColor(BoatPaint.nameColor(held));
            blue.setNameGlow(BoatPaint.nameGlows(held));
        }
        if (!level.isClientSide && held.hasTag()
                && held.getTag().contains("PackedFishingBoat", 10)) {
            CompoundTag saved = held.getTag().getCompound("PackedFishingBoat").copy();
            // A newly painted boat can have only partial NBT (engine or cargo).
            // Merge it into complete default entity NBT before loading.
            CompoundTag complete = new CompoundTag();
            boat.saveWithoutId(complete);
            complete.merge(saved);
            // A fresh entity needs its own identity; do not restore the old UUID.
            complete.remove("UUID");
            try {
                boat.load(complete);
            } catch (RuntimeException error) {
                org.slf4j.LoggerFactory.getLogger(SkiffBoatItem.class)
                    .error("Could not unpack the stored fishing boat", error);
                return InteractionResultHolder.fail(held);
            }
            // Item customizations can change after the boat was packed.
            // Apply them after loading the saved engine and cargo.
            if (boat instanceof BlueFishingBoat packedBlue) {
                packedBlue.setHullStyle(BoatPaint.woodStyle(held, style));
                packedBlue.setHullPaint(BoatPaint.hull(held));
                packedBlue.setFloorPaint(BoatPaint.floor(held));
                for (int channel = 2; channel < BoatPaint.CHANNELS; channel++)
                    packedBlue.setPaint(channel, BoatPaint.get(held, channel));
                packedBlue.setBoatName(BoatPaint.name(held));
                packedBlue.setNameColor(BoatPaint.nameColor(held));
                packedBlue.setNameGlow(BoatPaint.nameGlows(held));
            }
            // A stored running engine must never restart autonomously.
            var engine = MotorData.writable(boat);
            engine.putBoolean("Running", false);
            engine.putInt("Throttle", 0);
        }
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
