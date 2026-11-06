package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/** Aggelos' 72-cuboid blue fishing boat using its own Blockbench texture atlas. */
public final class BlueBoatMesh {
    public static final ResourceLocation TEXTURE = new ResourceLocation(SimpleBoatEngine.ID,
        "textures/entity/aggelos_blue_fishing_boat.png");

    private BlueBoatMesh() {}

    public static void draw(PoseStack stack, MultiBufferSource buffers, int light) {
        SkiffMesh.drawParts(stack, buffers, light, TEXTURE, BlueBoatMeshData.PARTS);
    }
}
