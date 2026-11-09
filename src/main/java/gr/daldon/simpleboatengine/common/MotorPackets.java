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
        CHANNEL.messageBuilder(SelectPaintStand.class, 7, NetworkDirection.PLAY_TO_SERVER)
            .encoder(SelectPaintStand::write).decoder(SelectPaintStand::read)
            .consumerMainThread(SelectPaintStand::receive).add();
        CHANNEL.messageBuilder(BoatPaintDraft.class, 8, NetworkDirection.PLAY_TO_SERVER)
            .encoder(BoatPaintDraft::write).decoder(BoatPaintDraft::read)
            .consumerMainThread(BoatPaintDraft::receive).add();
        CHANNEL.messageBuilder(OpenParkedCargo.class, 9, NetworkDirection.PLAY_TO_SERVER)
            .encoder(OpenParkedCargo::write).decoder(OpenParkedCargo::read)
            .consumerMainThread(OpenParkedCargo::receive).add();
        CHANNEL.messageBuilder(CycleParkedCargo.class, 10, NetworkDirection.PLAY_TO_SERVER)
            .encoder(CycleParkedCargo::write).decoder(CycleParkedCargo::read)
            .consumerMainThread(CycleParkedCargo::receive).add();
        CHANNEL.messageBuilder(BuilderAction.class, 11, NetworkDirection.PLAY_TO_SERVER)
            .encoder(BuilderAction::write).decoder(BuilderAction::read)
            .consumerMainThread(BuilderAction::receive).add();
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

    /** Server-authoritative blueprint selection and material consumption. */
    public record BuilderAction(BlockPos pos,int delta,int craft) {
        static void write(BuilderAction msg,FriendlyByteBuf out){
            out.writeBlockPos(msg.pos);out.writeVarInt(msg.delta);out.writeVarInt(msg.craft);
        }
        static BuilderAction read(FriendlyByteBuf in){
            return new BuilderAction(in.readBlockPos(),in.readVarInt(),in.readVarInt());
        }
        static void receive(BuilderAction msg,Supplier<NetworkEvent.Context> ctx){
            ServerPlayer player=ctx.get().getSender();
            ctx.get().enqueueWork(()->{
                if(player==null || !(player.containerMenu instanceof BoatBuilderMenu menu)
                   || !menu.position().equals(msg.pos)
                   || player.distanceToSqr(msg.pos.getX()+.5,
                       msg.pos.getY()+.5,msg.pos.getZ()+.5)>64)return;
                if(msg.craft==2)menu.selectTab(true);
                else if(msg.craft==3)menu.selectTab(false);
                else if(Math.abs(msg.delta)==1)menu.change(msg.delta);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record SelectPaintStand(BlockPos table, int direction) {
        static void write(SelectPaintStand msg, FriendlyByteBuf out) {
            out.writeBlockPos(msg.table); out.writeVarInt(msg.direction);
        }
        static SelectPaintStand read(FriendlyByteBuf in) {
            return new SelectPaintStand(in.readBlockPos(), in.readVarInt());
        }
        static void receive(SelectPaintStand msg, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player=context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player==null || !(player.containerMenu instanceof BoatCustomizerMenu menu)
                    || !menu.getBlockPos().equals(msg.table)
                    || player.distanceToSqr(msg.table.getX()+.5,msg.table.getY()+.5,msg.table.getZ()+.5)>64
                    || !(player.level().getBlockEntity(msg.table) instanceof BoatCustomizerBlockEntity table))
                    return;
                menu.selectRelative(msg.direction);
                menu.broadcastChanges();
            });
            context.get().setPacketHandled(true);
        }
    }

    /** All proposed values are checked server-side against the player menu. */
    public record BoatPaintDraft(BlockPos table, BlockPos stand, int[] colors,
                                 String name, int nameColor, int attachments, int action) {
        static void write(BoatPaintDraft msg, FriendlyByteBuf out) {
            out.writeBlockPos(msg.table);out.writeBlockPos(msg.stand);
            for(int c:msg.colors)out.writeVarInt(c);
            out.writeUtf(msg.name,32);out.writeVarInt(msg.nameColor);
            out.writeVarInt(msg.attachments);out.writeVarInt(msg.action);
        }
        static BoatPaintDraft read(FriendlyByteBuf in) {
            BlockPos table=in.readBlockPos(), stand=in.readBlockPos();
            int[] colors=new int[BoatPaint.CHANNELS];
            for(int i=0;i<colors.length;i++)colors[i]=in.readVarInt();
            return new BoatPaintDraft(table,stand,colors,in.readUtf(32),
                in.readVarInt(),in.readVarInt(),in.readVarInt());
        }
        static void receive(BoatPaintDraft msg, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player=ctx.get().getSender();
            ctx.get().enqueueWork(()->{
                if(player==null || !(player.containerMenu instanceof BoatCustomizerMenu menu)
                    || !menu.getBlockPos().equals(msg.table)
                    || player.distanceToSqr(msg.table.getX()+.5,msg.table.getY()+.5,msg.table.getZ()+.5)>64)
                    return;
                boolean success=BoatPaintSessions.execute(player,menu,msg.stand,msg.colors,
                    msg.name,msg.nameColor,msg.attachments,msg.action);
                if(success && (msg.action==BoatPaintSessions.CANCEL
                    || msg.action==BoatPaintSessions.PREVIEW)) {
                    // Close only after server has restored or staged the actual boat.
                    player.closeContainer();
                } else menu.broadcastChanges();
            });
            ctx.get().setPacketHandled(true);
        }
    }


    public record OpenParkedCargo(BlockPos table,BlockPos stand) {
        static void write(OpenParkedCargo msg,FriendlyByteBuf out){
            out.writeBlockPos(msg.table);out.writeBlockPos(msg.stand);
        }
        static OpenParkedCargo read(FriendlyByteBuf in){
            return new OpenParkedCargo(in.readBlockPos(),in.readBlockPos());
        }
        static void receive(OpenParkedCargo msg,Supplier<NetworkEvent.Context> ctx){
            ServerPlayer player=ctx.get().getSender();
            ctx.get().enqueueWork(()->{
                if(player==null || !(player.containerMenu instanceof BoatCustomizerMenu menu)
                    || !menu.getBlockPos().equals(msg.table)
                    || !msg.stand.equals(menu.selectedStandPos())
                    || player.distanceToSqr(msg.table.getX()+.5,msg.table.getY()+.5,msg.table.getZ()+.5)>64
                    || !(player.level().getBlockEntity(msg.stand) instanceof BoatStandBlockEntity stand)
                    || stand.getItem(0).isEmpty()
                    || stand.previewLockedFor(player.getUUID()))return;
                net.minecraftforge.network.NetworkHooks.openScreen(player,
                    new net.minecraft.world.SimpleMenuProvider(
                        (id,inv,owner)->new ParkedBoatCargoMenu(id,inv,stand),
                        Component.literal("Parked boat inventory")),
                    buf->buf.writeBlockPos(msg.stand));
            });
            ctx.get().setPacketHandled(true);
        }
    }
    public record CycleParkedCargo(BlockPos stand,int delta) {
        static void write(CycleParkedCargo msg,FriendlyByteBuf out){
            out.writeBlockPos(msg.stand);out.writeVarInt(msg.delta);
        }
        static CycleParkedCargo read(FriendlyByteBuf in){
            return new CycleParkedCargo(in.readBlockPos(),in.readVarInt());
        }
        static void receive(CycleParkedCargo msg,Supplier<NetworkEvent.Context> ctx){
            ServerPlayer player=ctx.get().getSender();
            ctx.get().enqueueWork(()->{
                if(player==null || !(player.containerMenu instanceof ParkedBoatCargoMenu menu)
                    || !menu.standPos().equals(msg.stand))return;
                menu.switchPage(msg.delta);
            });
            ctx.get().setPacketHandled(true);
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
                    || !(player.level().getBlockEntity(msg.pos) instanceof BoatCustomizerBlockEntity table)) return;
                BoatCustomizerBlockEntity workshop;
                if (menu.standCount()>0) {
                    BoatStandBlockEntity chosen=menu.selectedStand();
                    if (chosen==null || chosen.getItem(0).isEmpty()) return;
                    workshop=chosen;
                } else workshop=table;
                ItemStack item = workshop.getItem(0);
                if (!(item.getItem() instanceof SkiffBoatItem)) return;
                if (msg.channel == -1) {
                    BoatPaint.setName(item, msg.name);
                } else if (msg.channel == -4) {
                    ItemStack material = table.getItem(2);
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
                        table.setItem(2, material);
                    }
                } else if (msg.channel == -2 || msg.channel == -3) {
                    ItemStack input = table.getItem(3);
                    if (msg.channel == -2 && input.getItem() instanceof DyeItem dye) {
                        if (BoatPaint.nameColor(item) != dye.getDyeColor().getId()) {
                            BoatPaint.setNameColor(item, dye.getDyeColor().getId());
                            if (!player.getAbilities().instabuild) {
                                input.shrink(1);
                                table.setItem(3, input);
                            }
                        }
                    } else if (msg.channel == -3 && input.is(net.minecraft.world.item.Items.GLOW_INK_SAC)) {
                        if (!BoatPaint.nameGlows(item)) {
                            BoatPaint.setNameGlow(item, true);
                            if (!player.getAbilities().instabuild) {
                                input.shrink(1);
                                table.setItem(1, input);
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
                    ItemStack dye = table.getItem(1);
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
                        table.setItem(1, dye);
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
