package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.item.ModItems;
import com.partatoes.digitalfrontier.tag.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.RecipeProvider;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.BYTE,1)
                .input(ModTags.Items.BIT)
                .input(ModTags.Items.BIT)
                .input(ModTags.Items.BIT)
                .input(ModTags.Items.BIT)
                .input(ModTags.Items.BIT)
                .input(ModTags.Items.BIT)
                .input(ModTags.Items.BIT)
                .input(ModTags.Items.BIT)
                .criterion("has_bit", RecipeProvider.conditionsFromTag(ModTags.Items.BIT))
                .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.REDSTONE, ModItems.COAL_DUST, 1)
                .input(ModItems.BINARY_0)
                .input(ModItems.LUMINESSENCE)
                .criterion(hasItem(ModItems.BINARY_0), RecipeProvider.conditionsFromItem(ModItems.BINARY_0))
                .criterion(hasItem(ModItems.LUMINESSENCE), RecipeProvider.conditionsFromItem(ModItems.LUMINESSENCE))
                .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.REDSTONE, ModItems.PIXEL_DUST, 1)
                .input(ModItems.BINARY_1)
                .input(ModItems.LUMINESSENCE)
                .criterion(hasItem(ModItems.BINARY_1), RecipeProvider.conditionsFromItem(ModItems.BINARY_1))
                .criterion(hasItem(ModItems.LUMINESSENCE), RecipeProvider.conditionsFromItem(ModItems.LUMINESSENCE))
                .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.REDSTONE, Items.REDSTONE, 2)
                .input(Items.REDSTONE)
                .input(ModItems.LUMINESSENCE)
                .criterion(hasItem(Items.REDSTONE), RecipeProvider.conditionsFromItem(Items.REDSTONE))
                .criterion(hasItem(ModItems.LUMINESSENCE), RecipeProvider.conditionsFromItem(ModItems.LUMINESSENCE))
                .offerTo(exporter);

        offer2x2CompactingRecipe(exporter, RecipeCategory.REDSTONE, ModBlocks.PIXEL_BLOCK, ModItems.PIXEL_DUST);
    }
}
