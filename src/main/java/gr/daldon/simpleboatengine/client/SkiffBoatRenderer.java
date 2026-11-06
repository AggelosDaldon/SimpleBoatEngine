package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.SkiffBoat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Renders the experimental user-authored hull instead of a vanilla boat model. */
public final class SkiffBoatRenderer extends EntityRenderer<SkiffBoat> {
    public SkiffBoatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.85f;
    }

    @Override
    public void render(SkiffBoat boat, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        // Hull keel goes below the waterline, with both gunwales above it.
        pose.translate(0f, -0.35f, 0f);
        SkiffMesh.draw(pose, buffers, light);
        pose.popPose();
        super.render(boat, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SkiffBoat boat) {
        return SkiffMesh.TEXTURE;
    }
}
