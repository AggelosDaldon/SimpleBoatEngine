package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.BoatCustomizerBlockEntity;
import gr.daldon.simpleboatengine.common.BoatPaint;
import gr.daldon.simpleboatengine.common.SkiffBoatItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Shows the actual customized boat physically resting on the workstation. */
public final class BoatCustomizerRenderer implements BlockEntityRenderer<BoatCustomizerBlockEntity> {
    public BoatCustomizerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BoatCustomizerBlockEntity workshop, float partial,
                       PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var item = workshop.getItem(0);
        if (!(item.getItem() instanceof SkiffBoatItem boat)) return;
        pose.pushPose();
        pose.translate(0.5, 1.08, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(90f));
        pose.scale(0.30f, 0.30f, 0.30f);
        BlueBoatMesh.draw(pose, buffers, light, boat.getHullStyle(), BoatPaint.all(item));
        pose.popPose();
    }
}
