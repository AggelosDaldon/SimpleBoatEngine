package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Paint ingredients and attachment inputs are dedicated temporary slots.
 * The edited boat remains bound to this player's selected stand.
 */
public final class BoatCustomizerMenu extends AbstractContainerMenu {
    private final BoatCustomizerBlockEntity table;
    private final BlockPos blockPos;
    private final List<BlockPos> stands;
    private final DataSlot selected=DataSlot.standalone();
    private final SimpleContainer materials=new SimpleContainer(16);
    public static final int MATERIAL_SLOTS=16;
    public static final int ATTACHMENT_SLOTS=4;
    private final SimpleContainer attachmentInputs=new SimpleContainer(ATTACHMENT_SLOTS);
    private final Player editor;
    private boolean loadingAttachments;
    private int lastSyncedMask=-1;

    public BoatCustomizerMenu(int id, Inventory inv, FriendlyByteBuf packet) {
        this(id,inv,packet.readBlockPos());
    }
    private BoatCustomizerMenu(int id,Inventory inv,BlockPos pos) {
        this(id,inv,inv.player.level().getBlockEntity(pos)
            instanceof BoatCustomizerBlockEntity be ? be : null,pos);
    }
    public BoatCustomizerMenu(int id,Inventory inv,BoatCustomizerBlockEntity station) {
        this(id,inv,station,station.getBlockPos());
    }
    private BoatCustomizerMenu(int id,Inventory inv,BoatCustomizerBlockEntity station,BlockPos pos) {
        super(EngineMenus.CUSTOMIZER.get(),id);
        table=station;blockPos=pos;editor=inv.player;
        stands=station==null?List.of():List.copyOf(station.nearbyStands());
        selected.set(0);
        addDataSlot(selected);
        // Only 12 explicit material slots + the player's own inventory.
        for(int i=0;i<MATERIAL_SLOTS;i++)
            addSlot(new Slot(materials,i,17+(i%8)*25,227+(i/8)*24){
                @Override public int getMaxStackSize(){return 64;}
            });
        for(int i=0;i<ATTACHMENT_SLOTS;i++){
            final int index=i;
            addSlot(new Slot(attachmentInputs,i,249+i*48,246){
                @Override public boolean mayPlace(ItemStack item){
                    return item.is(attachmentType(index));
                }
                @Override public int getMaxStackSize(){return 1;}
            });
        }
        for(int row=0;row<3;row++)
            for(int col=0;col<9;col++)
                addSlot(new Slot(inv,col+row*9+9,21+col*20,279+row*19));
        for(int col=0;col<9;col++)
            addSlot(new Slot(inv,col,21+col*20,337));
        loadAttachmentSlots();
    }
    public BlockPos getBlockPos(){return blockPos;}
    public int standCount(){return stands.size();}
    public int standIndex(){return stands.isEmpty()?-1:Math.floorMod(selected.get(),stands.size());}
    public BlockPos selectedStandPos(){return stands.isEmpty()?null:stands.get(standIndex());}
    public BoatStandBlockEntity selectedStand(){
        BlockPos target=selectedStandPos();
        return target!=null && table!=null && table.getLevel()!=null
            && table.getLevel().getBlockEntity(target) instanceof BoatStandBlockEntity stand
                ? stand : null;
    }
    public void selectRelative(int delta){
        if(!stands.isEmpty()){
            selected.set(Math.floorMod(standIndex()+Integer.signum(delta),stands.size()));
            loadAttachmentSlots();
            broadcastChanges();
        }
    }
    public ItemStack boatItem(){
        BoatStandBlockEntity stand=selectedStand();
        return stand==null?ItemStack.EMPTY:stand.getItem(0);
    }
    public BoatCustomizerBlockEntity table(){return table;}
    // Compatibility with older customization consumers; no visible GUI slots.
    public ItemStack dyeItem(){return ItemStack.EMPTY;}
    public ItemStack woodItem(){return ItemStack.EMPTY;}
    public ItemStack nameDyeItem(){return ItemStack.EMPTY;}
    public static Item attachmentType(int i){
        return switch(i){
            case 0 -> EngineItems.ENGINE.get();
            case 1 -> EngineItems.POLE_LIGHT.get();
            case 2 -> EngineItems.FUEL_CANISTER.get();
            default -> EngineItems.ICE_BREAKER.get();
        };
    }

    /**
     * Equipment slots are actual item slots.  Removing an installed item gives
     * it back to the cursor; depositing one installs it immediately. No Apply
     * action, no duplicated virtual copies, and no on/off toggles.
     */
    private void loadAttachmentSlots(){
        if(editor.level().isClientSide)return;
        loadingAttachments=true;
        try{
            ItemStack boat=boatItem();
            int mask=BoatPaintSessions.attachmentMask(boat);
            for(int i=0;i<ATTACHMENT_SLOTS;i++){
                ItemStack equipped=(mask&(1<<i))!=0
                    ? new ItemStack(attachmentType(i)):ItemStack.EMPTY;
                if(i==3 && !equipped.isEmpty() &&
                    BoatPaintSessions.motor(boat).getBoolean("IceBreakerSilk"))
                    equipped.enchant(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,1);
                attachmentInputs.setItem(i,equipped);
            }
            lastSyncedMask=mask;
        }finally{loadingAttachments=false;}
    }
    private void saveAttachmentSlots(){
        if(loadingAttachments||editor.level().isClientSide)return;
        BoatStandBlockEntity stand=selectedStand();
        if(stand==null||stand.getItem(0).isEmpty()||stand.hasPreview())return;
        int next=0;
        for(int i=0;i<ATTACHMENT_SLOTS;i++)
            if(!attachmentInputs.getItem(i).isEmpty())next|=1<<i;
        if(next==lastSyncedMask)return;
        ItemStack boat=stand.getItem(0).copy();
        var motor=BoatPaintSessions.writableMotor(boat);
        motor.putBoolean("Installed",(next&1)!=0);
        motor.putBoolean("PoleLightInstalled",(next&2)!=0);
        motor.putBoolean("CanisterInstalled",(next&4)!=0);
        motor.putBoolean("IceBreakerInstalled",(next&8)!=0);
        if((next&1)==0){
            motor.putBoolean("Running",false);motor.putInt("Throttle",0);
        }
        if((next&8)!=0)
            motor.putBoolean("IceBreakerSilk",
                net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getItemEnchantmentLevel(
                        net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,
                        attachmentInputs.getItem(3))>0);
        stand.setItem(0,boat);
        lastSyncedMask=next;
        broadcastChanges();
    }
    @Override public void slotsChanged(net.minecraft.world.Container source){
        if(source==attachmentInputs)saveAttachmentSlots();
        super.slotsChanged(source);
    }
    public ItemStack attachmentItem(int i){return attachmentInputs.getItem(i);}
    public boolean hasAttachmentInputs(int oldMask,int newMask){return oldMask==newMask;}
    public void consumeAttachmentInputs(int oldMask,int newMask){}
    public int materialSlotCount(){return MATERIAL_SLOTS;}
    public ItemStack materialItem(int i){return materials.getItem(i);}
    public boolean hasMaterials(Map<Item,Integer> cost){
        if(cost.size()>MATERIAL_SLOTS)return false;
        int index=0;
        for(var entry:cost.entrySet()){
            ItemStack stack=materials.getItem(index++);
            if(!stack.is(entry.getKey()) || stack.getCount()<entry.getValue())
                return false;
        }
        return true;
    }
    public void consumeMaterials(Map<Item,Integer> cost){
        int index=0;
        for(var entry:cost.entrySet()) {
            materials.removeItem(index++,entry.getValue());
        }
        broadcastChanges();
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack original=slot.getItem(),copy=original.copy();
        if(index<MATERIAL_SLOTS+ATTACHMENT_SLOTS){
            if(!moveItemStackTo(original,MATERIAL_SLOTS+ATTACHMENT_SLOTS,slots.size(),true))
                return ItemStack.EMPTY;
        } else {
            boolean moved=false;
            for(int i=0;i<ATTACHMENT_SLOTS && !moved;i++)
                if(original.is(attachmentType(i)))
                    moved=moveItemStackTo(original,MATERIAL_SLOTS+i,MATERIAL_SLOTS+i+1,false);
            if(!moved)moved=moveItemStackTo(original,0,MATERIAL_SLOTS,false);
            if(!moved)return ItemStack.EMPTY;
        }
        if(original.isEmpty())slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
    @Override public void removed(Player player){
        super.removed(player);
        // Installed equipment stays on the boat. Only return unused paint.
        for(int i=0;i<MATERIAL_SLOTS;i++){
            ItemStack stack=materials.removeItemNoUpdate(i);
            if(stack.isEmpty())continue;
            if(!player.getInventory().add(stack))player.drop(stack,false);
        }
    }
    @Override public boolean stillValid(Player p){
        return table!=null && table.stillValid(p);
    }
}
