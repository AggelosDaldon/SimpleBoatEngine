package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import gr.daldon.simpleboatengine.common.BlueFishingBoat;
import gr.daldon.simpleboatengine.common.EngineItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import java.util.UUID;

/** A dark lead-like line from the winch rope to a selected fishing boat. */
@Mod.EventBusSubscriber(modid = SimpleBoatEngine.ID, bus=Mod.EventBusSubscriber.Bus.FORGE,
    value=Dist.CLIENT)
public final class BoatWinchRopeRenderer {
    private BoatWinchRopeRenderer() {}

    @SubscribeEvent
    public static void draw(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        ItemStack rope = minecraft.player.getMainHandItem();
        if (!rope.is(EngineItems.WINCH_ROPE.get()))
            rope = minecraft.player.getOffhandItem();
        if (!rope.is(EngineItems.WINCH_ROPE.get()) || !rope.hasTag()
            || !rope.getTag().hasUUID("BoatWinchTarget")) return;
        UUID targetId = rope.getTag().getUUID("BoatWinchTarget");
        BlueFishingBoat boat = null;
        for (Entity e : minecraft.level.entitiesForRendering()) {
            if (e instanceof BlueFishingBoat b && e.getUUID().equals(targetId)) {
                boat = b;
                break;
            }
        }
        if (boat == null || minecraft.player.distanceToSqr(boat) > 400) return;

        float partial = event.getPartialTick();
        Vec3 origin = minecraft.player.getEyePosition(partial)
            .add(minecraft.player.getViewVector(partial).scale(.28))
            .add(0, -.36, 0);
        Vec3 destination = new Vec3(
            boat.xOld + (boat.getX() - boat.xOld) * partial,
            boat.yOld + (boat.getY() - boat.yOld) * partial + .65,
            boat.zOld + (boat.getZ() - boat.zOld) * partial);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        var source = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = source.getBuffer(RenderType.lines());
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        for (int i = 0; i < 20; i++) {
            float t0 = i / 20f, t1 = (i+1) / 20f;
            Vec3 a = point(origin,destination,t0).subtract(camera);
            Vec3 b = point(origin,destination,t1).subtract(camera);
            Vec3 direction = b.subtract(a).normalize();
            vertices.vertex(matrix,(float)a.x,(float)a.y,(float)a.z)
                .color(14,14,18,255).normal(normal,
                    (float)direction.x,(float)direction.y,(float)direction.z).endVertex();
            vertices.vertex(matrix,(float)b.x,(float)b.y,(float)b.z)
                .color(14,14,18,255).normal(normal,
                    (float)direction.x,(float)direction.y,(float)direction.z).endVertex();
        }
        source.endBatch(RenderType.lines());
    }

    private static Vec3 point(Vec3 a, Vec3 b, float t) {
        double droop = Math.sin(Math.PI*t)*.22;
        return a.lerp(b,t).add(0,-droop,0);
    }
}
