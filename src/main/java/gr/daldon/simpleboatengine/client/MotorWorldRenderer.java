package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import gr.daldon.simpleboatengine.common.MotorData;
import gr.daldon.simpleboatengine.common.BoatMounts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SimpleBoatEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT)
public final class MotorWorldRenderer {
    private MotorWorldRenderer() {}

    @SubscribeEvent
    public static void afterBoats(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft game = Minecraft.getInstance();
        if (game.level == null || game.player == null) return;
        var buffers = game.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        float tick = event.getPartialTick();

        for (Entity entity : game.level.entitiesForRendering()) {
            if (!(entity instanceof Boat boat)) continue;
            var status = ClientMotorState.get(boat);
            if (status == null || !status.getBoolean("Installed")) continue;
            pose.pushPose();
            pose.translate(Mth.lerp(tick, boat.xOld, boat.getX()) - camera.x,
                Mth.lerp(tick, boat.yOld, boat.getY()) - camera.y,
                Mth.lerp(tick, boat.zOld, boat.getZ()) - camera.z);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                180f - Mth.rotLerp(tick, boat.yRotO, boat.getYRot())));
            var motor = BoatMounts.motor(boat);
            pose.pushPose();
            pose.translate(motor.x(), motor.y(), motor.z());
            pose.scale(1.5f, 1.5f, 1.5f);
            int throttle = Math.max(0, Math.min(10, status.getInt("Throttle")));
            float angle = status.getBoolean("Running")
                ? (boat.tickCount + tick) * (0.12f + throttle * 0.048f) : 0f;
            if (boat instanceof gr.daldon.simpleboatengine.common.BlueFishingBoat)
                MotorMesh.drawFishing(pose, buffers, angle,
                    LevelRenderer.getLightColor(game.level, boat.blockPosition()));
            else
                MotorMesh.draw(pose, buffers, angle,
                    LevelRenderer.getLightColor(game.level, boat.blockPosition()));
            pose.popPose();

            if (status.getBoolean("CanisterInstalled")) {
                pose.pushPose();
                var canister=BoatMounts.canister(boat);
                pose.translate(canister.x(),canister.y(),canister.z());
                pose.mulPose(Axis.YP.rotationDegrees(-90f));
                pose.scale(1.3f,1.3f,1.3f);
                CanisterMesh.draw(pose,buffers,
                    LevelRenderer.getLightColor(game.level,boat.blockPosition()));
                pose.popPose();
            }
            if (status.getBoolean("PoleLightInstalled")) {
                pose.pushPose();
                var lamp = BoatMounts.poleLight(boat);
                pose.translate(lamp.x(), lamp.y(), lamp.z());
                pose.scale(1.3f, 1.3f, 1.3f);
                PoleLightMesh.draw(pose, buffers,
                    MotorData.isDark(game.level.getDayTime()),
                    LevelRenderer.getLightColor(game.level, boat.blockPosition()));
                pose.popPose();
            }
            pose.popPose();
        }
        buffers.endBatch();
    }
}
