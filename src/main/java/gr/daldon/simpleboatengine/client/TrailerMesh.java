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

/** Render the exact user-authored trailer with 64px atlas and rotating X-axis wheels. */
public final class TrailerMesh {
    public static final ResourceLocation ATLAS = new ResourceLocation(
        SimpleBoatEngine.ID, "textures/entity/fishing_boat_trailer.png");
    private static final int[][] CORNERS = {
        {0,3,2,1},{5,6,7,4},{4,7,3,0},{1,2,6,5},{3,7,6,2},{4,0,1,5}
    };
    private static final float[][] NORMALS = {
        {0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}
    };
    private TrailerMesh() {}

    public static void draw(PoseStack pose, MultiBufferSource buffer, int light, float wheelAngle) {
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(ATLAS));
        for (float[] part : TrailerMeshData.PARTS) {
            pose.pushPose();
            if (part[12] > 0.5f) {
                // The two group pivots are (-12.35,-4,-7) and (+12.35,-4,-7).
                float x = part[9] < 0 ? -12.35f : 12.35f;
                rotate(pose, x, 3f, -7f, Axis.ZP, wheelAngle);
            }
            // Blockbench local cube rotations are around their own pivots.
            if (part[6] != 0) rotate(pose, part[9],part[10],part[11],Axis.XP,part[6]);
            if (part[7] != 0) rotate(pose, part[9],part[10],part[11],Axis.YP,part[7]);
            if (part[8] != 0) rotate(pose, part[9],part[10],part[11],Axis.ZP,part[8]);
            cube(pose,vertices,part,light);
            pose.popPose();
        }
    }

    private static void rotate(PoseStack pose,float x,float y,float z,Axis axis,float angle) {
        pose.translate(x/16f,y/16f,z/16f);
        pose.mulPose(axis.rotationDegrees(angle));
        pose.translate(-x/16f,-y/16f,-z/16f);
    }

    private static void cube(PoseStack stack,VertexConsumer out,float[] part,int light) {
        float x0=part[0]/16f,y0=part[1]/16f,z0=part[2]/16f;
        float x1=part[3]/16f,y1=part[4]/16f,z1=part[5]/16f;
        float[][] points={
            {x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},
            {x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}
        };
        Matrix4f matrix=stack.last().pose();
        Matrix3f normal=stack.last().normal();
        for (int face=0;face<6;face++) {
            int o=13+face*4;
            float u0=part[o]/64f,v0=part[o+1]/64f;
            float u1=part[o+2]/64f,v1=part[o+3]/64f;
            for (int i=0;i<4;i++) {
                float[] point=points[CORNERS[face][i]];
                float u=i<=1?u0:u1;
                float v=i==0||i==3?v1:v0;
                out.vertex(matrix,point[0],point[1],point[2])
                    .color(255,255,255,255).uv(u,v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(normal,NORMALS[face][0],NORMALS[face][1],NORMALS[face][2]).endVertex();
            }
        }
    }
}
