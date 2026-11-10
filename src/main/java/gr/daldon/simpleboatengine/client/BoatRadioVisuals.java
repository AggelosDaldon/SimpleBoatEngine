package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.BoatMounts;
import gr.daldon.simpleboatengine.common.EngineBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Uses the exact uploaded wooden radio model on the right forward deck corner. */
public final class BoatRadioVisuals {
    private BoatRadioVisuals(){}
    public static void draw(PoseStack pose,MultiBufferSource buffers,int light,float offset){
        pose.pushPose();
        pose.translate(.68,BoatMounts.HULL_Y+4.95/16.0+offset,-1.65);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.scale(.80f,.80f,.80f);
        pose.translate(-.5,0,-.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            EngineBlocks.BOAT_RADIO_DECORATION.get().defaultBlockState(),
            pose,buffers,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
