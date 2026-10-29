package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.tag.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {

    public ModBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        valueLookupBuilder(ModTags.Blocks.GRID_BLOCK)
                .add(ModBlocks.GRIDSTONE_BLOCK)
                .add(ModBlocks.CRACKED_GRIDSTONE_BLOCK)
                .add(ModBlocks.GRIDSTONE_BRICKS_BLOCK)
                .add(ModBlocks.LUMINANCE_ORE)
                .add(ModBlocks.BOOSTER_BLOCK)
                .add(ModBlocks.GRIDSTONE_STAIRS)
                .add(ModBlocks.GRIDSTONE_SLAB)
                .add(ModBlocks.GRIDSTONE_WALL)
                .add(ModBlocks.CRACKED_GRIDSTONE_STAIRS)
                .add(ModBlocks.CRACKED_GRIDSTONE_SLAB)
                .add(ModBlocks.CRACKED_GRIDSTONE_WALL)
                .add(ModBlocks.GRIDSTONE_BRICKS_STAIRS)
                .add(ModBlocks.GRIDSTONE_BRICKS_SLAB)
                .add(ModBlocks.GRIDSTONE_BRICKS_WALL)
                .add(ModBlocks.GRIDSTONE_PRESSURE_PLATE)
                .add(ModBlocks.LUMINANCE_ORE)
                .add(ModBlocks.GRIDSTONE_WALL);

        TagAppender<Block, Block> pickaxeMineable = valueLookupBuilder(BlockTags.MINEABLE_WITH_PICKAXE);
        ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.values()
                .forEach(colorMapping -> colorMapping.values().forEach(pickaxeMineable::add));
        pickaxeMineable
                .add(ModBlocks.GRIDSTONE_BLOCK)
                .add(ModBlocks.CRACKED_GRIDSTONE_BLOCK)
                .add(ModBlocks.GRIDSTONE_BRICKS_BLOCK)
                .add(ModBlocks.LUMINANCE_ORE)
                .add(ModBlocks.BOOSTER_BLOCK)
                .add(ModBlocks.GRIDSTONE_STAIRS)
                .add(ModBlocks.GRIDSTONE_SLAB)
                .add(ModBlocks.GRIDSTONE_WALL)
                .add(ModBlocks.CRACKED_GRIDSTONE_STAIRS)
                .add(ModBlocks.CRACKED_GRIDSTONE_SLAB)
                .add(ModBlocks.CRACKED_GRIDSTONE_WALL)
                .add(ModBlocks.GRIDSTONE_BRICKS_STAIRS)
                .add(ModBlocks.GRIDSTONE_BRICKS_SLAB)
                .add(ModBlocks.GRIDSTONE_BRICKS_WALL)
                .add(ModBlocks.GRIDSTONE_PRESSURE_PLATE)
                .add(ModBlocks.ARRIVAL_PAD)
                .add(ModBlocks.DIGITIZING_LASER)
                .add(ModBlocks.GRID_TUNING_LASER)
                .add(ModBlocks.STRONGHOLD_UPLINK);

        valueLookupBuilder(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.LUMINANCE_ORE);

        valueLookupBuilder(BlockTags.WALLS)
                .add(ModBlocks.GRIDSTONE_WALL)
                .add(ModBlocks.CRACKED_GRIDSTONE_WALL)
                .add(ModBlocks.GRIDSTONE_BRICKS_WALL);
    }
}
