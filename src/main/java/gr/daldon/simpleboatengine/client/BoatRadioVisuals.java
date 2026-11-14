package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/** Draws the radio at its authored location in the same coordinates as the hull. */
public final class BoatRadioVisuals {
    private static final ResourceLocation TEXTURE = new ResourceLocation(SimpleBoatEngine.ID,
        "textures/entity/mounted_boat_radio.png");

    private BoatRadioVisuals() {}

    public static void draw(PoseStack pose, MultiBufferSource buffers, int light) {
        float x = BoatRadioMeshData.X / 16f;
        float y = BoatRadioMeshData.Y / 16f;
        float z = BoatRadioMeshData.Z / 16f;
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(BoatRadioMeshData.YAW));
        pose.translate(-x, -y, -z);
        SkiffMesh.drawParts(pose, buffers, light, TEXTURE, BoatRadioMeshData.PARTS);
        pose.popPose();
    }
}
