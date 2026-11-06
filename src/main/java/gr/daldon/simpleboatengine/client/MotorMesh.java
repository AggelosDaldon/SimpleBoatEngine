package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Original blocky low-poly outboard, made from new cuboids and pixel materials. */
public final class MotorMesh {
    private static final ResourceLocation GREEN = material("cowling");
    private static final ResourceLocation STEEL = material("steel");
    private static final ResourceLocation DARK = material("charcoal");
    private static final ResourceLocation LIGHT = material("aluminium");
    private static final ResourceLocation PROP = material("propeller");

    private static ResourceLocation material(String name) {
        return new ResourceLocation(SimpleBoatEngine.ID, "textures/item/" + name + ".png");
    }

    private record Box(float x0, float y0, float z0, float x1, float y1, float z1,
                       ResourceLocation material, boolean blade) {}

    private static final Box[] PIECES = {
        // Domed-looking cowling assembled from simple overlapping cuboids.
        new Box(4.3f, 7f, 4.5f, 11.7f, 12.7f, 13.1f, GREEN, false),
        new Box(4.9f, 12.7f, 5.1f, 11.1f, 14.0f, 12.5f, LIGHT, false),
        new Box(3.9f, 9.7f, 4.7f, 12.1f, 10.5f, 13.0f, STEEL, false),
        new Box(6.0f, 14.0f, 7.1f, 10f, 14.8f, 10.8f, DARK, false),
        // Rear mounting bracket and articulated tiller.
        new Box(6.7f, 5.2f, 4.9f, 9.3f, 8.0f, 7.4f, STEEL, false),
        new Box(0.0f, 8.4f, 9.0f, 5.0f, 9.2f, 10.2f, LIGHT, false),
        new Box(-1.1f, 8.1f, 8.7f, 1.2f, 9.5f, 10.4f, DARK, false),
        // Long lower unit and anti-ventilation plate.
        new Box(7.1f, -4.9f, 6.5f, 8.9f, 7.0f, 8.5f, STEEL, false),
        new Box(5.8f, -4.4f, 6.0f, 10.2f, -3.7f, 9.1f, LIGHT, false),
        new Box(6.9f, -7.2f, 6.4f, 9.1f, -4.5f, 8.6f, STEEL, false),
        new Box(7.3f, -5.9f, 5.4f, 8.7f, -4.6f, 7.5f, PROP, false),
        // Each independently animated blade has its own cuboid.
        new Box(4.5f, -5.7f, 5.4f, 11.5f, -5.05f, 6.5f, PROP, true),
        new Box(7.7f, -8.7f, 5.4f, 8.3f, -2.0f, 6.5f, PROP, true)
    };
    private MotorMesh() {}

    public static void draw(PoseStack stack, MultiBufferSource source, float spin, int light) {
        for (Box piece : PIECES) {
            VertexConsumer vertices = source.getBuffer(RenderType.entityCutoutNoCull(piece.material()));
            cuboid(stack, vertices, piece, spin, light);
        }
    }

    private static void cuboid(PoseStack pose, VertexConsumer output, Box b, float angle, int light) {
        float[][] p = {
            {b.x0, b.y0, b.z0}, {b.x1, b.y0, b.z0},
            {b.x1, b.y1, b.z0}, {b.x0, b.y1, b.z0},
            {b.x0, b.y0, b.z1}, {b.x1, b.y0, b.z1},
            {b.x1, b.y1, b.z1}, {b.x0, b.y1, b.z1}
        };
        int[][] corners = {{0,3,2,1}, {5,6,7,4}, {4,7,3,0},
                           {1,2,6,5}, {3,7,6,2}, {4,0,1,5}};
        float[][] normals = {{0,0,-1}, {0,0,1}, {-1,0,0},
                             {1,0,0}, {0,1,0}, {0,-1,0}};
        Matrix4f position = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        float turn = angle + (float)Math.PI; // 180-degree rest orientation.
        float sine = (float)Math.sin(turn), cosine = (float)Math.cos(turn);
        for (int f = 0; f < 6; f++) {
            for (int i = 0; i < 4; i++) {
                float[] xyz = p[corners[f][i]];
                float x = xyz[0], y = xyz[1], z = xyz[2];
                if (b.blade()) {
                    float dx = x - 8f, dy = y + 5.4f;
                    x = 8f + dx * cosine - dy * sine;
                    y = -5.4f + dx * sine + dy * cosine;
                }
                float u = (i == 0 || i == 1) ? 0f : 1f;
                float v = (i == 0 || i == 3) ? 1f : 0f;
                output.vertex(position, (x - 8f) / 16f, y / 16f, (z - 8f) / 16f)
                    .color(255,255,255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light).normal(normal, normals[f][0], normals[f][1], normals[f][2])
                    .endVertex();
            }
        }
    }
}
