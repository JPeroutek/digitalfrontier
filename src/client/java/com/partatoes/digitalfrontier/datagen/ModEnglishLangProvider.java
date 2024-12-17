package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.ModVehicles;
import com.partatoes.digitalfrontier.item.ModItemGroups;
import com.partatoes.digitalfrontier.item.ModItems;
import com.partatoes.digitalfrontier.tag.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ModEnglishLangProvider extends FabricLanguageProvider {
    public ModEnglishLangProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generateTranslations(RegistryWrapper.WrapperLookup wrapperLookup, TranslationBuilder translationBuilder) {
        translationBuilder.add(ModItems.BINARY_0, "Binary 0");
        translationBuilder.add(ModItems.BINARY_1, "Binary 1");
        translationBuilder.add(ModItems.PIXEL_DUST, "Pixel Dust");
        translationBuilder.add(ModItems.COAL_DUST, "Coal Dust");
        translationBuilder.add(ModItems.LUMINESSENCE, "Luminessence");
        translationBuilder.add(ModItems.LUMINANCE_ORE_DETECTOR, "Luminessence Detector");
        translationBuilder.add("item.digitalfrontier.luminance_detector.tooltip", "Right click a block to detect luminance ore below it.");
        translationBuilder.add(ModItems.BYTE, "Byte");
        translationBuilder.add(ModItems.LIGHTCYCLE_BATON, "Lightcycle Baton");

        translationBuilder.add(ModBlocks.PIXEL_BLOCK, "Pixel");
        translationBuilder.add(ModBlocks.PIXEL_LAMP, "Pixel Lamp");
        translationBuilder.add(ModBlocks.GRIDSTONE_BLOCK, "Gridstone");
        translationBuilder.add(ModBlocks.CRACKED_GRIDSTONE_BLOCK, "Cracked Gridstone");
        translationBuilder.add(ModBlocks.GRIDSTONE_BRICKS_BLOCK, "Gridstone Bricks");
        translationBuilder.add(ModBlocks.LUMINANCE_ORE, "Luminance Ore");
        translationBuilder.add(ModBlocks.BOOSTER_BLOCK, "Boost Block");

        translationBuilder.add(ModTags.Items.BIT, "Bit");

        translationBuilder.add(ModEntities.PROGRAM, "Program");
        translationBuilder.add(ModVehicles.LIGHTCYCLE, "Light Cycle");

        translationBuilder.add("itemgroup.digitalfrontier", "Digital Frontier items");

//        "block.digitalfrontier.lime_gridstone": "Lime Gridstone",
//        "block.digitalfrontier.blue_gridstone": "Blue Gridstone",
//        "block.digitalfrontier.red_gridstone": "Red Gridstone",
//        "block.digitalfrontier.white_gridstone": "White Gridstone",
//        "block.digitalfrontier.orange_gridstone": "Orange Gridstone",
//
//
//        "block.digitalfrontier.gridstone_stairs": "Gridstone Stairs",
//        "block.digitalfrontier.gridstone_slab": "Gridstone Slab",
//        "block.digitalfrontier.gridstone_wall": "Gridstone Wall",
//        "block.digitalfrontier.gridstone_pressure_plate": "Gridstone Pressure Plate",
//
//        "block.digitalfrontier.cracked_gridstone_stairs": "Cracked Gridstone Stairs",
//        "block.digitalfrontier.cracked_gridstone_slab": "Cracked Gridstone Slab",
//        "block.digitalfrontier.cracked_gridstone_wall": "Cracked Gridstone Wall",
//
//        "block.digitalfrontier.gridstone_bricks_stairs": "Gridstone Brick Stairs",
//        "block.digitalfrontier.gridstone_bricks_slab": "Gridstone Brick Slab",
//        "block.digitalfrontier.gridstone_bricks_wall": "Gridstone Brick Wall",
//
//        "itemgroup.digitalfrontier": "Digital Frontier Items",
//
//        "entity.digitalfrontier.lightcycle": "Light Cycle",
//        "entity.digitalfrontier.program": "Program",
    }
}
