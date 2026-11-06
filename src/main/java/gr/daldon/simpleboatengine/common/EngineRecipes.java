package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EngineRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES =
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SimpleBoatEngine.ID);

    public static final RegistryObject<RecipeSerializer<FishingBoatPaintingRecipe>> PAINT_BOAT =
        RECIPES.register("paint_fishing_boat", () ->
            new SimpleCraftingRecipeSerializer<>(FishingBoatPaintingRecipe::new));

    private EngineRecipes() {}
}
