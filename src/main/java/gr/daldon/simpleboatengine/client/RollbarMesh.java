package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/** Renders the exact Blockbench roll bar, switching only the four lens UVs. */
public final class RollbarMesh {
    private static final ResourceLocation ATLAS = new ResourceLocation(SimpleBoatEngine.ID,
        "textures/item/rollbar_spotlights.png");

    private RollbarMesh() {}

    public static void draw(PoseStack pose, MultiBufferSource buffers, boolean on, int light) {
        MotorMesh.drawAtlas(pose, buffers, ATLAS, RollbarMeshData.FRAME, 128, 0f, light);
        MotorMesh.drawAtlas(pose, buffers, ATLAS,
            on ? RollbarMeshData.ON_LENSES : RollbarMeshData.OFF_LENSES,
            128, 0f, on ? LightTexture.FULL_BRIGHT : light);
    }
}
