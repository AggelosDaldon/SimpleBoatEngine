package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/** Two-wheeled boat trailer; the loaded boat is stored once as full entity NBT.
 *  Automobility integration deliberately has no required binary dependency.
 */
public final class BoatTrailer extends Entity {
    private static final EntityDataAccessor<CompoundTag> DISPLAY =
        SynchedEntityData.defineId(BoatTrailer.class, EntityDataSerializers.COMPOUND_TAG);
    private CompoundTag loadedBoat = new CompoundTag();
    private UUID towVehicle;
    private static final EntityDataAccessor<Float> WHEEL_ROT =
        SynchedEntityData.defineId(BoatTrailer.class, EntityDataSerializers.FLOAT);
    private float previousWheelRot;
    private Vec3 lastTailPosition;
    private double suspensionVelocity;
    private static final double DRAWBAR_LENGTH = 3.75;
    private static final double AXLE_OFFSET = -7.0 / 16.0;

    public BoatTrailer(EntityType<? extends BoatTrailer> type, Level level) {
        super(type, level);
    }

    @Override protected void defineSynchedData() {
        entityData.define(DISPLAY, new CompoundTag());
        entityData.define(WHEEL_ROT, 0f);
    }

    @Override public boolean isPickable() { return true; }
    @Override public boolean canBeCollidedWith() { return true; }
    public boolean isLoaded() { return !loadedBoat.isEmpty(); }
    public CompoundTag displayData() { return entityData.get(DISPLAY); }
    public boolean isConnected() { return towVehicle != null; }
    public float wheelAngle(float partial) {
        return net.minecraft.util.Mth.lerp(partial, previousWheelRot, entityData.get(WHEEL_ROT));
    }
    public void coupleTo(Entity automobile) {
        if (automobile(automobile)) {
            towVehicle = automobile.getUUID();
            setYRot(automobile.getYRot());
            lastTailPosition = tail(automobile);
            suspensionVelocity = 0;
        }
    }
    public static boolean automobile(Entity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && id.getNamespace().equals("automobility")
            && id.getPath().equals("automobile");
    }
    public static Vec3 couplingPosition(Entity car) {
        return tail(car).subtract(tongue(car.getYRot()).scale(DRAWBAR_LENGTH));
    }

    /** The coupler is at the forward end of the modeled trailer tongue. */
    private static Vec3 tongue(float yaw) {
        return new Vec3(0, 0, 1).yRot((float) Math.toRadians(-yaw));
    }

    /** Sample the two trailer wheels against real block collision surfaces. */
    private Double surfaceAt(Vec3 pos) {
        Vec3 start = pos.add(0, 1.3, 0);
        Vec3 end = pos.add(0, -2.1, 0);
        var hit = level().clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().y : null;
    }

    private double desiredWheelHeight(float yaw) {
        Vec3 axle = position().add(tongue(yaw).scale(AXLE_OFFSET));
        Vec3 right = new Vec3(0.78, 0, 0).yRot((float)Math.toRadians(-yaw));
        Double a = surfaceAt(axle.add(right));
        Double b = surfaceAt(axle.subtract(right));
        if (a != null && b != null) return (a + b) / 2;
        if (a != null) return a;
        if (b != null) return b;
        return getY() - 0.20;
    }


    private static CompoundTag visual(BlueFishingBoat boat) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Style", boat.getHullStyle());
        tag.putIntArray("Paints", boat.getPaints());
        tag.putString("Name", boat.getBoatName());
        tag.putInt("NameColor", boat.getNameColor());
        tag.putBoolean("NameGlowing", boat.nameGlows());
        CompoundTag motor = MotorData.read(boat);
        tag.putBoolean("Engine", motor.getBoolean("Installed"));
        tag.putBoolean("PoleLight", motor.getBoolean("PoleLightInstalled"));
        tag.putBoolean("FuelCanister", motor.getBoolean("CanisterInstalled"));
        return tag;
    }

    /** Both chest inventories, paint, name, and all motor data are saved. */
    public boolean loadBoat(ServerPlayer player, ItemStack rope) {
        if (isLoaded()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "The trailer is already loaded."), true);
            return false;
        }
        if (!(level() instanceof ServerLevel server)) return false;
        BlueFishingBoat boat = null;
        if (rope.hasTag() && rope.getTag().hasUUID("BoatWinchTarget")) {
            UUID chosen = rope.getTag().getUUID("BoatWinchTarget");
            if (server.getEntity(chosen) instanceof BlueFishingBoat found
                && found.isAlive() && found.distanceToSqr(this) <= 256)
                boat = found;
        }
        if (boat == null) {
            // The fishing hull is far larger than vanilla boat's pick box.
            // A single boat nearby can be winched without the fragile first click.
            List<BlueFishingBoat> nearby = server.getEntitiesOfClass(
                BlueFishingBoat.class, getBoundingBox().inflate(16),
                b -> b.isAlive() && b.distanceToSqr(this) <= 256);
            if (nearby.size() == 1) boat = nearby.get(0);
            else {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    nearby.isEmpty() ? "No fishing boat within 16 blocks."
                        : "Multiple boats nearby: select one with the winch rope first."), true);
                return false;
            }
        }
        if (!boat.getPassengers().isEmpty()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "Everyone must leave the boat before loading."), true);
            return false;
        }
        CompoundTag full = new CompoundTag();
        boat.saveWithoutId(full);
        // Capture the full entity state before discarding the entity without drops.
        loadedBoat = full.copy();
        entityData.set(DISPLAY, visual(boat));
        boat.discard();
        if (rope.hasTag()) rope.getTag().remove("BoatWinchTarget");
        level().playSound(null, blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8f, 0.9f);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
            "Fishing boat winched onto trailer (cargo and equipment preserved)."), true);
        return true;
    }

    /** Unload directly behind the trailer, keeping all recorded entity NBT. */
    public boolean unloadBoat(ServerPlayer player) {
        if (!isLoaded() || !(level() instanceof ServerLevel server)) return false;
        BlueFishingBoat boat = EngineEntities.BLUE_FISHING_BOAT.get().create(server);
        if (boat == null) return false;
        try {
            boat.load(loadedBoat.copy());
        } catch (RuntimeException error) {
            org.slf4j.LoggerFactory.getLogger(BoatTrailer.class).error("Could not restore fishing boat from trailer", error);
            return false;
        }
        Vec3 behind = position().add(new Vec3(0, 0, -5.6)
            .yRot((float)Math.toRadians(-getYRot())));
        boat.setPos(behind.x, getY() + 0.3, behind.z);
        boat.setYRot(getYRot());
        if (!server.noCollision(boat, boat.getBoundingBox().inflate(-0.08))) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "There is not enough space behind the trailer to unload."), true);
            return false;
        }
        // Do not clear the saved payload until the world accepts the boat.
        if (!server.addFreshEntity(boat)) return false;
        loadedBoat = new CompoundTag();
        entityData.set(DISPLAY, new CompoundTag());
        server.playSound(null, blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.9f, 0.7f);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
            "Fishing boat released behind the trailer."), true);
        return true;
    }

    /** The 1.20.1 AutomobileEntity exposes getTailPos(); reflection makes
     *  this an optional integration and preserves standalone mod loading.
     */
    private static Vec3 tail(Entity car) {
        try {
            Method method = car.getClass().getMethod("getTailPos");
            Object result = method.invoke(car);
            if (result instanceof Vec3 pos) return pos;
        } catch (ReflectiveOperationException ignored) {}
        return car.position().add(new Vec3(0, 0, 1.15)
            .yRot((float)Math.toRadians(180 - car.getYRot())));
    }

    private boolean attachNearestAutomobile(ServerPlayer player) {
        List<Entity> nearby = level().getEntities(this, getBoundingBox().inflate(7),
            e -> e.isAlive() && automobile(e));
        Entity nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity car : nearby) {
            double dsq = car.distanceToSqr(this);
            if (dsq < best) { best = dsq; nearest = car; }
        }
        if (nearest == null || best > 49) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "Move the trailer close to an Automobility car."), true);
            return false;
        }
        coupleTo(nearest);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
            "Trailer coupled to Automobility car."), true);
        return true;
    }

    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (isLoaded()) return unloadBoat(serverPlayer) ? InteractionResult.CONSUME : InteractionResult.FAIL;
            if (towVehicle != null) {
                towVehicle = null;
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Trailer uncoupled."), true);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
        if (held.is(EngineItems.WINCH_ROPE.get())) {
            // A winch rope means load a nearby boat, even if first click
            // failed because of vanilla boat's undersized selection hit box.
            return loadBoat(serverPlayer, held) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }
        if (held.is(Items.LEAD))
            return attachNearestAutomobile(serverPlayer) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
            isLoaded() ? "Shift-right-click to unload. Use lead to couple car."
                       : "Select boat with black winch rope, then right-click trailer."), true);
        return InteractionResult.SUCCESS;
    }

    @Override public void tick() {
        super.tick();
        previousWheelRot = entityData.get(WHEEL_ROT);
        if (level().isClientSide) return;
        Vec3 beforeMove = position();
        Entity tow = null;
        if (towVehicle != null && level() instanceof ServerLevel server)
            tow = server.getEntity(towVehicle);
        if (tow != null && tow.isAlive() && automobile(tow)) {
            // Automobility v0.4.2.b computes the rear attachment from getTailPos()
            // and rotates its rear attachment independently. Implement the same
            // articulated principle for our separate trailer Entity.
            Vec3 hitch = tail(tow);
            Vec3 toHitch = hitch.subtract(position());
            float carYaw = tow.getYRot();
            float wantedYaw = (float)Math.toDegrees(Math.atan2(-toHitch.x, toHitch.z));
            if (toHitch.horizontalDistanceSqr() < .16) wantedYaw = carYaw;
            // Same 90 degree hinge limit as Automobility's RearAttachment.
            float relativeYaw = Mth.wrapDegrees(wantedYaw - carYaw);
            wantedYaw = carYaw + Mth.clamp(relativeYaw, -85f, 85f);
            float deltaYaw = Mth.wrapDegrees(wantedYaw - getYRot());
            setYRot(getYRot() + Mth.clamp(deltaYaw, -11f, 11f));

            // Coupler must stay attached to the moving automobile frame,
            // but the axle follows a different curve through corners.
            Vec3 target = hitch.subtract(tongue(getYRot()).scale(DRAWBAR_LENGTH));
            Vec3 displacement = target.subtract(position());
            Vec3 currentVelocity = getDeltaMovement();
            double vx = Mth.clamp(displacement.x * .38 + currentVelocity.x * .32, -.90, .90);
            double vz = Mth.clamp(displacement.z * .38 + currentVelocity.z * .32, -.90, .90);

            // Two-point wheel suspension: spring toward the real collision
            // surfaces beneath left and right wheels, with vertical damping.
            double contactY = desiredWheelHeight(getYRot());
            double targetY = Mth.clamp(contactY, hitch.y - 1.15, hitch.y + 1.15);
            suspensionVelocity = Mth.clamp(suspensionVelocity * .64
                + (targetY - getY()) * .23, -.36, .36);
            setDeltaMovement(vx, suspensionVelocity, vz);
            move(MoverType.SELF, getDeltaMovement());
            // Keep the body from stretching excessively on steep turns,
            // but never teleport through solid block collision.
            if (position().distanceToSqr(target) > 12.25) {
                Vec3 extra = target.subtract(position()).scale(.25);
                move(MoverType.SELF, extra);
            }
            setXRot(0);
            lastTailPosition = hitch;
        } else {
            setDeltaMovement(0, Math.max(-0.35, getDeltaMovement().y - 0.055), 0);
            move(MoverType.SELF, getDeltaMovement());
            if (onGround()) setDeltaMovement(Vec3.ZERO);
        }
        double traveled = Math.hypot(getX() - beforeMove.x, getZ() - beforeMove.z);
        if (traveled > .0005) {
            float next = entityData.get(WHEEL_ROT)
                + (float)(traveled * (360.0 / (2.0 * Math.PI * (3.30 / 16.0))));
            entityData.set(WHEEL_ROT, next % 360f);
        }
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return true;
        // Never destroy a loaded trailer: the 135 inventory slots would be lost.
        if (isLoaded()) return false;
        spawnAtLocation(new ItemStack(EngineItems.BOAT_TRAILER.get()));
        discard();
        return true;
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (isLoaded()) tag.put("LoadedFishingBoat", loadedBoat.copy());
        tag.put("BoatTrailerDisplay", displayData().copy());
        if (towVehicle != null) tag.putUUID("TowVehicle", towVehicle);
        tag.putDouble("SuspensionVelocity", suspensionVelocity);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        loadedBoat = tag.getCompound("LoadedFishingBoat").copy();
        entityData.set(DISPLAY, tag.getCompound("BoatTrailerDisplay").copy());
        towVehicle = tag.hasUUID("TowVehicle") ? tag.getUUID("TowVehicle") : null;
        suspensionVelocity = tag.getDouble("SuspensionVelocity");
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override public ItemStack getPickResult() {
        return new ItemStack(EngineItems.BOAT_TRAILER.get());
    }
}
