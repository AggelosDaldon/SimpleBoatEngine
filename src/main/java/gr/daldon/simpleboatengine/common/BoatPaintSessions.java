package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.Map;

/** Atomic painting: Preview changes no inventory, Cancel restores saved boat,
 * Apply checks and consumes all ingredients before committing that one stand.
 */
public final class BoatPaintSessions {
    private BoatPaintSessions() {}
    public static final int PREVIEW=0, APPLY=1, CANCEL=2;
    public static final int ENGINE=1, POLE=2, CANISTER=4, BREAKER=8;

    private static int mask(ItemStack stack) {
        CompoundTag data=motor(stack);
        int mask=0;
        if(data.getBoolean("Installed")) mask |= ENGINE;
        if(data.getBoolean("PoleLightInstalled")) mask |= POLE;
        if(data.getBoolean("CanisterInstalled")) mask |= CANISTER;
        if(data.getBoolean("IceBreakerInstalled")) mask |= BREAKER;
        return mask;
    }
    public static int attachmentMask(ItemStack boat){return mask(boat);}
    public static CompoundTag motor(ItemStack stack) {
        if(!stack.hasTag() || !stack.getTag().contains("PackedFishingBoat",10))
            return new CompoundTag();
        return stack.getTag().getCompound("PackedFishingBoat").getCompound("ForgeData")
            .getCompound("SimpleBoatEngine");
    }
    public static CompoundTag writableMotor(ItemStack stack) {
        CompoundTag packed=stack.getOrCreateTag().getCompound("PackedFishingBoat");
        if(!stack.getOrCreateTag().contains("PackedFishingBoat",10)) {
            packed=new CompoundTag(); stack.getOrCreateTag().put("PackedFishingBoat",packed);
        }
        CompoundTag forge=packed.getCompound("ForgeData");
        packed.put("ForgeData",forge);
        CompoundTag data=forge.getCompound("SimpleBoatEngine");
        forge.put("SimpleBoatEngine",data);
        return data;
    }
    /** Requirements in a stable order: the GUI uses precisely this list. */
    public static java.util.LinkedHashMap<Item,Integer> requirements(
            ItemStack original,int[] colors,int nameColor,int attachments) {
        java.util.LinkedHashMap<Item,Integer> cost=new java.util.LinkedHashMap<>();
        if(colors==null || colors.length!=BoatPaint.CHANNELS)return cost;
        for(int i=0;i<colors.length;i++){
            if(colors[i]>=0 && colors[i]!=(BoatPaint.get(original,i))) {
                if(i==BoatPaint.ANTIFOUL) add(cost,Items.BLACK_DYE,1);
                else add(cost,dyeItem(colors[i]),1);
            }
        }
        if(nameColor>=0 && nameColor!=BoatPaint.nameColor(original))
            add(cost,dyeItem(nameColor),1);
        int before=mask(original);
        for(int bit:new int[]{ENGINE,POLE,CANISTER,BREAKER})
            if((attachments&bit)!=0 && (before&bit)==0)
                add(cost,bit==ENGINE?EngineItems.ENGINE.get():
                    bit==POLE?EngineItems.POLE_LIGHT.get():
                    bit==CANISTER?EngineItems.FUEL_CANISTER.get():EngineItems.ICE_BREAKER.get(),1);
        return cost;
    }
    private static void tell(ServerPlayer p,String msg){
        p.displayClientMessage(Component.literal(msg),true);
    }

    /** Request colors array length 7, name, name dye, attachment toggle mask.
     * All uncommitted values are derived from the unmodified original boat.
     */
    public static void execute(ServerPlayer player, BoatCustomizerMenu menu,
            BlockPos standPos, int[] colors, String name, int nameColor,
            int attachments, int action) {
        BoatStandBlockEntity stand=menu.selectedStand();
        if(menu.standCount()==0 || !standPos.equals(menu.selectedStandPos())
            || stand==null || colors==null || colors.length!=BoatPaint.CHANNELS) return;
        if(stand.previewLockedFor(player.getUUID())) {tell(player,"Another player is previewing this boat.");return;}
        if(action==CANCEL) {stand.cancelPreview(player.getUUID());tell(player,"Changes cancelled.");return;}
        ItemStack original=stand.previewOriginal();
        if(!(original.getItem() instanceof SkiffBoatItem)) return;
        ItemStack proposed=original.copy();
        for(int i=0;i<BoatPaint.CHANNELS;i++){
            if(colors[i]<-1 || colors[i]>=(i==BoatPaint.ANTIFOUL?5:16))return;
            if(colors[i]>=0) BoatPaint.set(proposed,i,colors[i]);
        }
        BoatPaint.setName(proposed,name==null?"":name);
        if(nameColor>=0 && nameColor<16) BoatPaint.setNameColor(proposed,nameColor);
        if(attachments<0 || attachments>15)return;
        if((attachments&(POLE|CANISTER))!=0 && (attachments&ENGINE)==0){
            tell(player,"Engine required for pole light or reserve fuel canister.");
            return;
        }
        CompoundTag engine=writableMotor(proposed);
        engine.putBoolean("Installed",(attachments&ENGINE)!=0);
        engine.putBoolean("PoleLightInstalled",(attachments&POLE)!=0);
        engine.putBoolean("CanisterInstalled",(attachments&CANISTER)!=0);
        engine.putBoolean("IceBreakerInstalled",(attachments&BREAKER)!=0);
        if((attachments&ENGINE)==0) {engine.putBoolean("Running",false);engine.putInt("Throttle",0);}
        Map<Item,Integer> cost=requirements(original,colors,nameColor,attachments);
        int before=mask(original);
        // Preserve Silk Touch from the actual attachment consumed in the player's inventory.
        boolean silk=motor(original).getBoolean("IceBreakerSilk");
        if((attachments&BREAKER)!=0 && (before&BREAKER)==0) {
            for(int i=0;i<menu.materialSlotCount();i++) {
                ItemStack candidate=menu.materialItem(i);
                if(candidate.is(EngineItems.ICE_BREAKER.get())) {
                    silk=net.minecraft.world.item.enchantment.EnchantmentHelper
                        .getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,candidate)>0;
                    break;
                }
            }
        }
        engine.putBoolean("IceBreakerSilk",silk);
        for(int bit : new int[]{ENGINE,POLE,CANISTER,BREAKER})
            if((attachments&bit)!=0 && (before&bit)==0)
                add(cost,bit==ENGINE?EngineItems.ENGINE.get():
                    bit==POLE?EngineItems.POLE_LIGHT.get():
                    bit==CANISTER?EngineItems.FUEL_CANISTER.get():EngineItems.ICE_BREAKER.get(),1);
        if(action==APPLY) {
            if(!player.getAbilities().instabuild && !menu.hasMaterials(cost)){
                tell(player,"Missing materials: "+format(cost,player));return;
            }
            if(!player.getAbilities().instabuild)menu.consumeMaterials(cost);
            for(int bit : new int[]{ENGINE,POLE,CANISTER,BREAKER})
                if((before&bit)!=0 && (attachments&bit)==0) {
                    ItemStack returned=new ItemStack(bit==ENGINE?EngineItems.ENGINE.get():
                        bit==POLE?EngineItems.POLE_LIGHT.get():
                        bit==CANISTER?EngineItems.FUEL_CANISTER.get():EngineItems.ICE_BREAKER.get());
                    if(bit==BREAKER && motor(original).getBoolean("IceBreakerSilk"))
                        returned.enchant(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,1);
                    if(!player.getInventory().add(returned))player.drop(returned,false);
                }
            stand.setItem(0,proposed);
            stand.finishPreview();
            tell(player,"Changes applied to selected boat.");
        } else if(action==PREVIEW) {
            stand.beginPreview(player.getUUID());
            stand.setItem(0,proposed);
            tell(player,"Preview on parked boat. Reopen table to Apply or Cancel.");
        }
    }
    private static Item dyeItem(int id){
        return switch(DyeColor.byId(id)) {
            case WHITE -> Items.WHITE_DYE; case ORANGE -> Items.ORANGE_DYE;
            case MAGENTA -> Items.MAGENTA_DYE; case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
            case YELLOW -> Items.YELLOW_DYE; case LIME -> Items.LIME_DYE;
            case PINK -> Items.PINK_DYE; case GRAY -> Items.GRAY_DYE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE; case CYAN -> Items.CYAN_DYE;
            case PURPLE -> Items.PURPLE_DYE; case BLUE -> Items.BLUE_DYE;
            case BROWN -> Items.BROWN_DYE; case GREEN -> Items.GREEN_DYE;
            case RED -> Items.RED_DYE; case BLACK -> Items.BLACK_DYE;
        };
    }
    private static void add(Map<Item,Integer> m,Item i,int n){m.merge(i,n,Integer::sum);}
    private static boolean hasAll(ServerPlayer p,Map<Item,Integer> m){
        for(var e:m.entrySet()){
            int count=0;
            for(int i=0;i<p.getInventory().getContainerSize();i++){
                ItemStack s=p.getInventory().getItem(i);
                if(s.is(e.getKey()))count+=s.getCount();
            }
            if(count<e.getValue())return false;
        }
        return true;
    }
    private static void consume(ServerPlayer p,Map<Item,Integer> m){
        for(var e:m.entrySet()){
            int need=e.getValue();
            for(int i=0;i<p.getInventory().getContainerSize() && need>0;i++){
                ItemStack s=p.getInventory().getItem(i);
                if(!s.is(e.getKey()))continue;
                int used=Math.min(need,s.getCount());
                s.shrink(used);need-=used;
            }
        }
        p.getInventory().setChanged();
    }
    private static String format(Map<Item,Integer> cost,ServerPlayer p) {
        StringBuilder result=new StringBuilder();
        for(var e:cost.entrySet()) {
            if(result.length()>0)result.append(", ");
            result.append(e.getValue()).append(" ").append(e.getKey().getDescription().getString());
        }
        return result.toString();
    }
}
