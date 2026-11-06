package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SimpleBoatEngine.ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
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
            float height = boat.getBbHeight() / 3f;
            float stern = 1.24f;
            if (boat.getVariant() == Boat.Type.BAMBOO) {
                height = boat.getBbHeight() * 0.7f;
                stern = 1.12f;
            }
            pose.pushPose();
            pose.translate(0, height, stern);
            pose.scale(1.5f, 1.5f, 1.5f);
            int throttle = Math.max(0, Math.min(10, status.getInt("Throttle")));
            float angle = status.getBoolean("Running")
                ? (boat.tickCount + tick) * (0.12f + throttle * 0.048f) : 0f;
            MotorMesh.draw(pose, buffers, angle,
                LevelRenderer.getLightColor(game.level, boat.blockPosition()));
            pose.popPose();

            if (status.getBoolean("CanisterInstalled")) {
                pose.pushPose();
                // Starboard stern: close to the right outer edge of the vanilla hull.
                // +X is the boat's right side in our stern-relative rendering space.
                pose.translate(0.73f, boat.getBbHeight() * 0.34f, 0.83f);
                pose.mulPose(Axis.YP.rotationDegrees(90f));
                pose.scale(1.3f, 1.3f, 1.3f);
                CanisterMesh.draw(pose, buffers,
                    LevelRenderer.getLightColor(game.level, boat.blockPosition()));
                pose.popPose();
            }
            pose.popPose();
        }
        buffers.endBatch();
    }
}
