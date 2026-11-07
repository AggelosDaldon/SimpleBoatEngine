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
import net.minecraft.world.item.DyeColor;
import net.minecraft.client.renderer.LightTexture;

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
            // Match the two angled name plates near the v2 prow.
            String name = blue.getBoatName();
            int rgb = DyeColor.byId(blue.getNameColor()).getTextColor();
            int textLight = blue.nameGlows() ? LightTexture.FULL_BRIGHT : light;
            for (int side : new int[]{-1, 1}) {
                // Named Blockbench panels:
                // origin (-5.55,9.275,-33) yaw -52.7836 (port)
                // origin (+5.55,9.275,-33) yaw +52.7836 (starboard).
                // Rotate around the authored pivot, then step to the outer face.
                pose.pushPose();
                pose.translate(side * 5.55 / 16.0, 9.275 / 16.0, -33.0 / 16.0);
                pose.mulPose(Axis.YP.rotationDegrees(side * 52.7836f));
                pose.translate(side * 1.12 / 16.0, 0, 0);
                pose.mulPose(Axis.YP.rotationDegrees(side * 90f));
                float size = Math.min(0.010f, 0.73f / Math.max(1, font.width(name)));
                pose.scale(size, -size, size);
                font.drawInBatch(Component.literal(name), -font.width(name) / 2f,
                    -font.lineHeight / 2f, rgb, false, pose.last().pose(),
                    buffers, Font.DisplayMode.NORMAL, 0, textLight);
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
