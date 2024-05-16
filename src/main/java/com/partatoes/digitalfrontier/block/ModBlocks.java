package com.partatoes.digitalfrontier.block;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.custom.BoosterBlock;
import com.partatoes.digitalfrontier.block.custom.KeyboardBlock;
import com.partatoes.digitalfrontier.block.custom.LuminanceOreBlock;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block PIXEL_BLOCK = registerBlock("pixel_block",
            new Block(AbstractBlock.Settings.copy(Blocks.GLOWSTONE)));
    public static final Block PIXEL_LAMP = registerBlock("pixel_lamp",
            new RedstoneLampBlock(AbstractBlock.Settings.copy(Blocks.REDSTONE_LAMP)));
    public static final Block GRIDSTONE_BLOCK = registerBlock("gridstone",
            new Block(AbstractBlock.Settings.copy(Blocks.DEEPSLATE)));
    public static final Block CRACKED_GRIDSTONE_BLOCK = registerBlock("cracked_gridstone",
            new Block(AbstractBlock.Settings.copy(Blocks.COBBLED_DEEPSLATE)));
    public static final Block GRIDSTONE_BRICKS_BLOCK = registerBlock("gridstone_bricks",
            new Block(AbstractBlock.Settings.copy(Blocks.DEEPSLATE_BRICKS)));
    public static final Block LIME_GRIDSTONE_BLOCK = registerBlock("lime_gridstone",
            new Block(AbstractBlock.Settings.copy(Blocks.COBBLED_DEEPSLATE)));
    public static final Block BLUE_GRIDSTONE_BLOCK = registerBlock("blue_gridstone",
            new Block(AbstractBlock.Settings.copy(Blocks.COBBLED_DEEPSLATE)));
    public static final Block RED_GRIDSTONE_BLOCK = registerBlock("red_gridstone",
            new Block(AbstractBlock.Settings.copy(Blocks.COBBLED_DEEPSLATE)));
    public static final Block WHITE_GRIDSTONE_BLOCK = registerBlock("white_gridstone",
            new Block(AbstractBlock.Settings.copy(Blocks.COBBLED_DEEPSLATE)));
    public static final Block ORANGE_GRIDSTONE_BLOCK = registerBlock("orange_gridstone",
            new Block(AbstractBlock.Settings.copy(Blocks.COBBLED_DEEPSLATE)));

    public static final Block LUMINANCE_ORE = registerBlock("luminance_ore",
            new LuminanceOreBlock(AbstractBlock.Settings.copy(Blocks.REDSTONE_ORE)));
    public static final Block BOOSTER_BLOCK = registerBlock("booster",
            new BoosterBlock(AbstractBlock.Settings.copy(Blocks.DEEPSLATE)));

    public static final Block GRIDSTONE_STAIRS = registerBlock("gridstone_stairs",
            new StairsBlock(ModBlocks.GRIDSTONE_BLOCK.getDefaultState(), AbstractBlock.Settings.copy(Blocks.DEEPSLATE)));
    public static final Block GRIDSTONE_SLAB = registerBlock("gridstone_slab",
            new SlabBlock(AbstractBlock.Settings.copy(Blocks.DEEPSLATE)));
    public static final Block GRIDSTONE_WALL = registerBlock("gridstone_wall",
            new WallBlock(AbstractBlock.Settings.copy(Blocks.DEEPSLATE)));
    public static final Block GRIDSTONE_PRESSURE_PLATE = registerBlock("gridstone_pressure_plate",
            new PressurePlateBlock(BlockSetType.STONE, AbstractBlock.Settings.copy(Blocks.DEEPSLATE)));
    public static final Block KEYBOARD_BLOCK = registerBlock("keyboard",
            new KeyboardBlock(AbstractBlock.Settings.copy(Blocks.COMPARATOR)));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, new Identifier(DigitalFrontier.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM,
            new Identifier(DigitalFrontier.MOD_ID, name),
            new BlockItem(block, new Item.Settings()));
    }

    public static void registerModBlocks() {
        DigitalFrontier.LOGGER.info("Registering ModBlocks for " + DigitalFrontier.MOD_ID);
    }
}
