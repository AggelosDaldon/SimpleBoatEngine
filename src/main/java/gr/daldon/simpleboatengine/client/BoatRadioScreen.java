package gr.daldon.simpleboatengine.client;

import gr.daldon.simpleboatengine.common.BoatRadioMenu;
import gr.daldon.simpleboatengine.common.BoatRadioTrackNames;
import gr.daldon.simpleboatengine.common.MotorPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Warm wooden 9-disc radio with track list and hand-rendered shadowless GUI. */
public final class BoatRadioScreen extends AbstractContainerScreen<BoatRadioMenu> {
    private int selected=0;
    public BoatRadioScreen(BoatRadioMenu menu,Inventory player,Component title){
        super(menu,player,title);
        imageWidth=350;imageHeight=260;
        titleLabelX=0;titleLabelY=-999;inventoryLabelX=0;inventoryLabelY=-999;
    }
    private void panel(GuiGraphics g,int x,int y,int w,int h,int color){
        g.fill(x,y,x+w,y+h,0xFF251C18);
        g.fill(x+2,y+2,x+w-2,y+h-2,color);
    }
    private void button(GuiGraphics g,String label,int x,int y,int w,int h,
                        boolean selected){
        panel(g,x,y,w,h,selected?0xFFAC8D60:0xFF6C5340);
        int tx=x+(w-font.width(label))/2;
        g.drawString(font,label,tx,y+(h-8)/2,0xFFEFE0BC,false);
    }
    @Override protected void renderBg(GuiGraphics g,float tick,int mouseX,int mouseY){
        int x=leftPos,y=topPos;
        panel(g,x,y,350,260,0xFF866345);
        for(int i=8;i<258;i+=18)
            g.fill(x+3,y+i,x+347,y+i+1,0x4F36291F);
        panel(g,x+12,y+12,112,104,0xFFB5A88E);
        for(int i=0;i<9;i++){
            int sx=x+24+(i%3)*22,sy=y+26+(i/3)*23;
            panel(g,sx-1,sy-1,18,18,0xFFDDCA9F);
        }
        panel(g,x+136,y+12,200,104,0xFFD4C5A5);
        for(int i=0;i<9;i++){
            int ty=y+20+i*10;
            if(i==selected)g.fill(x+141,ty-1,x+328,ty+10,0xFFB4C9C4);
            if(i+1==menu.current())g.fill(x+141,ty,x+145,ty+8,0xFF70A070);
        }
        button(g,"START",x+14,y+124,96,26,selected==menu.current()-1);
        button(g,"STOP",x+127,y+124,96,26,false);
        button(g,"NEXT",x+240,y+124,96,26,false);
        panel(g,x+59,y+161,230,95,0xFFC5AA83);
        for(int i=0;i<36;i++){
            int sx=x+68+(i%9)*18,sy=y+(i<27?167+(i/9)*18:225);
            g.fill(sx-1,sy-1,sx+17,sy+17,0xFF594332);
            g.fill(sx,sy,sx+16,sy+16,0xFFD5B996);
        }
    }
    private void text(GuiGraphics g,String value,int x,int y,int color){
        g.drawString(font,value,x,y,color,false);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        text(g,"RECORDS",27,15,0xFF30251B);
        text(g,"PLAYLIST",154,14,0xFF30251B);
        for(int i=0;i<9;i++){
            String name=menu.disc(i).isEmpty()?"-- Empty --":
                BoatRadioTrackNames.track(menu.disc(i));
            if(name.length()>28)name=name.substring(0,25)+"...";
            text(g,(i+1)+". "+name,149,20+i*10,
                i+1==menu.current()?0xFF1F5233:0xFF3C2C25);
        }
        // The inventory grid is self-explanatory; no overlapping label.
    }
    private void action(int type){
        MotorPackets.CHANNEL.sendToServer(
            new MotorPackets.RadioControl(menu.boatId(),type,selected));
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button==0){
            int x=(int)mx-leftPos,y=(int)my-topPos;
            if(x>=140&&x<332&&y>=20&&y<110){
                selected=Math.min(8,(y-20)/10);return true;
            }
            if(y>=124&&y<150){
                if(x>=14&&x<110){action(0);return true;}
                if(x>=127&&x<223){action(1);return true;}
                if(x>=240&&x<336){action(2);return true;}
            }
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);super.render(g,mx,my,partial);renderTooltip(g,mx,my);
    }
}
