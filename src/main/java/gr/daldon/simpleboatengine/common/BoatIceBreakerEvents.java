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
        if(event.getHand()!=InteractionHand.MAIN_HAND || !(event.getTarget() instanceof Boat boat)
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

    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
            || level.getGameTime()%3!=0)return;
        for(Entity e:level.getAllEntities()) {
            if(!(e instanceof Boat boat) || !boat.isAlive())continue;
            CompoundTag data=MotorData.read(boat);
            if(!data.getBoolean("IceBreakerInstalled"))continue;
            Vec3 velocity=boat.getDeltaMovement();
            if(velocity.x*velocity.x + velocity.z*velocity.z < .00015)continue;
            // Velocity determines which side is really forward even in reversing turns.
            double length=Math.hypot(velocity.x,velocity.z);
            double fx=velocity.x/length, fz=velocity.z/length;
            double reach=boat instanceof BlueFishingBoat?2.45:1.12;
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
                        if(!(boat instanceof BlueFishingBoat fishing
                             && fishing.storeIce(drop.copy())))
                            net.minecraft.world.Containers.dropItemStack(level,
                                pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,drop);
                    }
                    level.playSound(null,pos,SoundEvents.GLASS_BREAK,SoundSource.BLOCKS,.5f,1.0f);
                }
            }
        }
    }
}
