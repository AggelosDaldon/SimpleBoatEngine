package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.BoatCustomizerBlockEntity;
import gr.daldon.simpleboatengine.common.BoatPaint;
import gr.daldon.simpleboatengine.common.SkiffBoatItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;

/** Bright boat preview with a legible, camera-facing name above the workbench. */
public final class BoatCustomizerRenderer implements BlockEntityRenderer<BoatCustomizerBlockEntity> {
    private final Font font;

    public BoatCustomizerRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(BoatCustomizerBlockEntity workshop, float partial,
                       PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var item = workshop.getItem(0);
        if (!(item.getItem() instanceof SkiffBoatItem boat)) return;

        // Render with full-bright vertex lighting so pigments stay readable at
        // night and indoors. This affects only the workstation preview.
        pose.pushPose();
        pose.translate(0.5, 1.10, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(90f));
        pose.scale(0.35f, 0.35f, 0.35f);
        BlueBoatMesh.draw(pose, buffers, LightTexture.FULL_BRIGHT,
            boat.getHullStyle(), BoatPaint.all(item));
        pose.popPose();

    }
}
