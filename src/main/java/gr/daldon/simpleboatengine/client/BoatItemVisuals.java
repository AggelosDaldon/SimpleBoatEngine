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
        if(!(item.getItem() instanceof SkiffBoatItem boat)) return;
        BlueBoatMesh.draw(pose,buffers,light,
            BoatPaint.woodStyle(item,boat.getHullStyle()),BoatPaint.all(item));
        drawAccessories(item,pose,buffers,light);
    }

    public static void drawAccessories(ItemStack item, PoseStack pose,
                                       MultiBufferSource buffers, int light) {
        CompoundTag engine=engine(item);
        if(engine.getBoolean("Installed")) {
            pose.pushPose();
            pose.translate(0,.56,2.76);
            pose.scale(1.5f,1.5f,1.5f);
            MotorMesh.drawFishing(pose,buffers,0f,light);
            pose.popPose();

            if(engine.getBoolean("CanisterInstalled")) {
                pose.pushPose();
                pose.translate(.59,.10+4.95/16.0,.70);
                pose.mulPose(Axis.YP.rotationDegrees(-90));
                pose.scale(1.3f,1.3f,1.3f);
                CanisterMesh.draw(pose,buffers,light);
                pose.popPose();
            }
            if(engine.getBoolean("PoleLightInstalled")) {
                pose.pushPose();
                pose.translate(-.935,.10+14.25/16.0,2.09);
                pose.scale(1.3f,1.3f,1.3f);
                PoleLightMesh.draw(pose,buffers,false,light);
                pose.popPose();
            }
        }
    }
}
