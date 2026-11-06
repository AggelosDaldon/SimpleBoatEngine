package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/** Same metal pole in both states; only the lens switches UVs and brightness. */
public final class PoleLightMesh {
    private static final ResourceLocation ATLAS = new ResourceLocation(SimpleBoatEngine.ID,
        "textures/item/pole_light.png");
    private PoleLightMesh() {}

    public static void draw(PoseStack pose, MultiBufferSource buffers, boolean on, int light) {
        MotorMesh.drawAtlas(pose, buffers, ATLAS, PoleLightMeshData.BASE, 128, 0f, light);
        MotorMesh.drawAtlas(pose, buffers, ATLAS,
            on ? PoleLightMeshData.ON_LENS : PoleLightMeshData.OFF_LENS,
            128, 0f, on ? LightTexture.FULL_BRIGHT : light);
    }
}
