package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.EngineItems;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Reuses the exact textured world model in the hotbar, GUI and player's hands. */
public final class MotorItemRenderer extends BlockEntityWithoutLevelRenderer {
    public MotorItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(ItemStack item, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        pose.pushPose();
        // Item models render in a 0..1 local cube; our mesh is centered on 0.
        if (item.is(EngineItems.LARGE_SKIFF.get())
            || item.is(EngineItems.BLUE_FISHING_BOAT.get())) {
            pose.translate(0.5, 0.39, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-35f));
            pose.mulPose(Axis.XP.rotationDegrees(25f));
            pose.scale(0.19f, 0.19f, 0.19f);
            if (item.is(EngineItems.BLUE_FISHING_BOAT.get()))
                BlueBoatMesh.draw(pose, buffers, packedLight);
            else SkiffMesh.draw(pose, buffers, packedLight);
        } else if (item.is(EngineItems.FUEL_CANISTER.get())) {
            pose.translate(0.5, 0.36, 0.5);
            CanisterMesh.draw(pose, buffers, packedLight);
        } else if (item.is(EngineItems.POLE_LIGHT.get())) {
            pose.translate(0.5, 0.28, 0.5);
            PoleLightMesh.draw(pose, buffers, false, packedLight);
        } else {
            pose.translate(0.5, 0.5, 0.5);
            MotorMesh.draw(pose, buffers, 0f, packedLight);
        }
        pose.popPose();
    }
}
