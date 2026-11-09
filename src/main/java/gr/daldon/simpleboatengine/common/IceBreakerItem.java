package gr.daldon.simpleboatengine.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/** Silk Touch is explicitly supported even though this is a boat attachment. */
public final class IceBreakerItem extends EngineItem {
    public IceBreakerItem(Properties properties){super(properties);}
    @Override public boolean isEnchantable(ItemStack stack){return true;}
    @Override public int getEnchantmentValue(){return 12;}
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack,Enchantment enchant){
        return enchant==Enchantments.SILK_TOUCH || super.canApplyAtEnchantingTable(stack,enchant);
    }
}
