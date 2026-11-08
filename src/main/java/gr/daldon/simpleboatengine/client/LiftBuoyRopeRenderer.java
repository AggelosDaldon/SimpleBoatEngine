package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gr.daldon.simpleboatengine.common.LiftBuoyBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class LiftBuoyRopeRenderer implements BlockEntityRenderer<LiftBuoyBlockEntity> {
    public LiftBuoyRopeRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(LiftBuoyBlockEntity buoy, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        BlockPos target=buoy.ropeTarget();
        if (target==null) return;
        float y2=buoy.isRaised() ? -0.5f : target.getY()-buoy.getBlockPos().getY()+0.5f;
        if (y2>-0.22f) return;
        VertexConsumer output=buffers.getBuffer(RenderType.lines());
        Matrix4f position=pose.last().pose();
        Matrix3f normal=pose.last().normal();
        // Two offset dark strands keep the cord visible through water.
        strand(output,position,normal,.49f,.21f,.49f,.49f,y2,.49f);
        strand(output,position,normal,.51f,.21f,.51f,.51f,y2,.51f);
    }
    private static void strand(VertexConsumer out, Matrix4f matrix, Matrix3f normal,
                               float x1,float y1,float z1,float x2,float y2,float z2) {
        out.vertex(matrix,x1,y1,z1).color(99,74,48,255).normal(normal,0,-1,0).endVertex();
        out.vertex(matrix,x2,y2,z2).color(99,74,48,255).normal(normal,0,-1,0).endVertex();
    }
    @Override public boolean shouldRenderOffScreen(LiftBuoyBlockEntity buoy) { return true; }
}
