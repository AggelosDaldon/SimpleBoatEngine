package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import gr.daldon.simpleboatengine.common.BlueFishingBoat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Client-side moving record source, synchronized from server-authoritative boat data. */
@Mod.EventBusSubscriber(modid=SimpleBoatEngine.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,
    value=Dist.CLIENT)
public final class BoatRadioAudio {
    private static final Map<UUID,RecordSound> ACTIVE=new HashMap<>();
    private static ClientLevel lastWorld;
    private BoatRadioAudio(){}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft game=Minecraft.getInstance();
        if(game.level!=lastWorld){
            for(RecordSound sound:ACTIVE.values())sound.finish();
            ACTIVE.clear();lastWorld=game.level;
        }
        if(game.level==null||game.isPaused())return;
        for(var entity:game.level.entitiesForRendering()){
            if(!(entity instanceof BlueFishingBoat boat))continue;
            CompoundTag data=ClientMotorState.get(boat);
            boolean playing=data!=null&&data.getBoolean("RadioPlaying")&&
                data.getBoolean("RadioInstalled");
            int serial=data==null?0:data.getInt("RadioSerial");
            RecordSound old=ACTIVE.get(boat.getUUID());
            if(old!=null && (!playing || old.serial!=serial)){
                old.finish();ACTIVE.remove(boat.getUUID());old=null;
            }
            if(!playing||old!=null)continue;
            String soundId=data.getString("RadioSound");
            if(soundId.isEmpty())continue;
            ResourceLocation name=ResourceLocation.tryParse(soundId);
            if(name==null)continue;
            SoundEvent eventSound=ForgeRegistries.SOUND_EVENTS.getValue(name);
            if(eventSound==null)continue;
            RecordSound next=new RecordSound(boat,eventSound,serial);
            ACTIVE.put(boat.getUUID(),next);
            game.getSoundManager().play(next);
        }
        ACTIVE.values().removeIf(sound -> sound.finished);
    }
    private static final class RecordSound extends AbstractTickableSoundInstance {
        private final BlueFishingBoat boat;
        private final int serial;
        private boolean finished;
        RecordSound(BlueFishingBoat boat,SoundEvent source,int serial){
            super(source,SoundSource.RECORDS,SoundInstance.createUnseededRandom());
            this.boat=boat;this.serial=serial;
            looping=false;delay=0;volume=1.0f;pitch=1f;
            updatePosition();
        }
        void finish(){finished=true;stop();}
        @Override public void tick(){
            if(finished || !boat.isAlive()){finish();return;}
            updatePosition();
        }
        private void updatePosition() {
            double yaw = Math.toRadians(180f - boat.getYRot());
            double localX = -.44, localZ = -.94;
            x = boat.getX() + localX * Math.cos(yaw) + localZ * Math.sin(yaw);
            y = boat.getY() + gr.daldon.simpleboatengine.common.BoatMounts.HULL_Y + 1.0;
            z = boat.getZ() - localX * Math.sin(yaw) + localZ * Math.cos(yaw);
        }
    }
}
