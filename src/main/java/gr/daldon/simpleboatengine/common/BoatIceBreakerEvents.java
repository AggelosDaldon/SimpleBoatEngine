package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Front-mounted breaker for regular and fishing boats. No changes to non-ice. */
@Mod.EventBusSubscriber(modid=SimpleBoatEngine.ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class BoatIceBreakerEvents {
    private BoatIceBreakerEvents(){}
    @SubscribeEvent public static void install(PlayerInteractEvent.EntityInteract event){
        if(event.getHand()!=InteractionHand.MAIN_HAND || !(event.getTarget() instanceof BlueFishingBoat boat)
            || !event.getEntity().getMainHandItem().is(EngineItems.ICE_BREAKER.get()))return;
        if(!boat.level().isClientSide){
            CompoundTag data=MotorData.writable(boat);
            if(!data.getBoolean("IceBreakerInstalled")){
                ItemStack held=event.getEntity().getMainHandItem();
                data.putBoolean("IceBreakerInstalled",true);
                data.putBoolean("IceBreakerSilk",EnchantmentHelper.getItemEnchantmentLevel(
                    Enchantments.SILK_TOUCH,held)>0);
                if(!event.getEntity().getAbilities().instabuild)held.shrink(1);
                event.getEntity().displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Ice breaker installed on bow."),true);
            }
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(boat.level().isClientSide));
    }

    /** Point the portable recovery kit at an empty boat to pack it as an item. */
    @SubscribeEvent public static void recover(PlayerInteractEvent.EntityInteract event){
        if(event.getHand()!=InteractionHand.MAIN_HAND
            || !(event.getTarget() instanceof Boat boat))return;
        ItemStack kit=event.getEntity().getMainHandItem();
        if(!kit.is(EngineItems.BOAT_RECOVERY_KIT.get()) || BoatRecoveryItem.loaded(kit))
            return;
        if(!boat.level().isClientSide && boat.getPassengers().isEmpty()
            && boat instanceof BlueFishingBoat blue){
            ItemStack saved=blue.getPickResult();
            BoatRecoveryItem.put(kit,saved);
            boat.discard();
            event.getEntity().displayClientMessage(
                net.minecraft.network.chat.Component.literal("Boat captured with its stored data."),true);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(boat.level().isClientSide));
    }

    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
            || level.getGameTime()%3!=0)return;
        for(Entity e:level.getAllEntities()) {
            if(!(e instanceof BlueFishingBoat boat) || !boat.isAlive())continue;
            CompoundTag data=MotorData.read(boat);
            if(!data.getBoolean("IceBreakerInstalled"))continue;
            // Vanilla boats have no built-in synchronized motor data. Keep the
            // ice-breaker visuals updated via our status packet, without an engine.
            if(level.getGameTime()%10==0){
                for(net.minecraft.server.level.ServerPlayer observer:level.players())
                    if(observer.distanceToSqr(boat)<64*64)
                        MotorPackets.sendStatus(boat,observer);
            }
            Vec3 velocity=boat.getDeltaMovement();
            if(velocity.x*velocity.x + velocity.z*velocity.z < .00015
                && boat.getControllingPassenger()==null)continue;
            // Use actual travel direction, or the bow direction when a
            // controlled boat has stopped against a solid ice wall.
            double length=Math.hypot(velocity.x,velocity.z);
            double fx=length>.012?velocity.x/length:-Math.sin(Math.toRadians(boat.getYRot()));
            double fz=length>.012?velocity.z/length:Math.cos(Math.toRadians(boat.getYRot()));
            double reach=2.45;
            for(int side=-1;side<=1;side++){
                double nx=boat.getX()+fx*reach-fz*side*.8;
                double nz=boat.getZ()+fz*reach+fx*side*.8;
                for(int off=0;off<=1;off++){
                    BlockPos pos=BlockPos.containing(nx,boat.getY()+off,nz);
                    BlockState state=level.getBlockState(pos);
                    if(!(state.is(Blocks.ICE)||state.is(Blocks.PACKED_ICE)||
                        state.is(Blocks.BLUE_ICE)||state.is(Blocks.FROSTED_ICE)))continue;
                    boolean silk=data.getBoolean("IceBreakerSilk");
                    ItemStack drop=new ItemStack(state.getBlock());
                    if(!level.setBlock(pos,Blocks.WATER.defaultBlockState(),3))continue;
                    if(silk && !state.is(Blocks.FROSTED_ICE)){
                        if(!storeIce(boat,drop.copy()))
                            net.minecraft.world.Containers.dropItemStack(level,
                                pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,drop);
                    }
                    level.playSound(null,pos,SoundEvents.GLASS_BREAK,SoundSource.BLOCKS,.5f,1.0f);
                }
            }
        }
    }
    /** Fishing boats use our 108-slot hold; vanilla chest boats use their
     * normal 27-slot inventory. Regular vanilla boats drop the ice nearby.
     */
    private static boolean storeIce(Boat boat,ItemStack incoming){
        if(boat instanceof BlueFishingBoat fishing)return fishing.storeIce(incoming);
        if(boat instanceof net.minecraft.world.Container storage) {
            for(int slot=0;slot<storage.getContainerSize();slot++) {
                ItemStack have=storage.getItem(slot);
                if(have.isEmpty()){
                    storage.setItem(slot,incoming.copy());
                    storage.setChanged();
                    return true;
                }
                if(ItemStack.isSameItemSameTags(have,incoming)
                   && have.getCount()+incoming.getCount()<=have.getMaxStackSize()){
                    have.grow(incoming.getCount());
                    storage.setChanged();
                    return true;
                }
            }
        }
        return false;
    }

}
