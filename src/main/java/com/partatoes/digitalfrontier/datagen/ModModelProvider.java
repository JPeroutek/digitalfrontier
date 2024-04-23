package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.block.Blocks;
import net.minecraft.data.client.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.GRIDSTONE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CRACKED_GRIDSTONE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.GRIDSTONE_BRICKS_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.LIME_GRIDSTONE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.BLUE_GRIDSTONE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.RED_GRIDSTONE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.WHITE_GRIDSTONE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.ORANGE_GRIDSTONE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.PIXEL_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.LUMINANCE_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.BOOSTER_BLOCK);

        // Horrifying mess to create blockstates and models for Lamp
        blockStateModelGenerator.blockStateCollector.accept(
                VariantsBlockStateSupplier.create(ModBlocks.PIXEL_LAMP)
                        .coordinate(BlockStateModelGenerator.createBooleanModelMap(
                                Properties.LIT,
                                blockStateModelGenerator.createSubModel(ModBlocks.PIXEL_LAMP, "_lit", Models.CUBE_ALL, TextureMap::all),
                                TexturedModel.CUBE_ALL.upload(ModBlocks.PIXEL_LAMP, blockStateModelGenerator.modelCollector)))
        );

    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.BINARY_0, Models.GENERATED);
        itemModelGenerator.register(ModItems.BINARY_1, Models.GENERATED);
        itemModelGenerator.register(ModItems.BYTE, Models.GENERATED);
        itemModelGenerator.register(ModItems.COAL_DUST, Models.GENERATED);
        itemModelGenerator.register(ModItems.PIXEL_DUST, Models.GENERATED);
        itemModelGenerator.register(ModItems.LUMINESSENCE, Models.GENERATED);

    }
}
