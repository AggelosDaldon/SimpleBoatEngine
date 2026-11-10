package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.common.ParkedBoatCargoMenu;
import gr.daldon.simpleboatengine.common.MotorPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The parked boat’s three cargo pages and removable radio records. */
public final class ParkedBoatCargoScreen extends AbstractContainerScreen<ParkedBoatCargoMenu> {
    private static final String[] TABS = {"Bow Hatch", "Main Storage 1", "Main Storage 2", "Radio Inventory"};
    public ParkedBoatCargoScreen(ParkedBoatCargoMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);
        imageWidth=288;imageHeight=222;
        titleLabelX=8;titleLabelY=6;
        inventoryLabelX=8;inventoryLabelY=127;
    }
    @Override protected void init(){
        super.init();
        for (int i=0;i<TABS.length;i++) {
            int page=i;
            addRenderableWidget(Button.builder(Component.literal(TABS[i]), b->select(page))
                .bounds(leftPos+174,topPos+18+i*23,105,20).build());
        }
    }
    private void select(int page){
        menu.selectPage(page);
        MotorPackets.CHANNEL.sendToServer(new MotorPackets.SelectParkedCargo(menu.standPos(),page));
    }
    @Override protected void renderBg(GuiGraphics g,float tick,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF352A20);
        g.fill(leftPos+4,topPos+4,leftPos+imageWidth-4,topPos+imageHeight-4,0xFFAB977B);
        int cells = menu.page()==ParkedBoatCargoMenu.RADIO_PAGE ? (menu.hasRadio()?9:0)
            : menu.page()==0 ? 27 : 54;
        for(int i=0;i<cells;i++){
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
        g.drawString(font,TABS[menu.page()],8,6,0xFFFFE9C3,false);
        if(menu.page()==ParkedBoatCargoMenu.RADIO_PAGE && !menu.hasRadio())
            g.drawString(font,"No radio installed",8,22,0xFFFFE9C3,false);
        g.drawString(font,playerInventoryTitle,8,127,0xFFFFE9C3,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float tick){
        renderBackground(g);super.render(g,mx,my,tick);renderTooltip(g,mx,my);
    }
}
