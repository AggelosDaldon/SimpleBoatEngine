package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/** User-authored side-mounted fuel upgrade, using its original 128px UV atlas. */
public final class CanisterMesh {
    private static final ResourceLocation ATLAS = new ResourceLocation(SimpleBoatEngine.ID,
        "textures/item/boat_accessories.png");
    private CanisterMesh() {}

    public static void draw(PoseStack pose, MultiBufferSource buffers, int light) {
        MotorMesh.drawAtlas(pose, buffers, ATLAS, CanisterMeshData.PARTS, 128, 0f, light);
    }
}
