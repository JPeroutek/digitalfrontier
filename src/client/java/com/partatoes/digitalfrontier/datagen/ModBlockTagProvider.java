package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.tag.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        getOrCreateTagBuilder(ModTags.Blocks.GRID_BLOCK)
                .add(ModBlocks.GRIDSTONE_BLOCK)
                .add(ModBlocks.CRACKED_GRIDSTONE_BLOCK)
                .add(ModBlocks.GRIDSTONE_BRICKS_BLOCK)
                .add(ModBlocks.LIME_GRIDSTONE_BLOCK)
                .add(ModBlocks.BLUE_GRIDSTONE_BLOCK)
                .add(ModBlocks.RED_GRIDSTONE_BLOCK)
                .add(ModBlocks.WHITE_GRIDSTONE_BLOCK)
                .add(ModBlocks.ORANGE_GRIDSTONE_BLOCK)
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

        getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
                .add(ModBlocks.GRIDSTONE_BLOCK)
                .add(ModBlocks.CRACKED_GRIDSTONE_BLOCK)
                .add(ModBlocks.GRIDSTONE_BRICKS_BLOCK)
                .add(ModBlocks.LIME_GRIDSTONE_BLOCK)
                .add(ModBlocks.BLUE_GRIDSTONE_BLOCK)
                .add(ModBlocks.RED_GRIDSTONE_BLOCK)
                .add(ModBlocks.WHITE_GRIDSTONE_BLOCK)
                .add(ModBlocks.ORANGE_GRIDSTONE_BLOCK)
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
                .add(ModBlocks.GRIDSTONE_PRESSURE_PLATE);

        getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.LUMINANCE_ORE);

        getOrCreateTagBuilder(BlockTags.WALLS)
                .add(ModBlocks.GRIDSTONE_WALL)
                .add(ModBlocks.CRACKED_GRIDSTONE_WALL)
                .add(ModBlocks.GRIDSTONE_BRICKS_WALL);
    }
}
