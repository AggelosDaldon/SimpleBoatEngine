package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
        pose.translate(0.5, 0.5, 0.5);
        MotorMesh.draw(pose, buffers, 0f, packedLight);
        pose.popPose();
    }
}
