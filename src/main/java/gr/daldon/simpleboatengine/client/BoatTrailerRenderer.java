package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.BoatMounts;
import gr.daldon.simpleboatengine.common.BoatTrailer;
import gr.daldon.simpleboatengine.common.MotorData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;

/** Textured steel two-wheel trailer carrying a real painted fishing-boat model. */
public final class BoatTrailerRenderer extends EntityRenderer<BoatTrailer> {
    private final Font font;
    public BoatTrailerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        font = ctx.getFont();
        shadowRadius = 1.1f;
    }

    private void part(PoseStack pose, MultiBufferSource buffers, int light, Block block,
                      float x0, float y0, float z0, float x1, float y1, float z1) {
        pose.pushPose();
        pose.translate(x0, y0, z0);
        pose.scale(x1 - x0, y1 - y0, z1 - z0);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            block.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public void render(BoatTrailer trailer, float yaw, float partial,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        // The new Blockbench trailer points its tow tongue along +Z,
        // unlike the fishing boat (whose bow faces -Z).
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.pushPose();
        // Wheels in the .bbmodel center around Y=-4, with a 3.3px radius.
        pose.translate(0, 0.46, 0);
        TrailerMesh.draw(pose, buffers, light, trailer.wheelAngle(partial));
        pose.popPose();

        CompoundTag tag = trailer.displayData();
        if (!tag.isEmpty()) {
            int style = tag.getInt("Style");
            int[] paint = tag.getIntArray("Paints");
            if (paint.length != 7) paint = new int[]{-1,-1,-1,-1,-1,-1,-1};
            pose.pushPose();
            pose.translate(0, .91f + BoatMounts.HULL_Y, 0);
            // Boat bow toward towing vehicle, engine hanging off trailer rear.
            pose.mulPose(Axis.YP.rotationDegrees(180f));
            BlueBoatMesh.draw(pose, buffers, light, style, paint);
            if (tag.getBoolean("Engine")) {
                pose.pushPose();
                var motor = BoatMounts.motorForFishingTrailer();
                pose.translate(motor.x(), motor.y(), motor.z());
                pose.scale(1.5f, 1.5f, 1.5f);
                MotorMesh.drawFishing(pose, buffers, 0f, light);
                pose.popPose();
            }
            if (tag.getBoolean("PoleLight")) {
                pose.pushPose();
                var lamp = BoatMounts.poleLightForFishingTrailer();
                pose.translate(lamp.x(), lamp.y(), lamp.z());
                pose.scale(1.3f, 1.3f, 1.3f);
                PoleLightMesh.draw(pose, buffers,
                    MotorData.isDark(trailer.level().getDayTime()), light);
                pose.popPose();
            }
            if (tag.getBoolean("FuelCanister")) {
                pose.pushPose();
                var canister = BoatMounts.canisterForFishingTrailer();
                pose.translate(canister.x(), canister.y(), canister.z());
                pose.mulPose(Axis.YP.rotationDegrees(-90));
                pose.scale(1.3f, 1.3f, 1.3f);
                CanisterMesh.draw(pose, buffers, light);
                pose.popPose();
            }
            String name = tag.getString("Name");
            if (!name.isBlank()) {
                for(int side : new int[]{-1,1}) {
                    pose.pushPose();
                    pose.translate(side*5.55/16.0,10.5/16.0,-33.0/16.0);
                    pose.mulPose(Axis.YP.rotationDegrees(side*52.7836f));
                    pose.translate(side*1.12/16.0,0,0);
                    pose.mulPose(Axis.YP.rotationDegrees(side*90f));
                    float scale = Math.min(.010f, .73f/Math.max(1,font.width(name)));
                    pose.scale(scale,-scale,scale);
                    int color = DyeColor.byId(tag.getInt("NameColor")).getTextColor();
                    font.drawInBatch(Component.literal(name), -font.width(name)/2f,
                        -font.lineHeight/2f,color,false,pose.last().pose(),buffers,
                        Font.DisplayMode.NORMAL,0,
                        tag.getBoolean("NameGlowing") ? LightTexture.FULL_BRIGHT:light);
                    pose.popPose();
                }
            }
            pose.popPose();
        }
        pose.popPose();
        super.render(trailer,yaw,partial,pose,buffers,light);
    }

    @Override public ResourceLocation getTextureLocation(BoatTrailer trailer) {
        return new ResourceLocation("minecraft", "textures/block/iron_block.png");
    }
}
