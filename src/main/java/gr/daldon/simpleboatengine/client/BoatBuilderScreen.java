package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.common.BoatBuilderMenu;
import gr.daldon.simpleboatengine.common.MotorPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;

public final class BoatBuilderScreen extends AbstractContainerScreen<BoatBuilderMenu> {
    public BoatBuilderScreen(BoatBuilderMenu menu,Inventory inv,Component title) {
        super(menu,inv,title);
        imageWidth=192;imageHeight=237;
        titleLabelX=0;titleLabelY=-99;inventoryLabelX=0;inventoryLabelY=-99;
    }
    @Override protected void init(){
        super.init();
        addRenderableWidget(Button.builder(Component.literal("◀"),b->change(-1))
            .bounds(leftPos+12,topPos+13,22,20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"),b->change(1))
            .bounds(leftPos+160,topPos+13,22,20).build());
        addRenderableWidget(Button.builder(Component.literal("BOATS"),b->tab(true))
            .bounds(leftPos+17,topPos+120,75,20).build());
        addRenderableWidget(Button.builder(Component.literal("ATTACHMENTS"),b->tab(false))
            .bounds(leftPos+95,topPos+120,87,20).build());
    }
    private void change(int delta) {
        MotorPackets.CHANNEL.sendToServer(
            new MotorPackets.BuilderAction(menu.position(),delta,0));
    }
    private void tab(boolean boats){
        MotorPackets.CHANNEL.sendToServer(new MotorPackets.BuilderAction(menu.position(),0,boats?2:3));
    }
    private void panel(GuiGraphics g,int x,int y,int w,int h) {
        g.fill(x,y,x+w,y+h,0xFF493322);
        g.fill(x+2,y+2,x+w-2,y+h-2,0xFFBE9A68);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        panel(g,leftPos,topPos,imageWidth,imageHeight);
        for(int y=9;y<imageHeight;y+=17)
            g.fill(leftPos+4,topPos+y,leftPos+imageWidth-4,topPos+y+1,0x40563820);
        panel(g,leftPos+37,topPos+10,119,27);
        g.fill(leftPos+86,topPos+42,leftPos+104,topPos+61,0xFF543B24);
        g.fill(leftPos+87,topPos+43,leftPos+103,topPos+59,0xFFE4D0AD);
        for(int i=0;i<12;i++){
            int x=leftPos+27+(i%6)*23,y=topPos+82+(i/6)*25;
            g.fill(x-2,y-2,x+18,y+18,0xFF705039);
            g.fill(x-1,y-1,x+17,y+17,0xFFD3B58A);
            if(i<menu.recipe().cost().size() && menu.input(i).isEmpty()){
                var need=new ArrayList<>(menu.recipe().cost().entrySet()).get(i);
                g.renderFakeItem(new ItemStack(need.getKey()),x,y);
                g.fill(x,y,x+16,y+16,0xAAB49E80);
            }
        }
        for(int i=0;i<36;i++){
            int x=leftPos+17+(i%9)*18,y=topPos+(i<27?153+i/9*18:211);
            g.fill(x-1,y-1,x+17,y+17,0xFF654630);
            g.fill(x,y,x+16,y+16,0xFFD2B48C);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        g.drawString(font,"BOAT BUILDER",56,3,0xFFFFE8BF,false);
        g.drawString(font,menu.recipe().result().getHoverName(),48,22,0xFF372719,false);
        g.drawString(font,"NEEDED MATERIALS",20,68,0xFF372719,false);
        var need=new ArrayList<>(menu.recipe().cost().entrySet());
        for(int i=0;i<need.size() && i<12;i++){
            int x=27+(i%6)*23,y=82+(i/6)*25;
            g.drawString(font,String.valueOf(need.get(i).getValue()),x+9,y+10,0xFFFFFFFF,false);
        }

    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        renderBackground(g);super.render(g,mx,my,partial);renderTooltip(g,mx,my);
    }
}
