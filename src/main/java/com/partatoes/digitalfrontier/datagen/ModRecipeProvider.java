package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.item.ModItems;
import com.partatoes.digitalfrontier.tag.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.RecipeProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        // 8 bits in a byte
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

        offer2x2CompactingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_BLOCK, ModBlocks.GRIDSTONE_BLOCK);

        offerPressurePlateRecipe(exporter, ModBlocks.GRIDSTONE_PRESSURE_PLATE, ModBlocks.GRIDSTONE_BLOCK);
        offerSlabRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_SLAB, ModBlocks.GRIDSTONE_BLOCK);
        offerWallRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_WALL, ModBlocks.GRIDSTONE_BLOCK);
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_STAIRS, 4)
                .pattern("G  ")
                .pattern("GG ")
                .pattern("GGG")
                .input('G', ModBlocks.GRIDSTONE_BLOCK)
                .criterion(hasItem(ModBlocks.GRIDSTONE_BLOCK), conditionsFromItem(ModBlocks.GRIDSTONE_BLOCK))
                .offerTo(exporter, new Identifier(getRecipeName(ModBlocks.GRIDSTONE_STAIRS)));

        ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, ModItems.LIGHTCYCLE_BATON, 1)
                .pattern("B")
                .pattern("B")
                .input('B', ModItems.BINARY_1)
                .criterion(hasItem(ModItems.BINARY_1), conditionsFromItem(ModItems.BINARY_1))
                .offerTo(exporter);
    }
}
