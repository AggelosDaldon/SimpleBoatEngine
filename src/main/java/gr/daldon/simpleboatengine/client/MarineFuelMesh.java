package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Dedicated blue-steel marine refuelling jug, distinct from the red tank. */
public final class MarineFuelMesh {
    private MarineFuelMesh(){}
    private static void part(PoseStack p, MultiBufferSource b, int light, Block block,
                             float x,float y,float z,float w,float h,float d){
        p.pushPose();p.translate(x,y,z);p.scale(w,h,d);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            block.defaultBlockState(),p,b,light,OverlayTexture.NO_OVERLAY);
        p.popPose();
    }
    public static void draw(PoseStack p, MultiBufferSource b,int light){
        // Shaded dark frame, blue vessel, metal nozzle and hollow handle.
        part(p,b,light,Blocks.DEEPSLATE_TILES,.19,.07,.22,.62,.13,.59);
        part(p,b,light,Blocks.CYAN_TERRACOTTA,.16,.18,.18,.68,.55,.65);
        part(p,b,light,Blocks.BLUE_TERRACOTTA,.17,.20,.16,.12,.49,.70);
        part(p,b,light,Blocks.POLISHED_DEEPSLATE,.14,.70,.19,.72,.08,.66);
        part(p,b,light,Blocks.IRON_BLOCK,.56,.77,.26,.18,.17,.20);
        part(p,b,light,Blocks.IRON_BLOCK,.57,.89,.25,.27,.08,.18);
        part(p,b,light,Blocks.IRON_BLOCK,.16,.74,.62,.10,.20,.15);
        part(p,b,light,Blocks.IRON_BLOCK,.41,.74,.62,.10,.20,.15);
        part(p,b,light,Blocks.IRON_BLOCK,.16,.87,.62,.35,.09,.15);
        part(p,b,light,Blocks.YELLOW_CONCRETE,.28,.38,.16,.41,.12,.04);
    }
}
