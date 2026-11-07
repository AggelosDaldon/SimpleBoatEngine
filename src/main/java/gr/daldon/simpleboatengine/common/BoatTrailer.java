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
        if (automobile(automobile)) towVehicle = automobile.getUUID();
    }
    public static boolean automobile(Entity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && id.getNamespace().equals("automobility")
            && id.getPath().equals("automobile");
    }
    public static Vec3 couplingPosition(Entity car) {
        return tail(car).add(new Vec3(0, 0, 3.75)
            .yRot((float)Math.toRadians(180 - car.getYRot())));
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
            Vec3 hitch = tail(tow);
            Vec3 target = couplingPosition(tow);
            // Smooth trailing motion; the car controls direction and speed.
            Vec3 movement = target.subtract(position());
            setDeltaMovement(movement.scale(0.55));
            setPos(getX() + movement.x * 0.55, target.y - 0.35, getZ() + movement.z * 0.55);
            setYRot(tow.getYRot());
            setXRot(0);
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
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        loadedBoat = tag.getCompound("LoadedFishingBoat").copy();
        entityData.set(DISPLAY, tag.getCompound("BoatTrailerDisplay").copy());
        towVehicle = tag.hasUUID("TowVehicle") ? tag.getUUID("TowVehicle") : null;
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override public ItemStack getPickResult() {
        return new ItemStack(EngineItems.BOAT_TRAILER.get());
    }
}
