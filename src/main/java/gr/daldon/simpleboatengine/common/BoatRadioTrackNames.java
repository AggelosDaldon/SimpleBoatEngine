package gr.daldon.simpleboatengine.common;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.TooltipFlag;

/** The song identity printed in the music-disc tooltip, never the generic item name. */
public final class BoatRadioTrackNames {
    private BoatRadioTrackNames(){}
    public static String track(ItemStack disc){
        if(disc.isEmpty())return "";
        if(disc.getItem() instanceof RecordItem record)
            return record.getDisplayName().getString();
        // Etched and similar modded records often expose the track in a tooltip.
        try {
            for(Component line:disc.getTooltipLines(null,TooltipFlag.NORMAL)){
                String value=ChatFormatting.stripFormatting(line.getString());
                if(value==null || value.isBlank())continue;
                if(value.equalsIgnoreCase(disc.getHoverName().getString()))continue;
                if(value.toLowerCase(java.util.Locale.ROOT).startsWith("minecraft:"))continue;
                if(value.toLowerCase(java.util.Locale.ROOT).contains("record"))continue;
                return value;
            }
        } catch(RuntimeException ignored) {
            // Some third-party discs require a client world for their tooltip.
        }
        return disc.getHoverName().getString();
    }
}
