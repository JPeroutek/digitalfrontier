package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.item.ModItems;
import com.partatoes.digitalfrontier.tag.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipe.RecipeExporter;
import net.minecraft.data.recipe.RecipeGenerator;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeGenerator getRecipeGenerator(RegistryWrapper.WrapperLookup wrapperLookup, RecipeExporter recipeExporter) {
        return new RecipeGenerator(wrapperLookup, recipeExporter) {
            @Override
            public void generate() {
                // 8 bits in a byte
                createShapeless(RecipeCategory.MISC, ModItems.BYTE,1)
                        .input(ModTags.Items.BIT)
                        .input(ModTags.Items.BIT)
                        .input(ModTags.Items.BIT)
                        .input(ModTags.Items.BIT)
                        .input(ModTags.Items.BIT)
                        .input(ModTags.Items.BIT)
                        .input(ModTags.Items.BIT)
                        .input(ModTags.Items.BIT)
                        .criterion("has_bit", conditionsFromTag(ModTags.Items.BIT))
                        .offerTo(exporter);

                createShapeless(RecipeCategory.REDSTONE, ModItems.COAL_DUST, 1)
                        .input(ModItems.BINARY_0)
                        .input(ModItems.LUMINESSENCE)
                        .criterion(hasItem(ModItems.BINARY_0), conditionsFromItem(ModItems.BINARY_0))
                        .criterion(hasItem(ModItems.LUMINESSENCE), conditionsFromItem(ModItems.LUMINESSENCE))
                        .offerTo(exporter);

                createShapeless(RecipeCategory.REDSTONE, ModItems.PIXEL_DUST, 1)
                        .input(ModItems.BINARY_1)
                        .input(ModItems.LUMINESSENCE)
                        .criterion(hasItem(ModItems.BINARY_1), conditionsFromItem(ModItems.BINARY_1))
                        .criterion(hasItem(ModItems.LUMINESSENCE), conditionsFromItem(ModItems.LUMINESSENCE))
                        .offerTo(exporter);

                createShapeless(RecipeCategory.REDSTONE, Items.REDSTONE, 2)
                        .input(Items.REDSTONE)
                        .input(ModItems.LUMINESSENCE)
                        .criterion(hasItem(Items.REDSTONE), conditionsFromItem(Items.REDSTONE))
                        .criterion(hasItem(ModItems.LUMINESSENCE), conditionsFromItem(ModItems.LUMINESSENCE))
                        .offerTo(exporter);

                offer2x2CompactingRecipe(RecipeCategory.REDSTONE, ModBlocks.PIXEL_BLOCK, ModItems.PIXEL_DUST);

                offer2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_BLOCK, ModBlocks.GRIDSTONE_BLOCK);

                offerPressurePlateRecipe(ModBlocks.GRIDSTONE_PRESSURE_PLATE, ModBlocks.GRIDSTONE_BLOCK);
                offerSlabRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_SLAB, ModBlocks.GRIDSTONE_BLOCK);
                offerWallRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_WALL, ModBlocks.GRIDSTONE_BLOCK);
                createShaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_STAIRS, 4)
                        .pattern("G  ")
                        .pattern("GG ")
                        .pattern("GGG")
                        .input('G', ModBlocks.GRIDSTONE_BLOCK)
                        .criterion(hasItem(ModBlocks.GRIDSTONE_BLOCK), conditionsFromItem(ModBlocks.GRIDSTONE_BLOCK))
                        .offerTo(exporter, RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(getRecipeName(ModBlocks.GRIDSTONE_STAIRS))));

                createShaped(RecipeCategory.TOOLS, ModItems.LIGHTCYCLE_BATON, 1)
                        .pattern("B")
                        .pattern("B")
                        .input('B', ModItems.BINARY_1)
                        .criterion(hasItem(ModItems.BINARY_1), conditionsFromItem(ModItems.BINARY_1))
                        .offerTo(exporter);

                // The way into the grid is meant to be a mid/late-game build
                createShaped(RecipeCategory.TRANSPORTATION, ModBlocks.DIGITIZING_LASER, 1)
                        .pattern("LDL")
                        .pattern("PEP")
                        .pattern("III")
                        .input('L', ModItems.LUMINESSENCE)
                        .input('D', Items.DIAMOND)
                        .input('P', ModBlocks.PIXEL_BLOCK)
                        .input('E', Items.ENDER_EYE)
                        .input('I', Items.IRON_BLOCK)
                        .criterion(hasItem(ModItems.LUMINESSENCE), conditionsFromItem(ModItems.LUMINESSENCE))
                        .offerTo(exporter);

                // Tuning into other grids is end-game: a nether star and netherite on top of the laser
                createShaped(RecipeCategory.TRANSPORTATION, ModBlocks.GRID_TUNING_LASER, 1)
                        .pattern(" S ")
                        .pattern("DLD")
                        .pattern("NPN")
                        .input('S', Items.NETHER_STAR)
                        .input('D', Items.DIAMOND)
                        .input('L', ModBlocks.DIGITIZING_LASER)
                        .input('N', Items.NETHERITE_INGOT)
                        .input('P', ModBlocks.PIXEL_BLOCK)
                        .criterion(hasItem(ModBlocks.DIGITIZING_LASER), conditionsFromItem(ModBlocks.DIGITIZING_LASER))
                        .offerTo(exporter);

                createShaped(RecipeCategory.TOOLS, ModItems.LASER_TUNER, 1)
                        .pattern(" L ")
                        .pattern("ICI")
                        .input('L', ModItems.LUMINESSENCE)
                        .input('I', Items.IRON_INGOT)
                        .input('C', Items.COMPASS)
                        .criterion(hasItem(ModBlocks.DIGITIZING_LASER), conditionsFromItem(ModBlocks.DIGITIZING_LASER))
                        .offerTo(exporter);

                // Rare by design: built around an echo shard (ancient cities) and a totem of undying (raids, mansions)
                createShaped(RecipeCategory.TOOLS, ModItems.ESCAPE_KEY, 1)
                        .pattern("PDP")
                        .pattern("DSD")
                        .pattern("PDP")
                        .input('P', Items.ENDER_PEARL)
                        .input('D', Items.DIAMOND)
                        .input('S', Items.ECHO_SHARD)
                        .criterion(hasItem(Items.ECHO_SHARD), conditionsFromItem(Items.ECHO_SHARD))
                        .offerTo(exporter);

                createShaped(RecipeCategory.TOOLS, ModItems.MEMORY_PAGING, 1)
                        .pattern("BLB")
                        .pattern("LTL")
                        .pattern("BLB")
                        .input('B', ModItems.BYTE)
                        .input('L', ModItems.LUMINESSENCE)
                        .input('T', Items.TOTEM_OF_UNDYING)
                        .criterion(hasItem(Items.TOTEM_OF_UNDYING), conditionsFromItem(Items.TOTEM_OF_UNDYING))
                        .offerTo(exporter);

                // Memory Paging that keeps the inventory: a step further into the late game
                createShaped(RecipeCategory.TOOLS, ModItems.CORE_DUMP, 1)
                        .pattern("ENE")
                        .pattern("LML")
                        .pattern("ENE")
                        .input('E', Items.ECHO_SHARD)
                        .input('N', Items.NETHERITE_INGOT)
                        .input('L', ModItems.LUMINESSENCE)
                        .input('M', ModItems.MEMORY_PAGING)
                        .criterion(hasItem(ModItems.MEMORY_PAGING), conditionsFromItem(ModItems.MEMORY_PAGING))
                        .offerTo(exporter);

                offerSlabRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_SLAB, ModBlocks.CRACKED_GRIDSTONE_BLOCK);
                offerWallRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_WALL, ModBlocks.CRACKED_GRIDSTONE_BLOCK);
                createShaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_STAIRS, 4)
                        .pattern("G  ")
                        .pattern("GG ")
                        .pattern("GGG")
                        .input('G', ModBlocks.CRACKED_GRIDSTONE_BLOCK)
                        .criterion(hasItem(ModBlocks.CRACKED_GRIDSTONE_BLOCK), conditionsFromItem(ModBlocks.CRACKED_GRIDSTONE_BLOCK))
                        .offerTo(exporter, RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(getRecipeName(ModBlocks.CRACKED_GRIDSTONE_STAIRS))));

                offerSlabRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_SLAB, ModBlocks.GRIDSTONE_BRICKS_BLOCK);
                offerWallRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_WALL, ModBlocks.GRIDSTONE_BRICKS_BLOCK);
                createShaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_STAIRS, 4)
                        .pattern("G  ")
                        .pattern("GG ")
                        .pattern("GGG")
                        .input('G', ModBlocks.GRIDSTONE_BRICKS_BLOCK)
                        .criterion(hasItem(ModBlocks.GRIDSTONE_BRICKS_BLOCK), conditionsFromItem(ModBlocks.GRIDSTONE_BRICKS_BLOCK))
                        .offerTo(exporter, RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(getRecipeName(ModBlocks.GRIDSTONE_BRICKS_STAIRS))));

                offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_BLOCK, ModBlocks.GRIDSTONE_BLOCK);

                offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_STAIRS, ModBlocks.GRIDSTONE_BLOCK);
                offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_WALL, ModBlocks.GRIDSTONE_BLOCK);
                offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_SLAB, ModBlocks.GRIDSTONE_BLOCK, 2);
                offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_SLAB, ModBlocks.GRIDSTONE_BLOCK, 2);

                offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_STAIRS, ModBlocks.GRIDSTONE_BRICKS_BLOCK);
                offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_WALL, ModBlocks.GRIDSTONE_BRICKS_BLOCK);
                offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_SLAB, ModBlocks.GRIDSTONE_BRICKS_BLOCK, 2);

                offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_STAIRS, ModBlocks.CRACKED_GRIDSTONE_BLOCK);
                offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_WALL, ModBlocks.CRACKED_GRIDSTONE_BLOCK);
                offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_SLAB, ModBlocks.CRACKED_GRIDSTONE_BLOCK, 2);
            }
        };
    }

    @Override
    public String getName() {
        return DigitalFrontier.MOD_ID + " Recipes";
    }
}
