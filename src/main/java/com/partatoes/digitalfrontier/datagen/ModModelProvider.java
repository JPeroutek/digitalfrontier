package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Models;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.GRIDSTONE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CRACKED_GRIDSTONE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.PIXEL_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.LUMINANCE_ORE);
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
