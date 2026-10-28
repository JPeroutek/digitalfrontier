package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.tag.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.data.tag.ProvidedTagBuilder;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
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

        ProvidedTagBuilder<Block, Block> pickaxeMineable = valueLookupBuilder(BlockTags.PICKAXE_MINEABLE);
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
