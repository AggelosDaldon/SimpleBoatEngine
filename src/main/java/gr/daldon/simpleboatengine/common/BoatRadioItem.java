package gr.daldon.simpleboatengine.common;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

final class BoatRadioItem extends EngineItem {
    BoatRadioItem(Properties properties) {
        super(properties);
    }

    @Override public void appendHoverText(ItemStack radio, Level level, List<Component> lines, TooltipFlag flags) {
        super.appendHoverText(radio, level, lines, flags);
        boolean hasDiscs = false;
        for (Tag entry : BoatRadioStorage.discs(radio)) {
            CompoundTag discTag = (CompoundTag) entry;
            int slot = discTag.getByte("Slot") & 255;
            ItemStack disc = ItemStack.of(discTag);
            if (slot >= 9 || disc.isEmpty() || disc.is(this)) continue;
            hasDiscs = true;
            lines.add(Component.literal((slot + 1) + ". " + BoatRadioTrackNames.track(disc))
                .withStyle(ChatFormatting.GRAY));
        }
        if (!hasDiscs) lines.add(Component.translatable("tooltip.daldonsboatadditions.radio_empty")
            .withStyle(ChatFormatting.GRAY));
    }
}
