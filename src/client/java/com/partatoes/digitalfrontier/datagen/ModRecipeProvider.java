package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.item.ModItems;
import com.partatoes.digitalfrontier.tag.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider wrapperLookup, BootstrapContext<Recipe<?>> recipes,
                                                  BootstrapContext<Advancement> advancements) {
        return new RecipeProvider(recipes, advancements) {
            @Override
            public void buildRecipes() {
                // 8 bits in a byte
                shapeless(RecipeCategory.MISC, ModItems.BYTE,1)
                        .requires(ModTags.Items.BIT)
                        .requires(ModTags.Items.BIT)
                        .requires(ModTags.Items.BIT)
                        .requires(ModTags.Items.BIT)
                        .requires(ModTags.Items.BIT)
                        .requires(ModTags.Items.BIT)
                        .requires(ModTags.Items.BIT)
                        .requires(ModTags.Items.BIT)
                        .unlockedBy("has_bit", has(ModTags.Items.BIT))
                        .save(output);

                shapeless(RecipeCategory.REDSTONE, ModItems.COAL_DUST, 1)
                        .requires(ModItems.BINARY_0)
                        .requires(ModItems.LUMINESSENCE)
                        .unlockedBy(getHasName(ModItems.BINARY_0), has(ModItems.BINARY_0))
                        .unlockedBy(getHasName(ModItems.LUMINESSENCE), has(ModItems.LUMINESSENCE))
                        .save(output);

                shapeless(RecipeCategory.REDSTONE, ModItems.PIXEL_DUST, 1)
                        .requires(ModItems.BINARY_1)
                        .requires(ModItems.LUMINESSENCE)
                        .unlockedBy(getHasName(ModItems.BINARY_1), has(ModItems.BINARY_1))
                        .unlockedBy(getHasName(ModItems.LUMINESSENCE), has(ModItems.LUMINESSENCE))
                        .save(output);

                shapeless(RecipeCategory.REDSTONE, Items.REDSTONE, 2)
                        .requires(Items.REDSTONE)
                        .requires(ModItems.LUMINESSENCE)
                        .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                        .unlockedBy(getHasName(ModItems.LUMINESSENCE), has(ModItems.LUMINESSENCE))
                        .save(output);

                twoByTwoPacker(RecipeCategory.REDSTONE, ModBlocks.PIXEL_BLOCK, ModItems.PIXEL_DUST);

                twoByTwoPacker(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_BLOCK, ModBlocks.GRIDSTONE_BLOCK);

                pressurePlate(ModBlocks.GRIDSTONE_PRESSURE_PLATE, ModBlocks.GRIDSTONE_BLOCK);
                slab(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_SLAB, ModBlocks.GRIDSTONE_BLOCK);
                wall(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_WALL, ModBlocks.GRIDSTONE_BLOCK);
                shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_STAIRS, 4)
                        .pattern("G  ")
                        .pattern("GG ")
                        .pattern("GGG")
                        .define('G', ModBlocks.GRIDSTONE_BLOCK)
                        .unlockedBy(getHasName(ModBlocks.GRIDSTONE_BLOCK), has(ModBlocks.GRIDSTONE_BLOCK))
                        .save(output, ResourceKey.create(Registries.RECIPE, Identifier.parse(getSimpleRecipeName(ModBlocks.GRIDSTONE_STAIRS))));

                shaped(RecipeCategory.TOOLS, ModItems.LIGHTCYCLE_BATON, 1)
                        .pattern("B")
                        .pattern("B")
                        .define('B', ModItems.BINARY_1)
                        .unlockedBy(getHasName(ModItems.BINARY_1), has(ModItems.BINARY_1))
                        .save(output);

                // The way into the grid is meant to be a mid/late-game build
                shaped(RecipeCategory.TRANSPORTATION, ModBlocks.DIGITIZING_LASER, 1)
                        .pattern("LDL")
                        .pattern("PEP")
                        .pattern("III")
                        .define('L', ModItems.LUMINESSENCE)
                        .define('D', Items.DIAMOND)
                        .define('P', ModBlocks.PIXEL_BLOCK)
                        .define('E', Items.ENDER_EYE)
                        .define('I', Items.IRON_BLOCK)
                        .unlockedBy(getHasName(ModItems.LUMINESSENCE), has(ModItems.LUMINESSENCE))
                        .save(output);

                // Tuning into other grids is end-game: a nether star and netherite on top of the laser
                shaped(RecipeCategory.TRANSPORTATION, ModBlocks.GRID_TUNING_LASER, 1)
                        .pattern(" S ")
                        .pattern("DLD")
                        .pattern("NPN")
                        .define('S', Items.NETHER_STAR)
                        .define('D', Items.DIAMOND)
                        .define('L', ModBlocks.DIGITIZING_LASER)
                        .define('N', Items.NETHERITE_INGOT)
                        .define('P', ModBlocks.PIXEL_BLOCK)
                        .unlockedBy(getHasName(ModBlocks.DIGITIZING_LASER), has(ModBlocks.DIGITIZING_LASER))
                        .save(output);

                shaped(RecipeCategory.TOOLS, ModItems.LASER_TUNER, 1)
                        .pattern(" L ")
                        .pattern("ICI")
                        .define('L', ModItems.LUMINESSENCE)
                        .define('I', Items.IRON_INGOT)
                        .define('C', Items.COMPASS)
                        .unlockedBy(getHasName(ModBlocks.DIGITIZING_LASER), has(ModBlocks.DIGITIZING_LASER))
                        .save(output);

                // Rare by design: built around an echo shard (ancient cities) and a totem of undying (raids, mansions)
                shaped(RecipeCategory.TOOLS, ModItems.ESCAPE_KEY, 1)
                        .pattern("PDP")
                        .pattern("DSD")
                        .pattern("PDP")
                        .define('P', Items.ENDER_PEARL)
                        .define('D', Items.DIAMOND)
                        .define('S', Items.ECHO_SHARD)
                        .unlockedBy(getHasName(Items.ECHO_SHARD), has(Items.ECHO_SHARD))
                        .save(output);

                shaped(RecipeCategory.TOOLS, ModItems.MEMORY_PAGING, 1)
                        .pattern("BLB")
                        .pattern("LTL")
                        .pattern("BLB")
                        .define('B', ModItems.BYTE)
                        .define('L', ModItems.LUMINESSENCE)
                        .define('T', Items.TOTEM_OF_UNDYING)
                        .unlockedBy(getHasName(Items.TOTEM_OF_UNDYING), has(Items.TOTEM_OF_UNDYING))
                        .save(output);

                // Memory Paging that keeps the inventory: a step further into the late game
                shaped(RecipeCategory.TOOLS, ModItems.CORE_DUMP, 1)
                        .pattern("ENE")
                        .pattern("LML")
                        .pattern("ENE")
                        .define('E', Items.ECHO_SHARD)
                        .define('N', Items.NETHERITE_INGOT)
                        .define('L', ModItems.LUMINESSENCE)
                        .define('M', ModItems.MEMORY_PAGING)
                        .unlockedBy(getHasName(ModItems.MEMORY_PAGING), has(ModItems.MEMORY_PAGING))
                        .save(output);

                slab(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_SLAB, ModBlocks.CRACKED_GRIDSTONE_BLOCK);
                wall(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_WALL, ModBlocks.CRACKED_GRIDSTONE_BLOCK);
                shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_STAIRS, 4)
                        .pattern("G  ")
                        .pattern("GG ")
                        .pattern("GGG")
                        .define('G', ModBlocks.CRACKED_GRIDSTONE_BLOCK)
                        .unlockedBy(getHasName(ModBlocks.CRACKED_GRIDSTONE_BLOCK), has(ModBlocks.CRACKED_GRIDSTONE_BLOCK))
                        .save(output, ResourceKey.create(Registries.RECIPE, Identifier.parse(getSimpleRecipeName(ModBlocks.CRACKED_GRIDSTONE_STAIRS))));

                slab(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_SLAB, ModBlocks.GRIDSTONE_BRICKS_BLOCK);
                wall(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_WALL, ModBlocks.GRIDSTONE_BRICKS_BLOCK);
                shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_STAIRS, 4)
                        .pattern("G  ")
                        .pattern("GG ")
                        .pattern("GGG")
                        .define('G', ModBlocks.GRIDSTONE_BRICKS_BLOCK)
                        .unlockedBy(getHasName(ModBlocks.GRIDSTONE_BRICKS_BLOCK), has(ModBlocks.GRIDSTONE_BRICKS_BLOCK))
                        .save(output, ResourceKey.create(Registries.RECIPE, Identifier.parse(getSimpleRecipeName(ModBlocks.GRIDSTONE_BRICKS_STAIRS))));

                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_BLOCK, ModBlocks.GRIDSTONE_BLOCK);

                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_STAIRS, ModBlocks.GRIDSTONE_BLOCK);
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_WALL, ModBlocks.GRIDSTONE_BLOCK);
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_SLAB, ModBlocks.GRIDSTONE_BLOCK, 2);
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_SLAB, ModBlocks.GRIDSTONE_BLOCK, 2);

                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_STAIRS, ModBlocks.GRIDSTONE_BRICKS_BLOCK);
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_WALL, ModBlocks.GRIDSTONE_BRICKS_BLOCK);
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GRIDSTONE_BRICKS_SLAB, ModBlocks.GRIDSTONE_BRICKS_BLOCK, 2);

                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_STAIRS, ModBlocks.CRACKED_GRIDSTONE_BLOCK);
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_WALL, ModBlocks.CRACKED_GRIDSTONE_BLOCK);
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRACKED_GRIDSTONE_SLAB, ModBlocks.CRACKED_GRIDSTONE_BLOCK, 2);
            }
        };
    }

    @Override
    public String getName() {
        return DigitalFrontier.MOD_ID + " Recipes";
    }
}
