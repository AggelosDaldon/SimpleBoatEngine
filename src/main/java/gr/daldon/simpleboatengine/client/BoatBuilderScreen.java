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
import java.util.List;

/**
 * Shipwright catalogue GUI: Boats / Attachments / Buoys, 6x4 results,
 * item preview and description, ingredient slots with red missing
 * highlights and a server-authoritative output slot.
 */
public final class BoatBuilderScreen extends AbstractContainerScreen<BoatBuilderMenu> {
    private static final String[] CATEGORIES={"BOATS","ATTACHMENTS","BUOYS"};
    private static final int CATALOG_X=184,CATALOG_Y=30,CELL_W=42,CELL_H=37;
    private float uiScale, shiftX, shiftY;
    public BoatBuilderScreen(BoatBuilderMenu menu,Inventory inv,Component title){
        super(menu,inv,title);
        imageWidth=460;imageHeight=365;
        titleLabelX=0;titleLabelY=-999;inventoryLabelX=0;inventoryLabelY=-999;
    }
    @Override protected void init(){
        super.init();
        uiScale=Math.max(.3f,Math.min(.78f,Math.min((width-12f)/imageWidth,(height-12f)/imageHeight)));
        shiftX=(width-imageWidth*uiScale)/2-leftPos*uiScale;
        shiftY=(height-imageHeight*uiScale)/2-topPos*uiScale;
        addRenderableWidget(Button.builder(Component.literal("<"),b->category(-1))
            .bounds(leftPos+24,topPos+22,25,21).build());
        addRenderableWidget(Button.builder(Component.literal(">"),b->category(1))
            .bounds(leftPos+131,topPos+22,25,21).build());
    }
    private void category(int direction){
        MotorPackets.CHANNEL.sendToServer(
            new MotorPackets.BuilderAction(menu.position(),direction,0));
    }
    private void choose(int index){
        MotorPackets.CHANNEL.sendToServer(
            new MotorPackets.BuilderAction(menu.position(),index,4));
    }
    private static void panel(GuiGraphics g,int x,int y,int w,int h,int inside){
        g.fill(x,y,x+w,y+h,0xFF30231D);
        g.fill(x+2,y+2,x+w-2,y+h-2,inside);
    }
    private String details(){
        String id=menu.recipe().result().getItem().toString();
        if(id.contains("ice_breaker"))
            return "Breaks ice ahead of the hull. Can be enchanted with Silk Touch to store harvested ice.";
        if(id.contains("fuel_bucket"))
            return "Refillable steel canister. Use coal, charcoal, redstone or lava in the inventory.";
        if(id.contains("engine"))return "Powers the fishing boat. Install it in the Marine Workshop.";
        if(id.contains("light"))return "Adds a navigation light to your fishing boat.";
        if(id.contains("buoy"))return "A floating marker. Click to lift or lower the connected fishing trap.";
        if(id.contains("boat"))return "A customizable fishing boat with storage and room for attachments.";
        if(id.contains("canister"))return "An auxiliary fuel tank for longer voyages.";
        return "Craft this marine part with the required materials below.";
    }
    private void lineWrapped(GuiGraphics g,String value,int x,int y,int maxWidth,int col){
        List<net.minecraft.util.FormattedCharSequence> lines=font.split(Component.literal(value),maxWidth);
        for(int i=0;i<Math.min(lines.size(),5);i++)
            g.drawString(font,lines.get(i),x,y+i*11,col,false);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        int x=leftPos,y=topPos;
        panel(g,x,y,460,365,0xFF9F7852);
        for(int n=7;n<365;n+=19)
            g.fill(x+3,y+n,x+457,y+n+1,0x66533322);
        panel(g,x+13,y+14,155,175,0xFFD8BC91);
        panel(g,x+19,y+52,143,77,0xFFB89C77);
        panel(g,x+181,y+14,270,172,0xFFC5A17B);
        panel(g,x+185,y+191,202,68,0xFFDBC39A);
        panel(g,x+392,y+191,59,68,0xFFDBC39A);
        panel(g,x+129,y+267,202,94,0xFFBD956C);
        for(int i=0;i<24;i++){
            int bx=x+CATALOG_X+(i%6)*43,by=y+CATALOG_Y+(i/6)*37;
            boolean available=i<menu.categorySize();
            boolean selected=available && menu.selection()==menu.categoryStart(menu.category())+i;
            panel(g,bx,by,39,34,selected?0xFFB9D4B3:0xFFDECEB1);
            if(available){
                ItemStack item=menu.entry(i);
                g.renderFakeItem(item,bx+11,by+9);
            }
        }
        // Twelve material slots. Missing or insufficient inputs are red until
        // the exact item and count are deposited into the corresponding slot.
        var required=new ArrayList<>(menu.recipe().cost().entrySet());
        for(int i=0;i<12;i++){
            int bx=x+193+(i%6)*29,by=y+198+(i/6)*27;
            boolean needed=i<required.size();
            int supplied=0;
            if(needed)
                for(int slot=0;slot<12;slot++)
                    if(menu.input(slot).is(required.get(i).getKey()))
                        supplied+=menu.input(slot).getCount();
            boolean valid=needed && supplied>=required.get(i).getValue();
            panel(g,bx-1,by-1,19,19,needed&&!valid?0xFF9A3434:0xFF8E8068);
            if(needed && menu.input(i).isEmpty()){
                g.renderFakeItem(new ItemStack(required.get(i).getKey()),bx,by);
                g.fill(bx,by,bx+16,by+16,0x99C09A91);
            }
            // Stack counts of deposited inputs are rendered by Minecraft once.
            // Draw the recipe requirement only for vacant ghost slots.
            if(needed && menu.input(i).isEmpty() && required.get(i).getValue()>1){
                String count=String.valueOf(required.get(i).getValue());
                g.drawString(font,count,bx+16-font.width(count),by+9,
                    0xFFF3E7CB,false);
            }
        }
        panel(g,x+407,y+211,20,20,0xFFE7D3B5);
        for(int i=0;i<36;i++){
            int bx=x+142+(i%9)*18;
            int by=y+(i<27?276+(i/9)*18:334);
            panel(g,bx-1,by-1,18,18,0xFFDDC5A1);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        int ink=0xFF38261E;
        String label=CATEGORIES[menu.category()];
        g.drawString(font,label,90-font.width(label)/2,29,ink,false);
        g.drawString(font,"PREVIEW",65,55,ink,false);
        ItemStack selection=menu.recipe().result();
        g.renderFakeItem(selection,84,77);
        g.drawString(font,"INFO",24,135,0xFFFFD64A,false);
        lineWrapped(g,details(),24,147,135,0xFF654112);
        g.drawString(font,"AVAILABLE",275,19,0xFF382419,false);
        g.drawString(font,"MATERIALS",193,184,ink,false);
        g.drawString(font,"OUTPUT",399,184,ink,false);
        g.drawString(font,selection.getHoverName(),19,205,0xFFE9D0AA,false);
    }
    private double ux(double x){return (x-shiftX)/uiScale;}
    private double uy(double y){return (y-shiftY)/uiScale;}
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        mouseX=ux(mouseX);mouseY=uy(mouseY);
        if(button==0){
            int col=(int)(mouseX-leftPos-CATALOG_X)/43;
            int row=(int)(mouseY-topPos-CATALOG_Y)/37;
            double dx=mouseX-leftPos-CATALOG_X,dy=mouseY-topPos-CATALOG_Y;
            if(dx>=0&&dy>=0&&col>=0&&col<6&&row>=0&&row<4
                &&dx%43<39&&dy%37<34){
                int index=row*6+col;
                if(index<menu.categorySize()){choose(index);return true;}
            }
        }
        return super.mouseClicked(mouseX,mouseY,button);
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        return super.mouseReleased(ux(x),uy(y),button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        return super.mouseDragged(ux(x),uy(y),button,dx/uiScale,dy/uiScale);
    }
    @Override public boolean mouseScrolled(double x,double y,double amount){
        return super.mouseScrolled(ux(x),uy(y),amount);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float tick){
        renderBackground(g);
        g.pose().pushPose();
        g.pose().translate(shiftX,shiftY,0);
        g.pose().scale(uiScale,uiScale,1f);
        int x=(int)ux(mouseX),y=(int)uy(mouseY);
        super.render(g,x,y,tick);
        renderTooltip(g,x,y);
        g.pose().popPose();
    }
}
