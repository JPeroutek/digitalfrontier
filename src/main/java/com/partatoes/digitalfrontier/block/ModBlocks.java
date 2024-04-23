package com.partatoes.digitalfrontier.block;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.custom.LuminanceOreBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.RedstoneOreBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block PIXEL_BLOCK = registerBlock("pixel_block",
            new Block(FabricBlockSettings.copyOf(Blocks.GLOWSTONE)));
    public static final Block PIXEL_LAMP = registerBlock("pixel_lamp",
            new RedstoneLampBlock(FabricBlockSettings.copyOf(Blocks.REDSTONE_LAMP)));
    public static final Block GRIDSTONE_BLOCK = registerBlock("gridstone",
            new Block(FabricBlockSettings.copyOf(Blocks.DEEPSLATE)));
    public static final Block CRACKED_GRIDSTONE_BLOCK = registerBlock("cracked_gridstone",
            new Block(FabricBlockSettings.copyOf(Blocks.COBBLED_DEEPSLATE)));
    public static final Block GRIDSTONE_BRICKS_BLOCK = registerBlock("gridstone_bricks",
            new Block(FabricBlockSettings.copyOf(Blocks.DEEPSLATE_BRICKS)));
    public static final Block LIME_GRIDSTONE_BLOCK = registerBlock("lime_gridstone",
            new Block(FabricBlockSettings.copyOf(Blocks.COBBLED_DEEPSLATE)));
    public static final Block BLUE_GRIDSTONE_BLOCK = registerBlock("blue_gridstone",
            new Block(FabricBlockSettings.copyOf(Blocks.COBBLED_DEEPSLATE)));
    public static final Block RED_GRIDSTONE_BLOCK = registerBlock("red_gridstone",
            new Block(FabricBlockSettings.copyOf(Blocks.COBBLED_DEEPSLATE)));
    public static final Block WHITE_GRIDSTONE_BLOCK = registerBlock("white_gridstone",
            new Block(FabricBlockSettings.copyOf(Blocks.COBBLED_DEEPSLATE)));
    public static final Block ORANGE_GRIDSTONE_BLOCK = registerBlock("orange_gridstone",
            new Block(FabricBlockSettings.copyOf(Blocks.COBBLED_DEEPSLATE)));

    public static final Block LUMINANCE_ORE = registerBlock("luminance_ore",
            new LuminanceOreBlock(FabricBlockSettings.copyOf(Blocks.REDSTONE_ORE)));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, new Identifier(DigitalFrontier.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(Registries.ITEM,
                new Identifier(DigitalFrontier.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
    }

    public static void registerModBlocks() {
        DigitalFrontier.LOGGER.info("Registering ModBlocks for " + DigitalFrontier.MOD_ID);
    }
}
