package gr.daldon.simpleboatengine.common;

import java.util.List;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public record BoatAttachmentTooltip(List<ItemStack> attachments) implements TooltipComponent {}
