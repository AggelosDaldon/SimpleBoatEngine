package gr.daldon.simpleboatengine.client;

import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.BoatCustomizerMenu;
import gr.daldon.simpleboatengine.common.BoatPaint;
import gr.daldon.simpleboatengine.common.BoatPaintSessions;
import gr.daldon.simpleboatengine.common.MotorPackets;
import gr.daldon.simpleboatengine.common.SkiffBoatItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Exact-panel redesign: stand selector, part selector, 16 dyes, three actions,
 * materials, large live 3D preview, name, attachments and cargo entry. No slots.
 */
public final class BoatCustomizerScreen extends AbstractContainerScreen<BoatCustomizerMenu> {
    private static final String[] PARTS={"Hull","Floor","Stripe","Hatch lid","Seats",
        "Antifouling","Inner walls"};
    private static final String[] ATTACHMENTS={"Engine","Pole light","Red fuel canister","Ice breaker"};
    private final int[] draft=new int[BoatPaint.CHANNELS];
    private int part, nameDye, mask, lastStand=-1;
    private float angle=32f,pitch=-40f;
    private EditBox name;
    public BoatCustomizerScreen(BoatCustomizerMenu menu,Inventory inv,Component title){
        super(menu,inv,title);
        imageWidth=530;imageHeight=390;
        titleLabelX=0;titleLabelY=-999;
        inventoryLabelX=0;inventoryLabelY=-999;
    }
    private void resetDraft() {
        ItemStack boat=menu.boatItem();
        int[] colors=BoatPaint.all(boat);
        System.arraycopy(colors,0,draft,0,draft.length);
        nameDye=BoatPaint.nameColor(boat);
        mask=BoatPaintSessions.attachmentMask(boat);
        if(name!=null)name.setValue(BoatPaint.name(boat));
    }
    @Override protected void init(){
        super.init();
        resetDraft();lastStand=menu.standIndex();
        button("<",16,22,26,23,()->changeStand(-1));
        button(">",204,22,26,23,()->changeStand(1));
        button("<",16,74,26,23,()->part=(part+PARTS.length-1)%PARTS.length);
        button(">",204,74,26,23,()->part=(part+1)%PARTS.length);
        button("APPLY",15,202,70,25,()->send(BoatPaintSessions.APPLY,false));
        button("PREVIEW",91,202,74,25,()->send(BoatPaintSessions.PREVIEW,true));
        button("CANCEL",171,202,69,25,()->send(BoatPaintSessions.CANCEL,true));
        button("INVENTORY",359,348,158,25,()->inventory());
        name=new EditBox(font,leftPos+258,topPos+239,186,21,Component.literal("Boat name"));
        name.setMaxLength(20);name.setHint(Component.literal("Boat name"));
        name.setValue(BoatPaint.name(menu.boatItem()));
        addRenderableWidget(name);
    }
    private void button(String label,int x,int y,int w,int h,Runnable run){
        addRenderableWidget(Button.builder(Component.literal(label),b->run.run())
            .bounds(leftPos+x,topPos+y,w,h).build());
    }
    private void changeStand(int step){
        MotorPackets.CHANNEL.sendToServer(new MotorPackets.SelectPaintStand(menu.getBlockPos(),step));
    }
    private void send(int action,boolean close){
        if(menu.selectedStandPos()==null)return;
        MotorPackets.CHANNEL.sendToServer(new MotorPackets.BoatPaintDraft(
            menu.getBlockPos(),menu.selectedStandPos(),draft.clone(),
            name==null?"":name.getValue(),nameDye,mask,action));
        if(close && minecraft!=null && minecraft.player!=null)
            minecraft.player.closeContainer();
    }
    private void inventory(){
        if(menu.selectedStandPos()!=null)
            MotorPackets.CHANNEL.sendToServer(new MotorPackets.OpenParkedCargo(
                menu.getBlockPos(),menu.selectedStandPos()));
    }
    @Override protected void containerTick(){
        super.containerTick();
        if(lastStand!=menu.standIndex()){
            lastStand=menu.standIndex();resetDraft();
        }
    }
    private static void panel(GuiGraphics g,int x,int y,int w,int h,int edge,int fill){
        g.fill(x,y,x+w,y+h,edge);
        g.fill(x+2,y+2,x+w-2,y+h-2,fill);
    }
    private int color(int index){
        float[] c=DyeColor.byId(index).getTextureDiffuseColors();
        return 0xFF000000|(Math.round(c[0]*255)<<16)|
            (Math.round(c[1]*255)<<8)|Math.round(c[2]*255);
    }
    private ItemStack stagedItem(){
        ItemStack stack=menu.boatItem().copy();
        for(int i=0;i<draft.length;i++)if(draft[i]>=0)BoatPaint.set(stack,i,draft[i]);
        if(name!=null)BoatPaint.setName(stack,name.getValue());
        BoatPaint.setNameColor(stack,nameDye);
        if(stack.hasTag() && stack.getTag().contains("PackedFishingBoat",10)){
            var data=BoatPaintSessions.motor(stack);
            data.putBoolean("Installed",(mask&BoatPaintSessions.ENGINE)!=0);
            data.putBoolean("PoleLightInstalled",(mask&BoatPaintSessions.POLE)!=0);
            data.putBoolean("CanisterInstalled",(mask&BoatPaintSessions.CANISTER)!=0);
            data.putBoolean("IceBreakerInstalled",(mask&BoatPaintSessions.BREAKER)!=0);
        }
        return stack;
    }
    private void boatPreview(GuiGraphics g){
        ItemStack boat=stagedItem();
        if(!(boat.getItem() instanceof SkiffBoatItem))return;
        g.flush();
        g.enableScissor(leftPos+253,topPos+20,leftPos+516,topPos+220);
        g.pose().pushPose();
        g.pose().translate(leftPos+386,topPos+126,350);
        g.pose().mulPose(Axis.XP.rotationDegrees(pitch));
        g.pose().mulPose(Axis.YP.rotationDegrees(angle));
        g.pose().scale(34f,-34f,34f);
        BoatItemVisuals.drawBoat(boat,g.pose(),g.bufferSource(),LightTexture.FULL_BRIGHT,-.11f);
        g.flush();g.pose().popPose();g.disableScissor();
    }
    private List<String> materials(){
        List<String> items=new ArrayList<>();
        ItemStack original=menu.selectedStand()==null?ItemStack.EMPTY:
            menu.selectedStand().previewOriginal();
        for(int i=0;i<draft.length;i++)
            if(draft[i]>=0 && draft[i]!=BoatPaint.get(original,i))
                items.add("1x "+(i==BoatPaint.ANTIFOUL?"Black dye":
                    DyeColor.byId(draft[i]).getName()+" dye")+" - "+PARTS[i]);
        if(nameDye!=BoatPaint.nameColor(original))
            items.add("1x "+DyeColor.byId(nameDye).getName()+" dye - Name");
        int previous=BoatPaintSessions.attachmentMask(original);
        for(int i=0;i<4;i++)if((mask&(1<<i))!=0 && (previous&(1<<i))==0)
            items.add("1x "+ATTACHMENTS[i]);
        if(items.isEmpty())items.add("No materials required");
        return items;
    }
    @Override protected void renderBg(GuiGraphics g,float tick,int mx,int my){
        int x=leftPos,y=topPos;
        panel(g,x,y,530,390,0xFF33271E,0xFFBAA78C);
        panel(g,x+12,y+12,228,41,0xFF423025,0xFFDBCFB7);
        panel(g,x+12,y+64,228,41,0xFF423025,0xFFDBCFB7);
        panel(g,x+12,y+116,228,80,0xFF423025,0xFFB6A78F);
        panel(g,x+251,y+12,268,210,0xFF423025,0xFF746655);
        panel(g,x+251,y+228,268,42,0xFF423025,0xFFBDAA8F);
        panel(g,x+12,y+237,228,140,0xFF423025,0xFFDBCFB7);
        panel(g,x+251,y+280,268,60,0xFF423025,0xFFDBCFB7);
        for(int i=0;i<16;i++){
            int bx=x+19+(i%8)*27,by=y+127+(i/8)*31;
            panel(g,bx,by,24,25,draft[part]==i?0xFFFFFFFF:0xFF3C3025,color(i));
        }
        panel(g,x+451,y+235,61,29,0xFF493B2B,color(nameDye));
        boatPreview(g);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        int dark=0xFF30251B;
        g.drawCenteredString(font,"STAND BOAT  "+
            (menu.standCount()==0?"0/0":(menu.standIndex()+1)+"/"+menu.standCount()),
            123,30,dark);
        g.drawCenteredString(font,"PART TO PAINT: "+PARTS[part],123,82,dark);
        g.drawCenteredString(font,"ALL 16 DYES",125,119,0xFFECE0C7);
        g.drawCenteredString(font,"PREVIEW",385,23,0xFFFFE8C6);
        g.drawString(font,"NAME",258,228,dark);
        g.drawCenteredString(font,"NAME DYE",481,227,dark);
        g.drawString(font,"MATERIALS NEEDED TO APPLY",21,246,dark);
        List<String> materials=materials();
        for(int i=0;i<Math.min(materials.size(),8);i++)
            g.drawString(font,materials.get(i),21,264+i*13,0xFF534637);
        g.drawString(font,"ATTACHMENTS",261,284,dark);
        for(int i=0;i<4;i++){
            int xx=261+(i%2)*128,yy=302+(i/2)*16;
            g.drawString(font,((mask&(1<<i))!=0?"[x] ":"[ ] ")+ATTACHMENTS[i],
                xx,yy,dark);
        }
        g.drawCenteredString(font,"HATCH / MAIN STORAGE",438,379,dark);
    }
    @Override public boolean mouseClicked(double mx,double my,int btn){
        int x=(int)mx-leftPos,y=(int)my-topPos;
        if(btn==0 && x>=19 && x<235 && y>=127 && y<183){
            int col=(x-19)/27,row=(y-127)/31;
            if(col<8 && row<2 && (x-19)%27<24 && (y-127)%31<25) {
                draft[part]=row*8+col;
                if(part==BoatPaint.ANTIFOUL) draft[part]%=5;
                return true;
            }
        }
        if(btn==0 && x>=451 && x<512 && y>=235 && y<264){
            nameDye=(nameDye+1)%16;return true;
        }
        if(btn==0 && x>=261 && x<517 && y>=299 && y<335){
            int idx=((y-299)/17)*2+((x-261)/128);
            if(idx>=0 && idx<4)mask ^= (1<<idx);
            return true;
        }
        return super.mouseClicked(mx,my,btn);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
        if(button==0 && mx>=leftPos+251 && mx<=leftPos+519
            && my>=topPos+12 && my<=topPos+222){
            angle+=(float)dx*1.5f;pitch-=(float)dy*.7f;
            return true;
        }
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float tick){
        renderBackground(g);
        super.render(g,mx,my,tick);
        renderTooltip(g,mx,my);
    }
}
