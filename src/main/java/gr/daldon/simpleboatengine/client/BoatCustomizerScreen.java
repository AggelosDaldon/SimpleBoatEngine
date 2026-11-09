package gr.daldon.simpleboatengine.client;

import com.mojang.math.Axis;
import gr.daldon.simpleboatengine.common.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Compact marine paint editor: exact requested panels, 12 deposited-material
 * slots, selectable attachment icons and no extra unnecessary controls.
 */
public final class BoatCustomizerScreen extends AbstractContainerScreen<BoatCustomizerMenu> {
    private static final String[] PARTS={"Hull","Floor","Stripe","Hatch & trim",
        "Seats","Antifouling","Inner walls","Storage 2"};
    private static final String[] ATTACHMENTS={"Engine","Pole light","Reserve tank","Ice breaker"};
    private final int[] draft=new int[BoatPaint.CHANNELS];
    private int part,nameDye,mask,lastStand=-1;
    private float angle=32f,pitch=-38f;
    private EditBox name;
    private Button applyButton;
    private float uiScale=0.82f;
    private float shiftX,shiftY;

    public BoatCustomizerScreen(BoatCustomizerMenu menu,Inventory inv,Component title){
        super(menu,inv,title);
        imageWidth=460;imageHeight=365;
        titleLabelX=0;titleLabelY=-999;
        inventoryLabelX=0;inventoryLabelY=-999;
    }
    private void resetDraft(){
        ItemStack source=menu.boatItem();
        ItemStack original=menu.selectedStand()==null?source:menu.selectedStand().previewOriginal();
        int[] colors=BoatPaint.all(source);
        System.arraycopy(colors,0,draft,0,draft.length);
        nameDye=BoatPaint.nameColor(source);
        mask=BoatPaintSessions.attachmentMask(source);
        if(name!=null)name.setValue(BoatPaint.name(source));
    }
    private Button button(String text,int x,int y,int w,int h,Runnable action){
        Button result=Button.builder(Component.literal(text),b->action.run())
            .bounds(leftPos+x,topPos+y,w,h).build();
        addRenderableWidget(result);return result;
    }
    @Override protected void init(){
        super.init();
        uiScale=Math.min(.82f,Math.min((width-10f)/imageWidth,(height-10f)/imageHeight));
        uiScale=Math.max(.25f,uiScale);
        shiftX=(width-imageWidth*uiScale)/2-leftPos*uiScale;
        shiftY=(height-imageHeight*uiScale)/2-topPos*uiScale;
        resetDraft();lastStand=menu.standIndex();
        button("<",17,19,24,23,()->navigate(-1));
        button(">",199,19,24,23,()->navigate(1));
        button("<",17,56,24,23,()->part=(part+PARTS.length-1)%PARTS.length);
        button(">",199,56,24,23,()->part=(part+1)%PARTS.length);
        applyButton=button("APPLY",14,181,67,25,()->send(BoatPaintSessions.APPLY));
        button("PREVIEW",84,181,70,25,()->send(BoatPaintSessions.PREVIEW));
        button("CANCEL",157,181,69,25,()->send(BoatPaintSessions.CANCEL));
        button("INVENTORY",265,319,156,24,this::inventory);
        name=new EditBox(font,leftPos+245,topPos+192,143,20,Component.literal("Name"));
        name.setMaxLength(20);name.setHint(Component.literal("Boat name"));
        name.setValue(BoatPaint.name(menu.boatItem()));
        addRenderableWidget(name);
    }
    private void navigate(int d){
        MotorPackets.CHANNEL.sendToServer(new MotorPackets.SelectPaintStand(menu.getBlockPos(),d));
    }
    private void send(int action){
        if(menu.selectedStandPos()==null)return;
        MotorPackets.CHANNEL.sendToServer(new MotorPackets.BoatPaintDraft(menu.getBlockPos(),
            menu.selectedStandPos(),draft.clone(),name==null?"":name.getValue(),
            nameDye,mask,action));
        // IMPORTANT: server closes container after processing the packet.
        // Closing client-side first used to lose Cancel on laggy servers.
    }
    private void inventory(){
        if(menu.selectedStandPos()!=null)
            MotorPackets.CHANNEL.sendToServer(new MotorPackets.OpenParkedCargo(
                menu.getBlockPos(),menu.selectedStandPos()));
    }
    private List<Map.Entry<Item,Integer>> required(){
        ItemStack original=menu.selectedStand()==null?ItemStack.EMPTY:
            menu.selectedStand().previewOriginal();
        return new ArrayList<>(BoatPaintSessions.requirements(original,draft,nameDye,mask).entrySet());
    }
    private boolean valid(){
        if(menu.standCount()==0 || menu.boatItem().isEmpty())return false;
        if((mask&(BoatPaintSessions.POLE|BoatPaintSessions.CANISTER))!=0
            && (mask&BoatPaintSessions.ENGINE)==0)return false;
        if(minecraft!=null && minecraft.player!=null
            && minecraft.player.getAbilities().instabuild)return true;
        return menu.hasMaterials(BoatPaintSessions.requirements(
            menu.selectedStand().previewOriginal(),draft,nameDye,mask));
    }
    @Override protected void containerTick(){
        super.containerTick();
        if(lastStand!=menu.standIndex()){lastStand=menu.standIndex();resetDraft();}
        if(applyButton!=null)applyButton.active=valid();
    }
    private static void panel(GuiGraphics g,int x,int y,int w,int h,int border,int fill){
        g.fill(x,y,x+w,y+h,border);g.fill(x+2,y+2,x+w-2,y+h-2,fill);
    }
    private int color(int index){
        float[] c=DyeColor.byId(index).getTextureDiffuseColors();
        return 0xFF000000|(Math.round(c[0]*255)<<16)
            |(Math.round(c[1]*255)<<8)|Math.round(c[2]*255);
    }
    private ItemStack stagedItem(){
        ItemStack item=menu.boatItem().copy();
        for(int i=0;i<draft.length;i++)if(draft[i]>=0)BoatPaint.set(item,i,draft[i]);
        if(name!=null)BoatPaint.setName(item,name.getValue());
        BoatPaint.setNameColor(item,nameDye);
        var motor=BoatPaintSessions.writableMotor(item);
        motor.putBoolean("Installed",(mask&BoatPaintSessions.ENGINE)!=0);
        motor.putBoolean("PoleLightInstalled",(mask&BoatPaintSessions.POLE)!=0);
        motor.putBoolean("CanisterInstalled",(mask&BoatPaintSessions.CANISTER)!=0);
        motor.putBoolean("IceBreakerInstalled",(mask&BoatPaintSessions.BREAKER)!=0);
        return item;
    }
    private void preview(GuiGraphics g){
        ItemStack boat=stagedItem();
        if(!(boat.getItem() instanceof SkiffBoatItem))return;
        g.flush();
        g.pose().pushPose();
        g.pose().translate(leftPos+342,topPos+100,330);
        g.pose().mulPose(Axis.XP.rotationDegrees(pitch));
        g.pose().mulPose(Axis.YP.rotationDegrees(angle));
        g.pose().scale(25f,-25f,25f);
        BoatItemVisuals.drawBoat(boat,g.pose(),g.bufferSource(),LightTexture.FULL_BRIGHT,-.11f);
        g.flush();g.pose().popPose();
    }
    private static ItemStack equipment(int index){
        return new ItemStack(switch(index){
            case 0 -> EngineItems.ENGINE.get();
            case 1 -> EngineItems.POLE_LIGHT.get();
            case 2 -> EngineItems.FUEL_CANISTER.get();
            default -> EngineItems.ICE_BREAKER.get();
        });
    }
    @Override protected void renderBg(GuiGraphics g,float tick,int mx,int my){
        int x=leftPos,y=topPos;
        panel(g,x,y,460,365,0xFF382A21,0xFFB49D80);
        panel(g,x+12,y+12,215,36,0xFF40332A,0xFFD4C2A5);
        panel(g,x+12,y+51,215,36,0xFF40332A,0xFFD4C2A5);
        panel(g,x+12,y+90,215,82,0xFF40332A,0xFFAD967C);
        panel(g,x+234,y+12,214,160,0xFF40332A,0xFF685D4E);
        panel(g,x+234,y+178,214,43,0xFF40332A,0xFFD4C2A5);
        panel(g,x+12,y+213,215,58,0xFF40332A,0xFFD4C2A5);
        panel(g,x+234,y+224,214,67,0xFF40332A,0xFFD4C2A5);
        panel(g,x+12,y+273,215,84,0xFF40332A,0xFF99836B);
        panel(g,x+234,y+297,214,56,0xFF40332A,0xFFD4C2A5);
        for(int i=0;i<16;i++){
            int bx=x+17+(i%8)*26,by=y+112+(i/8)*26;
            panel(g,bx,by,23,23,draft[part]==i?0xFFFFFFFF:0xFF41352B,color(i));
        }
        panel(g,x+402,y+188,38,25,0xFF41352B,color(nameDye));
        // Material requirements are translucent ghosts BEHIND real inventory slots.
        List<Map.Entry<Item,Integer>> req=required();
        for(int i=0;i<BoatCustomizerMenu.MATERIAL_SLOTS;i++){
            int sx=x+17+(i%8)*25,sy=y+227+(i/8)*24;
            panel(g,sx-1,sy-1,18,18,0xFF685746,0xFFB2A38B);
            if(i<req.size() && menu.materialItem(i).isEmpty()){
                g.renderFakeItem(new ItemStack(req.get(i).getKey()),sx,sy);
                g.fill(sx,sy,sx+16,sy+16,0xAAB7A790);
            }
            if(i<req.size() && req.get(i).getValue()>1)
                g.drawString(font,String.valueOf(req.get(i).getValue()),sx+9,sy+9,0xFFECE9DF,true);
        }
        // Four interactive equipment sockets, not copies of real items.
        for(int i=0;i<4;i++){
            int sx=x+249+i*48,sy=y+246;
            boolean active=(mask&(1<<i))!=0;
            panel(g,sx-2,sy-2,22,22,active?0xFF36764B:0xFF76695C,0xFFC0B29B);
            g.renderFakeItem(equipment(i),sx,sy);
            if(!active)g.fill(sx,sy,sx+16,sy+16,0x998A8275);
        }
        // Player inventory cells: visible for inserting items into required slots.
        for(int i=0;i<36;i++){
            int sx=x+22+(i%9)*18,sy=y+(i<27?279+(i/9)*18:337);
            panel(g,sx-1,sy-1,18,18,0xFF514031,0xFFC7B69D);
        }
        preview(g);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        int ink=0xFF30251B;
        g.drawCenteredString(font,"STAND BOAT "+
            (menu.standCount()==0?"0/0":(menu.standIndex()+1)+"/"+menu.standCount()),121,24,ink);
        g.drawCenteredString(font,"PART: "+PARTS[part],121,63,ink);
        g.drawCenteredString(font,"16 DYES",121,95,0xFFF4E1B7);
        g.drawCenteredString(font,"PREVIEW - DRAG",342,20,0xFFFFE8C6);
        g.drawString(font,"NAME",242,181,ink);
        g.drawString(font,"DYE",406,181,ink);
        g.drawString(font,"MATERIALS TO APPLY",19,216,ink);
        g.drawString(font,"ATTACHMENTS - CLICK TO REMOVE",240,229,ink);
        for(int i=0;i<4;i++)
            g.drawCenteredString(font,new String[]{"ENGINE","LIGHT","TANK","ICE"}[i],
                257+i*48,269,ink);
        g.drawString(font,"YOUR INVENTORY",19,276,0xFFF8E7C9);
        g.drawString(font,valid()?"READY TO APPLY":"INSERT REQUIRED ITEMS",241,301,
            valid()?0xFF38754B:0xFF8E4A40);
    }
    private double logicalX(double x){return (x-shiftX)/uiScale;}
    private double logicalY(double y){return (y-shiftY)/uiScale;}
    @Override public boolean mouseClicked(double mx,double my,int button){
        double lx=logicalX(mx),ly=logicalY(my);
        int x=(int)lx-leftPos,y=(int)ly-topPos;
        if(button==0 && x>=17&&x<225&&y>=112&&y<161){
            int col=(x-17)/26,row=(y-112)/26;
            if(col<8 && row<2 && (x-17)%26<23 && (y-112)%26<23){
                draft[part]=row*8+col;
                if(part==BoatPaint.ANTIFOUL)draft[part]%=5;
                return true;
            }
        }
        if(button==0 && x>=402&&x<440&&y>=188&&y<213){
            nameDye=(nameDye+1)%16;return true;
        }
        if(button==0 && x>=247&&x<447&&y>=244&&y<266) {
            int index=(x-247)/48;
            if(index<4) {
                mask ^= 1<<index;
                if(index==0 && (mask&1)==0) mask &= ~(2|4);
                if((index==1||index==2)&&(mask&(1<<index))!=0)mask |= 1;
                return true;
            }
        }
        return super.mouseClicked(lx,ly,button);
    }
    @Override public boolean mouseReleased(double mx,double my,int b){
        return super.mouseReleased(logicalX(mx),logicalY(my),b);
    }
    @Override public boolean mouseDragged(double mx,double my,int b,double dx,double dy){
        double lx=logicalX(mx),ly=logicalY(my);
        if(b==0 && lx>=leftPos+234&&lx<=leftPos+448
           && ly>=topPos+12&&ly<=topPos+172){
            angle+=(float)(dx/uiScale)*1.5f;
            pitch-=(float)(dy/uiScale)*.7f;
            return true;
        }
        return super.mouseDragged(lx,ly,b,dx/uiScale,dy/uiScale);
    }
    @Override public boolean mouseScrolled(double mx,double my,double amount){
        return super.mouseScrolled(logicalX(mx),logicalY(my),amount);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float tick){
        renderBackground(g);
        g.pose().pushPose();
        g.pose().translate(shiftX,shiftY,0);
        g.pose().scale(uiScale,uiScale,1f);
        int lx=(int)logicalX(mx),ly=(int)logicalY(my);
        super.render(g,lx,ly,tick);
        renderTooltip(g,lx,ly);
        g.pose().popPose();
    }
}
