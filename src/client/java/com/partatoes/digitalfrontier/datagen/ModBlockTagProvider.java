package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.tag.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {

    public ModBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        builder(ModTags.Blocks.GRID_BLOCK)
                .add(key(ModBlocks.GRIDSTONE_BLOCK))
                .add(key(ModBlocks.CRACKED_GRIDSTONE_BLOCK))
                .add(key(ModBlocks.GRIDSTONE_BRICKS_BLOCK))
                .add(key(ModBlocks.LUMINANCE_ORE))
                .add(key(ModBlocks.BOOSTER_BLOCK))
                .add(key(ModBlocks.GRIDSTONE_STAIRS))
                .add(key(ModBlocks.GRIDSTONE_SLAB))
                .add(key(ModBlocks.GRIDSTONE_WALL))
                .add(key(ModBlocks.CRACKED_GRIDSTONE_STAIRS))
                .add(key(ModBlocks.CRACKED_GRIDSTONE_SLAB))
                .add(key(ModBlocks.CRACKED_GRIDSTONE_WALL))
                .add(key(ModBlocks.GRIDSTONE_BRICKS_STAIRS))
                .add(key(ModBlocks.GRIDSTONE_BRICKS_SLAB))
                .add(key(ModBlocks.GRIDSTONE_BRICKS_WALL))
                .add(key(ModBlocks.GRIDSTONE_PRESSURE_PLATE))
                .add(key(ModBlocks.LUMINANCE_ORE))
                .add(key(ModBlocks.GRIDSTONE_WALL));

        TagAppender<Block> pickaxeMineable = builder(BlockTags.MINEABLE_WITH_PICKAXE);
        ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.values()
                .forEach(colorMapping -> colorMapping.values().forEach(block -> pickaxeMineable.add(key(block))));
        pickaxeMineable
                .add(key(ModBlocks.GRIDSTONE_BLOCK))
                .add(key(ModBlocks.CRACKED_GRIDSTONE_BLOCK))
                .add(key(ModBlocks.GRIDSTONE_BRICKS_BLOCK))
                .add(key(ModBlocks.LUMINANCE_ORE))
                .add(key(ModBlocks.BOOSTER_BLOCK))
                .add(key(ModBlocks.GRIDSTONE_STAIRS))
                .add(key(ModBlocks.GRIDSTONE_SLAB))
                .add(key(ModBlocks.GRIDSTONE_WALL))
                .add(key(ModBlocks.CRACKED_GRIDSTONE_STAIRS))
                .add(key(ModBlocks.CRACKED_GRIDSTONE_SLAB))
                .add(key(ModBlocks.CRACKED_GRIDSTONE_WALL))
                .add(key(ModBlocks.GRIDSTONE_BRICKS_STAIRS))
                .add(key(ModBlocks.GRIDSTONE_BRICKS_SLAB))
                .add(key(ModBlocks.GRIDSTONE_BRICKS_WALL))
                .add(key(ModBlocks.GRIDSTONE_PRESSURE_PLATE))
                .add(key(ModBlocks.ARRIVAL_PAD))
                .add(key(ModBlocks.DIGITIZING_LASER))
                .add(key(ModBlocks.GRID_TUNING_LASER))
                .add(key(ModBlocks.STRONGHOLD_UPLINK));

        builder(BlockTags.NEEDS_IRON_TOOL)
                .add(key(ModBlocks.LUMINANCE_ORE));

        builder(BlockTags.WALLS)
                .add(key(ModBlocks.GRIDSTONE_WALL))
                .add(key(ModBlocks.CRACKED_GRIDSTONE_WALL))
                .add(key(ModBlocks.GRIDSTONE_BRICKS_WALL));
    }

    private static ResourceKey<Block> key(Block block) {
        return BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
    }
}
