package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
    public static final int ATTACHMENT_SLOTS=5;
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
        attachmentInputs.addListener(this::slotsChanged);
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
            addSlot(new Slot(attachmentInputs,i,249+i*39,246){
                @Override public boolean mayPlace(ItemStack item){
                    int kind=typeIndex(item);
                    if(kind<0)return false;
                    for(int other=0;other<ATTACHMENT_SLOTS;other++)
                        if(other!=index && attachmentInputs.getItem(other).is(item.getItem()))
                            return false;
                    return true;
                }
                @Override public int getMaxStackSize(){return 1;}
                @Override public void onTake(Player player,ItemStack taken){
                    if(taken.is(EngineItems.BOAT_RADIO.get())){
                        BoatStandBlockEntity stand=selectedStand();
                        if(stand!=null && !stand.getItem(0).isEmpty()){
                            ItemStack storedBoat=stand.getItem(0).copy();
                            BoatRadioStorage.detach(storedBoat,taken);
                            stand.setItem(0,storedBoat);
                        }
                    }
                    super.onTake(player,taken);
                }
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
            case 3 -> EngineItems.ICE_BREAKER.get();
            default -> EngineItems.BOAT_RADIO.get();
        };
    }

    /**
     * Equipment slots are actual item slots.  Removing an installed item gives
     * it back to the cursor; depositing one installs it immediately. No Apply
     * action, no duplicated virtual copies, and no on/off toggles.
     */
    private static int typeIndex(ItemStack item){
        if(item.is(EngineItems.ENGINE.get()))return 0;
        if(item.is(EngineItems.POLE_LIGHT.get()))return 1;
        if(item.is(EngineItems.FUEL_CANISTER.get()))return 2;
        if(item.is(EngineItems.ICE_BREAKER.get()))return 3;
        if(item.is(EngineItems.BOAT_RADIO.get()))return 4;
        return -1;
    }
    private void loadAttachmentSlots(){
        if(editor.level().isClientSide)return;
        loadingAttachments=true;
        try{
            ItemStack boat=boatItem();
            int mask=BoatPaintSessions.attachmentMask(boat);
            ListTag saved=boat.hasTag()
                ?boat.getTag().getList("BoatAttachmentSlots",Tag.TAG_COMPOUND):new ListTag();
            for(int i=0;i<ATTACHMENT_SLOTS;i++){
                ItemStack equipped=ItemStack.EMPTY;
                if(i<saved.size()) equipped=ItemStack.of(saved.getCompound(i));
                if(saved.isEmpty() && (mask&(1<<i))!=0){
                    equipped=new ItemStack(attachmentType(i));
                    if(i==3 && BoatPaintSessions.motor(boat).getBoolean("IceBreakerSilk"))
                        equipped.enchant(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,1);
                }
                attachmentInputs.setItem(i,equipped);
            }
            lastSyncedMask=mask;
        }finally{loadingAttachments=false;}
    }
    private void saveAttachmentSlots(){
        if(loadingAttachments || editor.level().isClientSide)return;
        BoatStandBlockEntity stand=selectedStand();
        if(stand==null || stand.getItem(0).isEmpty() || stand.hasPreview())return;
        int next=0;
        ListTag saved=new ListTag();
        for(int i=0;i<ATTACHMENT_SLOTS;i++){
            ItemStack item=attachmentInputs.getItem(i);
            int kind=typeIndex(item);
            if(kind>=0)next|=1<<kind;
            saved.add(item.save(new CompoundTag()));
        }
        ItemStack boat=stand.getItem(0).copy();
        int previous=BoatPaintSessions.attachmentMask(boat);
        if(next==previous && boat.hasTag() &&
            boat.getTag().getList("BoatAttachmentSlots",Tag.TAG_COMPOUND).equals(saved))return;
        // When a physical radio is inserted its own saved playlist goes onto
        // the selected boat; an empty radio starts with an empty playlist.
        if((next & BoatPaintSessions.RADIO)!=0 &&
           (previous & BoatPaintSessions.RADIO)==0){
            for(int i=0;i<ATTACHMENT_SLOTS;i++)
                if(attachmentInputs.getItem(i).is(EngineItems.BOAT_RADIO.get())){
                    BoatRadioStorage.install(boat,attachmentInputs.getItem(i));
                    break;
                }
        }
        boat.getOrCreateTag().put("BoatAttachmentSlots",saved);
        var motor=BoatPaintSessions.writableMotor(boat);
        motor.putBoolean("Installed",(next&1)!=0);
        motor.putBoolean("PoleLightInstalled",(next&2)!=0);
        motor.putBoolean("CanisterInstalled",(next&4)!=0);
        motor.putBoolean("IceBreakerInstalled",(next&8)!=0);
        motor.putBoolean("RadioInstalled",(next&16)!=0);
        if((next&1)==0){
            motor.putBoolean("Running",false);
            motor.putInt("Throttle",0);
        }
        boolean silk=false;
        for(int i=0;i<ATTACHMENT_SLOTS;i++)
            if(typeIndex(attachmentInputs.getItem(i))==3)
                silk=net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getItemEnchantmentLevel(
                        net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,
                        attachmentInputs.getItem(i))>0;
        motor.putBoolean("IceBreakerSilk",silk);
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
            if(index>=MATERIAL_SLOTS && original.is(EngineItems.BOAT_RADIO.get())){
                BoatStandBlockEntity stand=selectedStand();
                if(stand!=null && !stand.getItem(0).isEmpty()){
                    ItemStack storedBoat=stand.getItem(0).copy();
                    BoatRadioStorage.detach(storedBoat,original);
                    stand.setItem(0,storedBoat);
                }
            }
            if(!moveItemStackTo(original,MATERIAL_SLOTS+ATTACHMENT_SLOTS,slots.size(),true))
                return ItemStack.EMPTY;
        } else {
            boolean moved=false;
            for(int i=0;i<ATTACHMENT_SLOTS && !moved;i++)
                if(typeIndex(original)>=0 && !slots.get(MATERIAL_SLOTS+i).hasItem())
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
