package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/** Exact model of the supplied red canister with long filling spout.
 * This is the consumable fuel refill item, not the mounted reserve canister.
 */
public final class RefuelCanisterMesh {
    private static final ResourceLocation TEXTURE =
        new ResourceLocation(SimpleBoatEngine.ID,"textures/item/refuel_canister_custom.png");
    private RefuelCanisterMesh(){}
    public static void draw(PoseStack pose, MultiBufferSource buffers,int light) {
        MotorMesh.drawAtlas(pose,buffers,TEXTURE,RefuelCanisterMeshData.PARTS,64,0f,light);
    }
}
