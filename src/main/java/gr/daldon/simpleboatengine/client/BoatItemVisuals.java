package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.BoatPaint;
import gr.daldon.simpleboatengine.common.SkiffBoatItem;
import gr.daldon.simpleboatengine.common.MotorData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.DyeColor;
import net.minecraft.network.chat.Component;

/** Shared visual state for a packed boat in the world, workshop and inventory. */
public final class BoatItemVisuals {
    private BoatItemVisuals() {}

    public static CompoundTag engine(ItemStack stack) {
        CompoundTag root=stack.getTag();
        if(root==null || !root.contains("PackedFishingBoat",10)) return new CompoundTag();
        CompoundTag saved=root.getCompound("PackedFishingBoat");
        CompoundTag forge=saved.getCompound("ForgeData");
        // Forge persists entity.getPersistentData() in ForgeData.
        return forge.getCompound("SimpleBoatEngine");
    }

    public static void drawBoat(ItemStack item, PoseStack pose,
                                MultiBufferSource buffers, int light) {
        drawBoat(item,pose,buffers,light,0f);
    }

    public static void drawBoat(ItemStack item, PoseStack pose,
                                MultiBufferSource buffers, int light, float attachmentYOffset) {
        if(!(item.getItem() instanceof SkiffBoatItem boat)) return;
        BlueBoatMesh.draw(pose,buffers,light,
            BoatPaint.woodStyle(item,boat.getHullStyle()),BoatPaint.all(item));
        drawAccessories(item,pose,buffers,light,attachmentYOffset);
        drawName(item,pose,buffers,light);
    }

    public static void drawAccessories(ItemStack item, PoseStack pose,
                                       MultiBufferSource buffers, int light) {
        drawAccessories(item,pose,buffers,light,0f);
    }
    public static void drawAccessories(ItemStack item, PoseStack pose,
                                       MultiBufferSource buffers, int light, float offset) {
        CompoundTag engine=engine(item);
        if(engine.getBoolean("Installed")) {
            pose.pushPose();
            pose.translate(0,.56+offset,2.76);
            pose.scale(1.5f,1.5f,1.5f);
            MotorMesh.drawFishing(pose,buffers,0f,light);
            pose.popPose();

            if(engine.getBoolean("CanisterInstalled")) {
                pose.pushPose();
                pose.translate(.59,.10+4.95/16.0+offset,.70);
                pose.mulPose(Axis.YP.rotationDegrees(-90));
                pose.scale(1.3f,1.3f,1.3f);
                CanisterMesh.draw(pose,buffers,light);
                pose.popPose();
            }
            if(engine.getBoolean("PoleLightInstalled")) {
                pose.pushPose();
                pose.translate(-.935,.10+14.25/16.0+offset,2.09);
                pose.scale(1.3f,1.3f,1.3f);
                PoleLightMesh.draw(pose,buffers,false,light);
                pose.popPose();
            }
        }
    }
    /** Align the painted name to the two angled bow panels, like SkiffBoatRenderer. */
    public static void drawName(ItemStack item, PoseStack pose, MultiBufferSource buffers, int light) {
        String name = BoatPaint.name(item);
        if (name.isBlank()) return;
        Font font = Minecraft.getInstance().font;
        int color = DyeColor.byId(BoatPaint.nameColor(item)).getTextColor();
        int textLight = BoatPaint.nameGlows(item) ? LightTexture.FULL_BRIGHT : light;
        for (int side : new int[]{-1,1}) {
            pose.pushPose();
            pose.translate(side*5.55/16.0,10.5/16.0,-33.0/16.0);
            pose.mulPose(Axis.YP.rotationDegrees(side*52.7836f));
            pose.translate(side*1.12/16.0,0,0);
            pose.mulPose(Axis.YP.rotationDegrees(side*90f));
            float size = Math.min(.010f,.73f/Math.max(1,font.width(name)));
            pose.scale(size,-size,size);
            font.drawInBatch(Component.literal(name),-font.width(name)/2f,
                -font.lineHeight/2f,color,false,pose.last().pose(),
                buffers,Font.DisplayMode.NORMAL,0,textLight);
            pose.popPose();
        }
    }

}
