package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.BoatMounts;
import gr.daldon.simpleboatengine.common.BlueFishingBoat;
import gr.daldon.simpleboatengine.common.SkiffBoat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Renders the experimental user-authored hull instead of a vanilla boat model. */
public final class SkiffBoatRenderer extends EntityRenderer<SkiffBoat> {
    private final Font font;
    public SkiffBoatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.font = context.getFont();
        this.shadowRadius = 0.85f;
    }

    @Override
    public void render(SkiffBoat boat, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        // The original renderer was placing the entire deck below the waterline.
        // Both authored hulls use the same 4.95px floor height.
        pose.translate(0f, BoatMounts.HULL_Y, 0f);
        if (boat instanceof BlueFishingBoat blue)
            BlueBoatMesh.draw(pose, buffers, light,
                blue.getHullStyle(), blue.getPaints());
        else SkiffMesh.draw(pose, buffers, light);
        if (boat instanceof BlueFishingBoat blue && !blue.getBoatName().isBlank()) {
            String label = blue.getBoatName();
            // Keep a readable decal on the top of the closed bow deck as well as
            // both exterior sides. The old x=.82 decals were buried in the hull.
            pose.pushPose();
            pose.translate(0f, 13.78f / 16f + 0.018f, -1.65f);
            pose.mulPose(Axis.XP.rotationDegrees(90f));
            pose.scale(0.012f, -0.012f, 0.012f);
            font.drawInBatch(Component.literal(label), -font.width(label) / 2f, 0f,
                0xFFF5E1, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL,
                0, light);
            pose.popPose();
            for (int side : new int[]{-1, 1}) {
                pose.pushPose();
                pose.translate(side * 1.09, 0.52, -1.20);
                pose.mulPose(Axis.YP.rotationDegrees(side < 0 ? -90f : 90f));
                pose.scale(0.012f, -0.012f, 0.012f);
                font.drawInBatch(Component.literal(label), -font.width(label) / 2f, 0f,
                    0xFFF5E1, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL,
                    0, light);
                pose.popPose();
            }
        }
        pose.popPose();
        super.render(boat, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SkiffBoat boat) {
        return boat instanceof BlueFishingBoat blue
            ? BlueBoatMesh.texture(blue.getHullStyle()) : SkiffMesh.TEXTURE;
    }
}
