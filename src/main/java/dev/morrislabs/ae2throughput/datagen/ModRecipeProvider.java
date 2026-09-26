package dev.morrislabs.ae2throughput.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;

import appeng.core.definitions.AEItems;

import dev.morrislabs.ae2throughput.registry.ModParts;

/**
 * Generates the shaped crafting recipe for the throughput monitor.
 *
 * <pre>
 *   . C .
 *   C F C
 *   . C .
 * </pre>
 *
 * C = AE2 Calculation Processor, F = AE2 Fluix Crystal
 */
public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModParts.THROUGHPUT_MONITOR.get())
                .pattern(" C ")
                .pattern("CFC")
                .pattern(" C ")
                .define('C', AEItems.CALCULATION_PROCESSOR)
                .define('F', AEItems.FLUIX_CRYSTAL)
                .unlockedBy("has_calculation_processor", has(AEItems.CALCULATION_PROCESSOR))
                .save(output);
    }
}
