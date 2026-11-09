package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.common.ParkedBoatCargoMenu;
import gr.daldon.simpleboatengine.common.MotorPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Inventory opens on the hatch, then cycles through main storage pages. */
public final class ParkedBoatCargoScreen extends AbstractContainerScreen<ParkedBoatCargoMenu> {
    public ParkedBoatCargoScreen(ParkedBoatCargoMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);
        imageWidth=192;imageHeight=222;
        titleLabelX=8;titleLabelY=6;
        inventoryLabelX=8;inventoryLabelY=127;
    }
    @Override protected void init(){
        super.init();
        addRenderableWidget(Button.builder(Component.literal("◀"), b->change(-1))
            .bounds(leftPos+170,topPos+26,18,19).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), b->change(1))
            .bounds(leftPos+170,topPos+100,18,19).build());
    }
    private void change(int step){
        menu.switchPage(step);
        MotorPackets.CHANNEL.sendToServer(new MotorPackets.CycleParkedCargo(menu.standPos(),step));
    }
    @Override protected void renderBg(GuiGraphics g,float tick,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF352A20);
        g.fill(leftPos+4,topPos+4,leftPos+imageWidth-4,topPos+imageHeight-4,0xFFAB977B);
        for(int i=0;i<54;i++){
            if(menu.page()==0 && i>=27)break;
            int x=leftPos+7+(i%9)*18,y=topPos+18+(i/9)*18;
            g.fill(x,y,x+18,y+18,0xFF34271C);
            g.fill(x+1,y+1,x+17,y+17,0xFFC7B49A);
        }
        for(int i=0;i<36;i++){
            int x=leftPos+7+(i%9)*18,
                y=topPos+(i<27?139+(i/9)*18:197);
            g.fill(x,y,x+18,y+18,0xFF34271C);
            g.fill(x+1,y+1,x+17,y+17,0xFFC7B49A);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,menu.page()==0?"Bow hatch 27/27":
            menu.page()==1?"Main storage 1-54":"Main storage 55-108",
            8,6,0xFFFFE9C3,false);
        g.drawString(font,playerInventoryTitle,8,127,0xFFFFE9C3,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float tick){
        renderBackground(g);super.render(g,mx,my,tick);renderTooltip(g,mx,my);
    }
}
