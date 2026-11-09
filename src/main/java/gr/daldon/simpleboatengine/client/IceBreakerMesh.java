package gr.daldon.simpleboatengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import gr.daldon.simpleboatengine.SimpleBoatEngine;

public final class IceBreakerMesh {
    private IceBreakerMesh(){}
    private static final ResourceLocation V2_TEXTURE = new ResourceLocation(
        SimpleBoatEngine.ID, "textures/entity/icebreaker_v2.png");
    private static void beam(PoseStack p,MultiBufferSource b,int light,Block block,
            double x,double y,double z,double w,double h,double d){
        p.pushPose();p.translate(x,y,z);p.scale((float)w,(float)h,(float)d);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            block.defaultBlockState(),p,b,light,OverlayTexture.NO_OVERLAY);
        p.popPose();
    }
    public static void draw(PoseStack p,MultiBufferSource b,int light,boolean fishing){
        if (fishing) {
            // Exact 27-element V2 assembly, including precise blade rotations,
            // mounting plates, 64px UV atlas and extended bow cutting point.
            SkiffMesh.drawParts(p,b,light,V2_TEXTURE,IceBreakerModelData.PARTS);
            return;
        }
        // Compact fitted fallback for normal vanilla boats.
        double bow=-1.04;
        // V-shaped hardened-steel ice blades ahead of the bow.
        beam(p,b,light,Blocks.IRON_BLOCK,-.10,.17,bow-.26,.20,.28,.78);
        for(int side=-1;side<=1;side+=2){
            double x=side<0?-.63:.43;
            beam(p,b,light,Blocks.POLISHED_ANDESITE,x,.11,bow,.20,.31,.75);
            beam(p,b,light,Blocks.IRON_BLOCK,x,.18,bow-.28,.20,.20,.45);
        }
        beam(p,b,light,Blocks.IRON_BARS,-.58,.32,bow+.28,1.16,.28,.15);
    }
}
