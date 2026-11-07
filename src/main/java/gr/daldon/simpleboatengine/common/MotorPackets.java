package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import gr.daldon.simpleboatengine.client.ClientMotorState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeItem;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Synced engine controls, precise hatch interaction and click-to-board handling. */
public final class MotorPackets {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(SimpleBoatEngine.ID, "motor"),
        () -> VERSION, VERSION::equals, VERSION::equals);
    private MotorPackets() {}

    public static void initialize() {
        CHANNEL.messageBuilder(Status.class, 0, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Status::write).decoder(Status::read).consumerMainThread(Status::receive).add();
        CHANNEL.messageBuilder(SetThrottle.class, 1, NetworkDirection.PLAY_TO_SERVER)
            .encoder(SetThrottle::write).decoder(SetThrottle::read)
            .consumerMainThread(SetThrottle::receive).add();
        CHANNEL.messageBuilder(UseMotor.class, 2, NetworkDirection.PLAY_TO_SERVER)
            .encoder(UseMotor::write).decoder(UseMotor::read).consumerMainThread(UseMotor::receive).add();
        CHANNEL.messageBuilder(OpenCargo.class, 3, NetworkDirection.PLAY_TO_SERVER)
            .encoder(OpenCargo::write).decoder(OpenCargo::read)
            .consumerMainThread(OpenCargo::receive).add();
        CHANNEL.messageBuilder(BoardBoat.class, 4, NetworkDirection.PLAY_TO_SERVER)
            .encoder(BoardBoat::write).decoder(BoardBoat::read)
            .consumerMainThread(BoardBoat::receive).add();
        CHANNEL.messageBuilder(CustomizeBoat.class, 5, NetworkDirection.PLAY_TO_SERVER)
            .encoder(CustomizeBoat::write).decoder(CustomizeBoat::read)
            .consumerMainThread(CustomizeBoat::receive).add();
        CHANNEL.messageBuilder(ScrollCargo.class, 6, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ScrollCargo::write).decoder(ScrollCargo::read)
            .consumerMainThread(ScrollCargo::receive).add();
        CHANNEL.messageBuilder(TrailerInteract.class, 7, NetworkDirection.PLAY_TO_SERVER)
            .encoder(TrailerInteract::write).decoder(TrailerInteract::read)
            .consumerMainThread(TrailerInteract::receive).add();
    }

    public static void sendStatus(Boat boat, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new Status(boat.getId(), MotorData.snapshot(boat)));
    }

    public record Status(int entityId, CompoundTag values) {
        static void write(Status msg, FriendlyByteBuf out) {
            out.writeVarInt(msg.entityId);
            out.writeNbt(msg.values);
        }
        static Status read(FriendlyByteBuf in) {
            return new Status(in.readVarInt(), in.readNbt());
        }
        static void receive(Status msg, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientMotorState.accept(msg.entityId(), msg.values()));
            ctx.get().setPacketHandled(true);
        }
    }

    public record SetThrottle(int step) {
        static void write(SetThrottle msg, FriendlyByteBuf out) { out.writeVarInt(msg.step); }
        static SetThrottle read(FriendlyByteBuf in) { return new SetThrottle(in.readVarInt()); }
        static void receive(SetThrottle msg, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (player == null || !(player.getVehicle() instanceof Boat boat)
                    || boat.getControllingPassenger() != player || !MotorData.running(boat)) return;
                CompoundTag values = MotorData.writable(boat);
                int next = values.getInt("Throttle") + Integer.signum(msg.step());
                values.putInt("Throttle", Math.max(0, Math.min(next, MotorData.throttleLimit(values))));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record UseMotor(int boatId) {
        static void write(UseMotor msg, FriendlyByteBuf out) { out.writeVarInt(msg.boatId); }
        static UseMotor read(FriendlyByteBuf in) { return new UseMotor(in.readVarInt()); }
        static void receive(UseMotor msg, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (player == null || !(player.level().getEntity(msg.boatId()) instanceof Boat boat)) return;
                ServerMotorEvents.useMotor(player, boat);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record OpenCargo(int boatId) {
        static void write(OpenCargo msg, FriendlyByteBuf out) { out.writeVarInt(msg.boatId); }
        static OpenCargo read(FriendlyByteBuf in) { return new OpenCargo(in.readVarInt()); }
        static void receive(OpenCargo msg, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (player == null || !(player.level().getEntity(msg.boatId()) instanceof BlueFishingBoat boat)
                    || player.distanceToSqr(boat) > 40.0 || !boat.isAlive()
                    || !BoatInteractionTarget.aimedAtHatch(boat, player.getEyePosition(),
                        player.getViewVector(1f), 5.5)) return;
                boat.openCargo(player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record BoardBoat(int boatId) {
        static void write(BoardBoat msg, FriendlyByteBuf out) { out.writeVarInt(msg.boatId); }
        static BoardBoat read(FriendlyByteBuf in) { return new BoardBoat(in.readVarInt()); }
        static void receive(BoardBoat msg, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (player == null || !(player.level().getEntity(msg.boatId()) instanceof BlueFishingBoat boat)
                    || player.getVehicle() != null || !player.isInWater() || !boat.isAlive()
                    || boat.getPassengers().size() >= 2 || player.distanceToSqr(boat) > 40.0
                    || !BoatInteractionTarget.aimedAtHull(boat, player.getEyePosition(),
                        player.getViewVector(1f), 5.5)) return;
                player.startRiding(boat);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /**
     * Reliable interaction for the full 3D hull and trailer instead of relying
     * on Minecraft's much smaller default entity selection boxes.
     * 0 = mark boat with rope, 1 = winch boat, 2 = attach trailer to car,
     * 3 = unload trailer. Every action is validated server-side.
     */
    public record TrailerInteract(int targetId, int action) {
        static void write(TrailerInteract m, FriendlyByteBuf b) {
            b.writeVarInt(m.targetId);
            b.writeVarInt(m.action);
        }
        static TrailerInteract read(FriendlyByteBuf b) {
            return new TrailerInteract(b.readVarInt(), b.readVarInt());
        }
        static void receive(TrailerInteract m, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player == null || m.action < 0 || m.action > 3) return;
                var entity = player.level().getEntity(m.targetId());
                if (entity == null || !entity.isAlive() ||
                    player.distanceToSqr(entity) > 100.0) return;
                var hand = player.getMainHandItem();
                var eye = player.getEyePosition();
                var view = player.getViewVector(1f);
                if (m.action == 0 && hand.is(EngineItems.WINCH_ROPE.get())
                    && entity instanceof BlueFishingBoat boat
                    && BoatInteractionTarget.aimedAtHull(boat, eye, view, 7.5)) {
                    hand.getOrCreateTag().putUUID("BoatWinchTarget", boat.getUUID());
                    player.displayClientMessage(Component.literal(
                        "Boat selected. Right-click the trailer with the rope."), true);
                } else if (m.action == 1 && hand.is(EngineItems.WINCH_ROPE.get())
                    && entity instanceof BoatTrailer trailer
                    && TrailerTargets.aimedAtTrailer(trailer, eye, view, 7.5)) {
                    trailer.loadBoat(player, hand);
                } else if (m.action == 2 && hand.is(EngineItems.BOAT_TRAILER.get())
                    && BoatTrailer.automobile(entity)
                    && TrailerTargets.aimedAtCar(entity, eye, view, 7.5)) {
                    BoatTrailer trailer = EngineEntities.BOAT_TRAILER.get().create(player.level());
                    if (trailer == null) return;
                    var hitch = BoatTrailer.couplingPosition(entity);
                    trailer.setPos(hitch.x, hitch.y, hitch.z);
                    trailer.setYRot(entity.getYRot());
                    trailer.coupleTo(entity);
                    if (player.level().addFreshEntity(trailer)) {
                        if (!player.getAbilities().instabuild) hand.shrink(1);
                        player.displayClientMessage(Component.literal(
                            "Fishing boat trailer attached to car."), true);
                    }
                } else if (m.action == 3 && hand.isEmpty()
                    && player.isShiftKeyDown() && entity instanceof BoatTrailer trailer
                    && TrailerTargets.aimedAtTrailer(trailer, eye, view, 7.5)) {
                    trailer.unloadBoat(player);
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    /** Only scroll the inventory of the fishing boat currently open by the sender. */
    public record ScrollCargo(int boatId, int firstRow) {
        static void write(ScrollCargo msg, FriendlyByteBuf out) {
            out.writeVarInt(msg.boatId);
            out.writeVarInt(msg.firstRow);
        }
        static ScrollCargo read(FriendlyByteBuf in) {
            return new ScrollCargo(in.readVarInt(), in.readVarInt());
        }
        static void receive(ScrollCargo msg, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player == null || !(player.containerMenu instanceof BoatCargoMenu menu)
                    || menu.boatId() != msg.boatId()
                    || !(player.level().getEntity(msg.boatId()) instanceof BlueFishingBoat boat)
                    || player.distanceToSqr(boat) > 49 || !boat.isAlive()) return;
                menu.setStartRow(msg.firstRow());
            });
            context.get().setPacketHandled(true);
        }
    }

    /** Server accepts edits only while the requesting player has this station open. */
    public record CustomizeBoat(BlockPos pos, int channel, int color, String name) {
        static void write(CustomizeBoat msg, FriendlyByteBuf out) {
            out.writeBlockPos(msg.pos);
            out.writeVarInt(msg.channel);
            out.writeVarInt(msg.color);
            out.writeUtf(msg.name, 32);
        }
        static CustomizeBoat read(FriendlyByteBuf in) {
            return new CustomizeBoat(in.readBlockPos(), in.readVarInt(), in.readVarInt(), in.readUtf(32));
        }
        static void receive(CustomizeBoat msg, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (player == null || !(player.containerMenu instanceof BoatCustomizerMenu menu)
                    || !msg.pos.equals(menu.getBlockPos())
                    || player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5,
                        msg.pos.getZ() + 0.5) > 64
                    || !(player.level().getBlockEntity(msg.pos) instanceof BoatCustomizerBlockEntity workshop)) return;
                ItemStack item = workshop.getItem(0);
                if (!(item.getItem() instanceof SkiffBoatItem)) return;
                if (msg.channel == -1) {
                    BoatPaint.setName(item, msg.name);
                } else if (msg.channel == -4) {
                    ItemStack material = workshop.getItem(2);
                    int wood = EngineItems.woodStyleFor(material);
                    if (wood < 1) return;
                    int previous = item.getItem() instanceof SkiffBoatItem current
                        ? BoatPaint.woodStyle(item, current.getHullStyle()) : 0;
                    if (previous == wood) return;
                    ItemStack replacement = new ItemStack(EngineItems.boatForStyle(wood));
                    if (item.hasTag()) replacement.setTag(item.getTag().copy());
                    BoatPaint.setWoodStyle(replacement, wood);
                    item = replacement;
                    if (!player.getAbilities().instabuild) {
                        material.shrink(1);
                        workshop.setItem(2, material);
                    }
                } else if (msg.channel == -2 || msg.channel == -3) {
                    ItemStack input = workshop.getItem(3);
                    if (msg.channel == -2 && input.getItem() instanceof DyeItem dye) {
                        if (BoatPaint.nameColor(item) != dye.getDyeColor().getId()) {
                            BoatPaint.setNameColor(item, dye.getDyeColor().getId());
                            if (!player.getAbilities().instabuild) {
                                input.shrink(1);
                                workshop.setItem(3, input);
                            }
                        }
                    } else if (msg.channel == -3 && input.is(net.minecraft.world.item.Items.GLOW_INK_SAC)) {
                        if (!BoatPaint.nameGlows(item)) {
                            BoatPaint.setNameGlow(item, true);
                            if (!player.getAbilities().instabuild) {
                                input.shrink(1);
                                workshop.setItem(1, input);
                            }
                        }
                    } else {
                        player.displayClientMessage(Component.literal("Place dye or glow ink in the slot."), true);
                        return;
                    }
                } else {
                    if (msg.channel < 0 || msg.channel >= BoatPaint.CHANNELS) return;
                    // Dyes must now be placed in the actual station slot.
                    // Normal paints inherit the placed dye's exact vanilla color.
                    ItemStack dye = workshop.getItem(1);
                    if (!(dye.getItem() instanceof DyeItem dyeItem)) {
                        player.displayClientMessage(Component.literal("Put a dye into the station first."), true);
                        return;
                    }
                    int selectedColor;
                    if (msg.channel == BoatPaint.ANTIFOUL) {
                        if (msg.color < 0 || msg.color >= 5) return;
                        selectedColor = msg.color;
                    } else {
                        selectedColor = dyeItem.getDyeColor().getId();
                    }
                    if (BoatPaint.get(item, msg.channel) == selectedColor) return;
                    BoatPaint.set(item, msg.channel, selectedColor);
                    if (!player.getAbilities().instabuild) {
                        dye.shrink(1);
                        workshop.setItem(1, dye);
                    }
                }
                workshop.setItem(0, item);
                menu.broadcastChanges();
                player.displayClientMessage(Component.literal(msg.channel == -1
                    ? "Boat name saved: " + BoatPaint.name(item) : "Boat paint applied!"), true);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
