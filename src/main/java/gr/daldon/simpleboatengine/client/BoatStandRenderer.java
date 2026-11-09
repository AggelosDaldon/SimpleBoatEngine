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
        pose.pushPose();
        pose.translate(.5,0,.5);
        if(stand.getBlockState().hasProperty(gr.daldon.simpleboatengine.common.BoatStandBlock.FACING))
            pose.mulPose(Axis.YP.rotationDegrees(
                stand.getBlockState().getValue(gr.daldon.simpleboatengine.common.BoatStandBlock.FACING)
                .toYRot()-180f));
        pose.translate(-.5,0,-.5);
        // Longitudinal keel rails with four upright posts contacting hull sides.
        // Cross-ties terminate between rails; no co-planar surfaces at joints.
        beam(pose,buffers,light,Blocks.STRIPPED_SPRUCE_LOG,.06,.055,-3.80,.18,.22,3.96);
        beam(pose,buffers,light,Blocks.STRIPPED_SPRUCE_LOG,.76,.055,-3.80,.18,.22,3.96);
        for(double z : new double[]{-3.12,-1.08}) {
            beam(pose,buffers,light,Blocks.DARK_OAK_PLANKS,.245,.055,z,.51,.145,.16);
            // Two side posts per station. Their capped ends support the hull.
            beam(pose,buffers,light,Blocks.STRIPPED_OAK_LOG,.095,.275,z+.01,.17,.23,.17);
            beam(pose,buffers,light,Blocks.STRIPPED_OAK_LOG,.735,.275,z+.01,.17,.23,.17);
            beam(pose,buffers,light,Blocks.SPRUCE_SLAB,.085,.505,z,.19,.055,.19);
            beam(pose,buffers,light,Blocks.SPRUCE_SLAB,.725,.505,z,.19,.055,.19);
        }
        for(double z : new double[]{-3.10,-.82}) {
            beam(pose,buffers,light,Blocks.STRIPPED_SPRUCE_LOG,.06,-.24,z,.18,.295,.18);
            beam(pose,buffers,light,Blocks.STRIPPED_SPRUCE_LOG,.76,-.24,z,.18,.295,.18);
        }
        var item=stand.getItem(0);
        if(!(item.getItem() instanceof SkiffBoatItem)) {pose.popPose();return;}
        pose.pushPose();
        pose.translate(.5,.41,-1.45);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.scale(.94f,.94f,.94f);
        BoatItemVisuals.drawBoat(item,pose,buffers,light,-.14f);
        pose.popPose();
        pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(BoatStandBlockEntity stand){return true;}
    @Override public int getViewDistance(){return 96;}
}
