package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import gr.daldon.simpleboatengine.common.BlueFishingBoat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=SimpleBoatEngine.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public final class BoatIceBreakerRenderer {
    private BoatIceBreakerRenderer(){}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null)return;
        PoseStack pose=event.getPoseStack();
        var buffers=mc.renderBuffers().bufferSource();
        var camera=event.getCamera().getPosition();
        float tick=event.getPartialTick();
        for(Entity e:mc.level.entitiesForRendering()){
            if(!(e instanceof Boat boat))continue;
            var data=ClientMotorState.get(boat);
            if(data==null || !data.getBoolean("IceBreakerInstalled"))continue;
            pose.pushPose();
            pose.translate(Mth.lerp(tick,boat.xOld,boat.getX())-camera.x,
                Mth.lerp(tick,boat.yOld,boat.getY())-camera.y,
                Mth.lerp(tick,boat.zOld,boat.getZ())-camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(180f-
                Mth.rotLerp(tick,boat.yRotO,boat.getYRot())));
            IceBreakerMesh.draw(pose,buffers,
                LevelRenderer.getLightColor(mc.level,boat.blockPosition()),
                boat instanceof BlueFishingBoat);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
