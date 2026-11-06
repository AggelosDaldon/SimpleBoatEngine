package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/** Aggelos' 72-cuboid blue fishing boat using its own Blockbench texture atlas. */
public final class BlueBoatMesh {
    public static final ResourceLocation TEXTURE = new ResourceLocation(SimpleBoatEngine.ID,
        "textures/entity/aggelos_blue_fishing_boat.png");
    private static final ResourceLocation NEUTRAL_TEXTURE = new ResourceLocation(SimpleBoatEngine.ID,
        "textures/entity/paintable_fishing_boat.png");

    private BlueBoatMesh() {}

    public static void draw(PoseStack stack, MultiBufferSource buffers, int light) {
        draw(stack, buffers, light, 0);
    }

    public static ResourceLocation texture(int style) {
        if (style < 1 || style > 9) return TEXTURE;
        return new ResourceLocation(SimpleBoatEngine.ID,
            "textures/entity/" + gr.daldon.simpleboatengine.common.EngineItems.WOODS[style - 1]
                + "_fishing_boat.png");
    }

    public static void draw(PoseStack stack, MultiBufferSource buffers, int light, int style) {
        SkiffMesh.drawParts(stack, buffers, light, texture(style), BlueBoatMeshData.PARTS);
    }

    public static void draw(PoseStack stack, MultiBufferSource buffers, int light,
                            int style, int hullPaint, int floorPaint) {
        draw(stack, buffers, light, style,
            new int[]{hullPaint, floorPaint, -1, -1, -1, -1, -1});
    }

    public static void draw(PoseStack stack, MultiBufferSource buffers, int light,
                            int style, int[] paints) {
        boolean painted = false;
        for (int paint : paints) if (paint >= 0) painted = true;
        if (!painted) {
            draw(stack, buffers, light, style);
            return;
        }
        SkiffMesh.drawPainted(stack, buffers, light, texture(style), NEUTRAL_TEXTURE,
            BlueBoatMeshData.PARTS, BlueBoatMeshData.CHANNELS, paints);
    }
}
