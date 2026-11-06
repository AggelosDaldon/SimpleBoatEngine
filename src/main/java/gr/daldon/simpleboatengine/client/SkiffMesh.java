package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Exact 128px atlas and rotated cuboids of the supplied large fishing skiff. */
public final class SkiffMesh {
    public static final ResourceLocation TEXTURE = new ResourceLocation(SimpleBoatEngine.ID,
        "textures/entity/large_motor_fishing_skiff.png");
    private static final int[][] CORNERS = {
        {0, 3, 2, 1}, {5, 6, 7, 4}, {4, 7, 3, 0},
        {1, 2, 6, 5}, {3, 7, 6, 2}, {4, 0, 1, 5}
    };
    private static final float[][] NORMALS = {
        {0, 0, -1}, {0, 0, 1}, {-1, 0, 0},
        {1, 0, 0}, {0, 1, 0}, {0, -1, 0}
    };
    private SkiffMesh() {}

    public static void draw(PoseStack stack, MultiBufferSource buffers, int light) {
        drawParts(stack, buffers, light, TEXTURE, SkiffMeshData.PARTS);
    }

    static void drawParts(PoseStack stack, MultiBufferSource buffers, int light,
                          ResourceLocation texture, float[][] parts) {
        VertexConsumer output = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        for (float[] part : parts) {
            stack.pushPose();
            if (part[6] != 0f) {
                stack.translate(part[7] / 16f, part[8] / 16f, part[9] / 16f);
                stack.mulPose(Axis.YP.rotationDegrees(part[6]));
                stack.translate(-part[7] / 16f, -part[8] / 16f, -part[9] / 16f);
            }
            cube(stack, output, part, light, 255, 255, 255);
            stack.popPose();
        }
    }

    /** A neutral grayscale UV atlas is tinted independently for hull and floor. */
    static void drawPainted(PoseStack stack, MultiBufferSource buffers, int light,
                            ResourceLocation original, ResourceLocation neutral,
                            float[][] parts, byte[] channels, int[] paints) {
        for (int index = 0; index < parts.length; index++) {
            float[] part = parts[index];
            int category = channels[index];
            int selected = category >= 1 && category <= paints.length ? paints[category - 1] : -1;
            VertexConsumer output = buffers.getBuffer(RenderType.entityCutoutNoCull(
                selected < 0 ? original : neutral));
            int red = 255, green = 255, blue = 255;
            if (selected >= 0) {
                if (category == 6) {
                    // Marine antifouling: original teal plus four curated colors.
                    int[] rgb = {
                        0x14585D, 0x15345C, 0x314238, 0x522A32, 0x292D35
                    };
                    int packed = rgb[Math.max(0, Math.min(selected, rgb.length - 1))];
                    red = (packed >> 16) & 255;
                    green = (packed >> 8) & 255;
                    blue = packed & 255;
                } else {
                    float[] diffuse = DyeColor.byId(selected).getTextureDiffuseColors();
                    red = Math.round(diffuse[0] * 255f);
                    green = Math.round(diffuse[1] * 255f);
                    blue = Math.round(diffuse[2] * 255f);
                }
            }
            stack.pushPose();
            if (part[6] != 0f) {
                stack.translate(part[7] / 16f, part[8] / 16f, part[9] / 16f);
                stack.mulPose(Axis.YP.rotationDegrees(part[6]));
                stack.translate(-part[7] / 16f, -part[8] / 16f, -part[9] / 16f);
            }
            cube(stack, output, part, light, red, green, blue);
            stack.popPose();
        }
    }

    private static void cube(PoseStack stack, VertexConsumer output, float[] part, int light,
                             int red, int green, int blue) {
        float x0 = part[0] / 16f, y0 = part[1] / 16f, z0 = part[2] / 16f;
        float x1 = part[3] / 16f, y1 = part[4] / 16f, z1 = part[5] / 16f;
        float[][] points = {
            {x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0},
            {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}
        };
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        for (int face = 0; face < 6; face++) {
            int offset = 10 + face * 4;
            float u0 = part[offset] / 128f, v0 = part[offset + 1] / 128f;
            float u1 = part[offset + 2] / 128f, v1 = part[offset + 3] / 128f;
            for (int index = 0; index < 4; index++) {
                float[] point = points[CORNERS[face][index]];
                float u = index <= 1 ? u0 : u1;
                float v = index == 0 || index == 3 ? v1 : v0;
                output.vertex(pose, point[0], point[1], point[2])
                    .color(red, green, blue, 255).uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(normal, NORMALS[face][0], NORMALS[face][1], NORMALS[face][2])
                    .endVertex();
            }
        }
    }
}
