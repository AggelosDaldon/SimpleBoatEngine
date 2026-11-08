package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.BoatStandBlockEntity;
import gr.daldon.simpleboatengine.common.BoatPaint;
import gr.daldon.simpleboatengine.common.SkiffBoatItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Full-size dry-dock skiff plus wooden keel supports extending ahead of the
 * central paint-workbench block. The actual inventory remains in the BE.
 */
public final class BoatStandRenderer implements BlockEntityRenderer<BoatStandBlockEntity> {
    public BoatStandRenderer(BlockEntityRendererProvider.Context context) {}

    private static void beam(PoseStack p, MultiBufferSource b, int light, Block material,
                             double x,double y,double z,double width,double height,double depth) {
        p.pushPose();
        p.translate(x,y,z);
        p.scale((float)width,(float)height,(float)depth);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            material.defaultBlockState(),p,b,light,OverlayTexture.NO_OVERLAY);
        p.popPose();
    }

    @Override public void render(BoatStandBlockEntity stand, float tick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        // Two elongated keel beams / cross braces at the correct full-size length.
        beam(pose,buffers,light,Blocks.STRIPPED_SPRUCE_LOG,.06,.11,-3.8,.20,.24,4.0);
        beam(pose,buffers,light,Blocks.STRIPPED_SPRUCE_LOG,.76,.11,-3.8,.20,.24,4.0);
        beam(pose,buffers,light,Blocks.DARK_OAK_PLANKS,.03,.06,-2.9,.94,.17,.20);
        beam(pose,buffers,light,Blocks.DARK_OAK_PLANKS,.03,.06,-1.4,.94,.17,.20);
        beam(pose,buffers,light,Blocks.DARK_OAK_PLANKS,.03,.06,-.15,.94,.17,.20);
        for(double z : new double[]{-2.8,-.45}) {
            beam(pose,buffers,light,Blocks.STRIPPED_SPRUCE_LOG,.03,-.25,z,.20,.42,.20);
            beam(pose,buffers,light,Blocks.STRIPPED_SPRUCE_LOG,.77,-.25,z,.20,.42,.20);
        }
        var item=stand.getItem(0);
        if(!(item.getItem() instanceof SkiffBoatItem)) return;
        pose.pushPose();
        pose.translate(.5,.52,-1.45);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.scale(.94f,.94f,.94f);
        BoatItemVisuals.drawBoat(item,pose,buffers,light);
        pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(BoatStandBlockEntity stand){return true;}
    @Override public int getViewDistance(){return 96;}
}
