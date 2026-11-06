package gr.daldon.simpleboatengine.common;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Center: any fishing boat. Right: hull dye. Below: interior-floor dye. */
public final class FishingBoatPaintingRecipe extends CustomRecipe {
    public FishingBoatPaintingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        if (grid.getWidth() != 3 || grid.getHeight() != 3) return false;
        if (!(grid.getItem(4).getItem() instanceof SkiffBoatItem)) return false;
        boolean hull = grid.getItem(5).getItem() instanceof DyeItem;
        boolean floor = grid.getItem(7).getItem() instanceof DyeItem;
        if (!hull && !floor) return false;
        for (int slot = 0; slot < 9; slot++) {
            if (slot == 4 || slot == 5 && hull || slot == 7 && floor) continue;
            if (!grid.getItem(slot).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        if (grid.getWidth() != 3 || grid.getHeight() != 3) return ItemStack.EMPTY;
        ItemStack boat = grid.getItem(4);
        if (!(boat.getItem() instanceof SkiffBoatItem)) return ItemStack.EMPTY;
        ItemStack result = boat.copy();
        result.setCount(1);
        if (grid.getItem(5).getItem() instanceof DyeItem dye)
            BoatPaint.setHull(result, dye.getDyeColor().getId());
        if (grid.getItem(7).getItem() instanceof DyeItem dye)
            BoatPaint.setFloor(result, dye.getDyeColor().getId());
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EngineRecipes.PAINT_BOAT.get();
    }
}
