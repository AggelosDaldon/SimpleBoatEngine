package gr.daldon.simpleboatengine.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Discs belong to one physical radio. They travel either on the radio item
 * or inside the packed boat while installed, never as a shared/global playlist.
 */
public final class BoatRadioStorage {
    private static final String DISCS="FishingBoatRadioDiscs";
    private static final String ITEM_DISCS="StoredRadioDiscs";
    private BoatRadioStorage(){}

    public static void detach(ItemStack boat,ItemStack radio){
        if(boat.isEmpty() || radio.isEmpty())return;
        if(!boat.hasTag() || !boat.getTag().contains("PackedFishingBoat",Tag.TAG_COMPOUND))
            return;
        CompoundTag packed=boat.getTag().getCompound("PackedFishingBoat");
        ListTag contents=packed.getList(DISCS,Tag.TAG_COMPOUND);
        if(!contents.isEmpty())radio.getOrCreateTag().put(ITEM_DISCS,contents.copy());
        else if(radio.hasTag())radio.getTag().remove(ITEM_DISCS);
        packed.remove(DISCS);
        packed.putInt("FishingBoatRadioIndex",-1);
        packed.putInt("FishingBoatRadioTicks",0);
    }

    public static void install(ItemStack boat,ItemStack radio){
        if(boat.isEmpty()||radio.isEmpty())return;
        CompoundTag packed=boat.getOrCreateTag().getCompound("PackedFishingBoat");
        if(!boat.getTag().contains("PackedFishingBoat",Tag.TAG_COMPOUND))
            boat.getTag().put("PackedFishingBoat",packed);
        ListTag contents=radio.hasTag() ?
            radio.getTag().getList(ITEM_DISCS,Tag.TAG_COMPOUND) : new ListTag();
        packed.put(DISCS,contents.copy());
        packed.putInt("FishingBoatRadioIndex",-1);
        packed.putInt("FishingBoatRadioTicks",0);
    }
    public static ListTag discs(ItemStack radio){
        return radio.hasTag() ?
            radio.getTag().getList(ITEM_DISCS,Tag.TAG_COMPOUND) : new ListTag();
    }
}
