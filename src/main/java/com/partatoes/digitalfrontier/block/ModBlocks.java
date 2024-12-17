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
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Identifier PIXEL_BLOCK_ID = Identifier.of(DigitalFrontier.MOD_ID, "pixel_block");
    public static final Block PIXEL_BLOCK = registerBlock(PIXEL_BLOCK_ID,
            new Block(AbstractBlock.Settings
                    .copy(Blocks.GLOWSTONE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, PIXEL_BLOCK_ID))));
    public static final Identifier PIXEL_LAMP_ID = Identifier.of(DigitalFrontier.MOD_ID, "pixel_lamp");
    public static final Block PIXEL_LAMP = registerBlock(PIXEL_LAMP_ID,
            new RedstoneLampBlock(AbstractBlock.Settings
                    .copy(Blocks.REDSTONE_LAMP)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, PIXEL_LAMP_ID))));

    public static final Identifier GRIDSTONE_BLOCK_ID = Identifier.of(DigitalFrontier.MOD_ID, "gridstone");
    public static final Block GRIDSTONE_BLOCK = registerBlock(GRIDSTONE_BLOCK_ID,
            new Block(AbstractBlock.Settings
                    .copy(Blocks.DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, GRIDSTONE_BLOCK_ID))));
    public static final Identifier CRACKED_GRIDSTONE_BLOCK_ID = Identifier.of(DigitalFrontier.MOD_ID, "cracked_gridstone");
    public static final Block CRACKED_GRIDSTONE_BLOCK = registerBlock(CRACKED_GRIDSTONE_BLOCK_ID,
            new Block(AbstractBlock.Settings
                    .copy(Blocks.COBBLED_DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, CRACKED_GRIDSTONE_BLOCK_ID))));
    public static final Identifier GRIDSTONE_BRICKS_BLOCK_ID = Identifier.of(DigitalFrontier.MOD_ID, "gridstone_bricks");
    public static final Block GRIDSTONE_BRICKS_BLOCK = registerBlock(GRIDSTONE_BRICKS_BLOCK_ID,
            new Block(AbstractBlock.Settings
                    .copy(Blocks.DEEPSLATE_BRICKS)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, GRIDSTONE_BRICKS_BLOCK_ID))));
    public static final Identifier LIME_GRIDSTONE_BLOCK_ID = Identifier.of(DigitalFrontier.MOD_ID, "lime_gridstone");
    public static final Block LIME_GRIDSTONE_BLOCK = registerBlock(LIME_GRIDSTONE_BLOCK_ID,
            new Block(AbstractBlock.Settings
                    .copy(Blocks.COBBLED_DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, LIME_GRIDSTONE_BLOCK_ID))));
    public static final Identifier BLUE_GRIDSTONE_BLOCK_ID = Identifier.of(DigitalFrontier.MOD_ID, "blue_gridstone");
    public static final Block BLUE_GRIDSTONE_BLOCK = registerBlock(BLUE_GRIDSTONE_BLOCK_ID,
            new Block(AbstractBlock.Settings
                    .copy(Blocks.COBBLED_DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, BLUE_GRIDSTONE_BLOCK_ID))));
    public static final Identifier RED_GRIDSTONE_BLOCK_ID = Identifier.of(DigitalFrontier.MOD_ID, "red_gridstone");
    public static final Block RED_GRIDSTONE_BLOCK = registerBlock(RED_GRIDSTONE_BLOCK_ID,
            new Block(AbstractBlock.Settings
                    .copy(Blocks.COBBLED_DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, RED_GRIDSTONE_BLOCK_ID))));
    public static final Identifier WHITE_GRIDSTONE_BLOCK_ID = Identifier.of(DigitalFrontier.MOD_ID, "white_gridstone");
    public static final Block WHITE_GRIDSTONE_BLOCK = registerBlock(WHITE_GRIDSTONE_BLOCK_ID,
            new Block(AbstractBlock.Settings
                    .copy(Blocks.COBBLED_DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, WHITE_GRIDSTONE_BLOCK_ID))));
    public static final Identifier ORANGE_GRIDSTONE_BLOCK_ID = Identifier.of(DigitalFrontier.MOD_ID, "orange_gridstone");
    public static final Block ORANGE_GRIDSTONE_BLOCK = registerBlock(ORANGE_GRIDSTONE_BLOCK_ID,
            new Block(AbstractBlock.Settings
                    .copy(Blocks.COBBLED_DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, ORANGE_GRIDSTONE_BLOCK_ID))));
    public static final Identifier LUMINANCE_ORE_ID = Identifier.of(DigitalFrontier.MOD_ID, "luminance_ore");
    public static final Block LUMINANCE_ORE = registerBlock(LUMINANCE_ORE_ID,
            new LuminanceOreBlock(AbstractBlock.Settings
                    .copy(Blocks.REDSTONE_ORE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, LUMINANCE_ORE_ID))));
    public static final Identifier BOOSTER_BLOCK_ID = Identifier.of(DigitalFrontier.MOD_ID, "booster");
    public static final Block BOOSTER_BLOCK = registerBlock(BOOSTER_BLOCK_ID,
            new BoosterBlock(AbstractBlock.Settings
                    .copy(Blocks.DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, BOOSTER_BLOCK_ID))));
    public static final Identifier GRIDSTONE_STAIRS_ID = Identifier.of(DigitalFrontier.MOD_ID, "gridstone_stairs");
    public static final Block GRIDSTONE_STAIRS = registerBlock(GRIDSTONE_STAIRS_ID,
            new StairsBlock(ModBlocks.GRIDSTONE_BLOCK.getDefaultState(),
                    AbstractBlock.Settings.copy(Blocks.DEEPSLATE)
                            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, GRIDSTONE_STAIRS_ID))));
    public static final Identifier GRIDSTONE_SLAB_ID = Identifier.of(DigitalFrontier.MOD_ID, "gridstone_slab");
    public static final Block GRIDSTONE_SLAB = registerBlock(GRIDSTONE_SLAB_ID,
            new SlabBlock(AbstractBlock.Settings
                    .copy(Blocks.DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, GRIDSTONE_SLAB_ID))));
    public static final Identifier GRIDSTONE_WALL_ID = Identifier.of(DigitalFrontier.MOD_ID, "gridstone_wall");
    public static final Block GRIDSTONE_WALL = registerBlock(GRIDSTONE_WALL_ID,
            new WallBlock(AbstractBlock.Settings
                    .copy(Blocks.DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, GRIDSTONE_WALL_ID))));
    public static final Identifier CRACKED_GRIDSTONE_STAIRS_ID = Identifier.of(DigitalFrontier.MOD_ID, "cracked_gridstone_stairs");
    public static final Block CRACKED_GRIDSTONE_STAIRS = registerBlock(CRACKED_GRIDSTONE_STAIRS_ID,
            new StairsBlock(ModBlocks.GRIDSTONE_BLOCK.getDefaultState(),
                    AbstractBlock.Settings.copy(Blocks.DEEPSLATE)
                            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, CRACKED_GRIDSTONE_STAIRS_ID))));
    public static final Identifier CRACKED_GRIDSTONE_SLAB_ID = Identifier.of(DigitalFrontier.MOD_ID, "cracked_gridstone_slab");
    public static final Block CRACKED_GRIDSTONE_SLAB = registerBlock(CRACKED_GRIDSTONE_SLAB_ID,
            new SlabBlock(AbstractBlock.Settings.copy(Blocks.DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, CRACKED_GRIDSTONE_SLAB_ID))));
    public static final Identifier CRACKED_GRIDSTONE_WALL_ID = Identifier.of(DigitalFrontier.MOD_ID, "cracked_gridstone_wall");
    public static final Block CRACKED_GRIDSTONE_WALL = registerBlock(CRACKED_GRIDSTONE_WALL_ID,
            new WallBlock(AbstractBlock.Settings
                    .copy(Blocks.DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, CRACKED_GRIDSTONE_WALL_ID))));
    public static final Identifier GRIDSTONE_BRICKS_STAIRS_ID = Identifier.of(DigitalFrontier.MOD_ID, "gridstone_bricks_stairs");
    public static final Block GRIDSTONE_BRICKS_STAIRS = registerBlock(GRIDSTONE_BRICKS_STAIRS_ID,
            new StairsBlock(ModBlocks.GRIDSTONE_BLOCK.getDefaultState(),
                    AbstractBlock.Settings.copy(Blocks.DEEPSLATE)
                            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, GRIDSTONE_BRICKS_STAIRS_ID))));
    public static final Identifier GRIDSTONE_BRICKS_SLAB_ID = Identifier.of(DigitalFrontier.MOD_ID, "gridstone_bricks_slab");
    public static final Block GRIDSTONE_BRICKS_SLAB = registerBlock(GRIDSTONE_BRICKS_SLAB_ID,
            new SlabBlock(AbstractBlock.Settings
                    .copy(Blocks.DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, GRIDSTONE_BRICKS_SLAB_ID))));
    public static final Identifier GRIDSTONE_BRICKS_WALL_ID = Identifier.of(DigitalFrontier.MOD_ID, "gridstone_bricks_wall");
    public static final Block GRIDSTONE_BRICKS_WALL = registerBlock(GRIDSTONE_BRICKS_WALL_ID,
            new WallBlock(AbstractBlock.Settings
                    .copy(Blocks.DEEPSLATE)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, GRIDSTONE_BRICKS_WALL_ID))));
    public static final Identifier GRIDSTONE_PRESSURE_PLATE_ID = Identifier.of(DigitalFrontier.MOD_ID, "gridstone_pressure_plate");
    public static final Block GRIDSTONE_PRESSURE_PLATE = registerBlock(GRIDSTONE_PRESSURE_PLATE_ID,
            new PressurePlateBlock(BlockSetType.STONE,
                    AbstractBlock.Settings.copy(Blocks.DEEPSLATE)
                            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, GRIDSTONE_PRESSURE_PLATE_ID))));
    public static final Identifier KEYBOARD_BLOCK_ID = Identifier.of(DigitalFrontier.MOD_ID, "keyboard");
    public static final Block KEYBOARD_BLOCK = registerBlock(KEYBOARD_BLOCK_ID,
            new KeyboardBlock(AbstractBlock.Settings
                    .copy(Blocks.COMPARATOR)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, KEYBOARD_BLOCK_ID))));

    private static Block registerBlock(Identifier name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, name, block);
    }

    private static void registerBlockItem(Identifier name, Block block) {
        Registry.register(Registries.ITEM,
            name,
            new BlockItem(block, new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, name))));
    }

    public static void registerModBlocks() {
        DigitalFrontier.LOGGER.info("Registering ModBlocks for " + DigitalFrontier.MOD_ID);
    }
}
