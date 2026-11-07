package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Pixel-perfect renderer for the user-authored Blockbench outboard motor. */
public final class MotorMesh {
    private static final ResourceLocation ATLAS = new ResourceLocation(SimpleBoatEngine.ID,
        "textures/item/outboard_engine.png");
    private static final int[][] CORNERS = {
        {0, 3, 2, 1}, {5, 6, 7, 4}, {4, 7, 3, 0},
        {1, 2, 6, 5}, {3, 7, 6, 2}, {4, 0, 1, 5}
    };
    private static final float[][] NORMALS = {
        {0, 0, -1}, {0, 0, 1}, {-1, 0, 0},
        {1, 0, 0}, {0, 1, 0}, {0, -1, 0}
    };
    private MotorMesh() {}

    /** Reuse the canister cuboids with Minecraft iron-block surfaces. */
    public static void drawSteelCanister(PoseStack stack, MultiBufferSource source, int light) {
        drawAtlas(stack, source, new ResourceLocation("minecraft",
            "textures/block/iron_block.png"), CanisterMeshData.PARTS, 0, 0f, light);
    }

    public static void draw(PoseStack stack, MultiBufferSource source, float spin, int light) {
        drawAtlas(stack, source, ATLAS, MotorMeshData.PARTS, 256, spin, light);
    }

    static void drawAtlas(PoseStack stack, MultiBufferSource source, ResourceLocation atlas,
                          float[][] pieces, int atlasSize, float spin, int light) {
        VertexConsumer vertices = source.getBuffer(RenderType.entityCutoutNoCull(atlas));
        for (float[] piece : pieces) {
            stack.pushPose();
            if (piece[10] > 0f) {
                // The propeller group's pivot as authored in Blockbench.
                rotateAroundZ(stack, 8f, -4f, 6f, spin);
            }
            if (piece[6] != 0f) {
                // Preserve each blade's resting 120/240 degree orientation.
                rotateAroundZ(stack, piece[7], piece[8], piece[9],
                    (float) Math.toRadians(piece[6]));
            }
            cuboid(stack, vertices, piece, atlasSize, light);
            stack.popPose();
        }
    }

    private static void rotateAroundZ(PoseStack stack, float x, float y, float z, float radians) {
        stack.translate((x - 8f) / 16f, y / 16f, (z - 8f) / 16f);
        stack.mulPose(Axis.ZP.rotation(radians));
        stack.translate(-(x - 8f) / 16f, -y / 16f, -(z - 8f) / 16f);
    }

    private static void cuboid(PoseStack stack, VertexConsumer vertices, float[] b,
                               int atlasSize, int light) {
        float x0 = (b[0] - 8f) / 16f, y0 = b[1] / 16f, z0 = (b[2] - 8f) / 16f;
        float x1 = (b[3] - 8f) / 16f, y1 = b[4] / 16f, z1 = (b[5] - 8f) / 16f;
        float[][] p = {
            {x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0},
            {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}
        };
        Matrix4f position = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        for (int face = 0; face < CORNERS.length; face++) {
            int uvOffset = 11 + face * 4;
            float u0 = atlasSize == 0 ? 0f : b[uvOffset] / atlasSize;
            float v0 = atlasSize == 0 ? 0f : b[uvOffset + 1] / atlasSize;
            float u1 = atlasSize == 0 ? 1f : b[uvOffset + 2] / atlasSize;
            float v1 = atlasSize == 0 ? 1f : b[uvOffset + 3] / atlasSize;
            for (int vertex = 0; vertex < 4; vertex++) {
                float[] point = p[CORNERS[face][vertex]];
                float u = vertex <= 1 ? u0 : u1;
                float v = vertex == 0 || vertex == 3 ? v1 : v0;
                vertices.vertex(position, point[0], point[1], point[2])
                    .color(255, 255, 255, 255).uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(normal, NORMALS[face][0], NORMALS[face][1], NORMALS[face][2])
                    .endVertex();
            }
        }
    }
}
