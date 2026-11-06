package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import gr.daldon.simpleboatengine.client.ClientMotorState;
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

/** Three small messages: server state, throttle changes, and motor clicks. */
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
}
