package gr.daldon.simpleboatengine.common;

import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class BoatTrackerItem extends Item {
    BoatTrackerItem(Properties properties) {
        super(properties);
    }

    static void bind(ItemStack tracker, UUID id, BoatNavigationData data) {
        tracker.getOrCreateTag().putUUID("Boat", id);
        refresh(tracker, data);
    }

    private static void refresh(ItemStack stack, BoatNavigationData data) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID("Boat")) return;
        UUID id = tag.getUUID("Boat");
        Component name = data.targetName(id);
        if (name != null) {
            String serialized = Component.Serializer.toJson(name);
            if (!serialized.equals(tag.getString("TargetName"))) tag.putString("TargetName", serialized);
        } else {
            tag.remove("TargetName");
        }
        GlobalPos position = data.target(id);
        if (position == null) return;
        String dimension = position.dimension().location().toString();
        long packedPos = position.pos().asLong();
        if (!tag.getString("TargetDimension").equals(dimension)) tag.putString("TargetDimension", dimension);
        if (!tag.contains("TargetPosition") || tag.getLong("TargetPosition") != packedPos)
            tag.putLong("TargetPosition", packedPos);
    }

    public static GlobalPos target(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("TargetPosition")) return null;
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("TargetDimension"));
        return dimension == null ? null : GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension),
            BlockPos.of(tag.getLong("TargetPosition")));
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity carrier, int slot, boolean selected) {
        if (level instanceof ServerLevel server && level.getGameTime() % 10 == 0)
            refresh(stack, BoatNavigationData.get(server));
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        GlobalPos position = target(stack);
        if (position == null) {
            lines.add(Component.translatable("tooltip.daldonsboatadditions.tracker_unbound").withStyle(ChatFormatting.GRAY));
        } else {
            try {
                Component name = Component.Serializer.fromJson(stack.getTag().getString("TargetName"));
                if (name != null) lines.add(Component.translatable("tooltip.daldonsboatadditions.tracker_boat", name)
                    .withStyle(ChatFormatting.AQUA));
            } catch (com.google.gson.JsonParseException ignored) {
                // Older or externally edited item data can lack a readable name.
            }
            BlockPos pos = position.pos();
            lines.add(Component.translatable("tooltip.daldonsboatadditions.tracker_position",
                pos.getX(), pos.getY(), pos.getZ(), position.dimension().location().toString()).withStyle(ChatFormatting.GRAY));
        }
    }
}
