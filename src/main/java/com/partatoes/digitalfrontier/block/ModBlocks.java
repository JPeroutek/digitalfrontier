package com.partatoes.digitalfrontier.block;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.custom.*;
import net.minecraft.world.level.block.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ModBlocks {
    public static final Identifier PIXEL_BLOCK_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "pixel_block");
    public static final Block PIXEL_BLOCK = registerBlock(PIXEL_BLOCK_ID,
            new Block(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.GLOWSTONE)
                    .setId(ResourceKey.create(Registries.BLOCK, PIXEL_BLOCK_ID))));
    public static final Identifier PIXEL_LAMP_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "pixel_lamp");
    public static final Block PIXEL_LAMP = registerBlock(PIXEL_LAMP_ID,
            new RedstoneLampBlock(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.REDSTONE_LAMP)
                    .setId(ResourceKey.create(Registries.BLOCK, PIXEL_LAMP_ID))));

    public static final Identifier GRIDSTONE_BLOCK_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "gridstone");
    public static final Block GRIDSTONE_BLOCK = registerBlock(GRIDSTONE_BLOCK_ID,
            new Block(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.DEEPSLATE)
                    .setId(ResourceKey.create(Registries.BLOCK, GRIDSTONE_BLOCK_ID))));
    public static final Identifier CRACKED_GRIDSTONE_BLOCK_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "cracked_gridstone");
    public static final Block CRACKED_GRIDSTONE_BLOCK = registerBlock(CRACKED_GRIDSTONE_BLOCK_ID,
            new Block(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.COBBLED_DEEPSLATE)
                    .setId(ResourceKey.create(Registries.BLOCK, CRACKED_GRIDSTONE_BLOCK_ID))));
    public static final Identifier GRIDSTONE_BRICKS_BLOCK_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "gridstone_bricks");
    public static final Block GRIDSTONE_BRICKS_BLOCK = registerBlock(GRIDSTONE_BRICKS_BLOCK_ID,
            new Block(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.DEEPSLATE_BRICKS)
                    .setId(ResourceKey.create(Registries.BLOCK, GRIDSTONE_BRICKS_BLOCK_ID))));
    public static final Identifier LUMINANCE_ORE_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "luminance_ore");
    public static final Block LUMINANCE_ORE = registerBlock(LUMINANCE_ORE_ID,
            new LuminanceOreBlock(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.REDSTONE_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, LUMINANCE_ORE_ID))));
    public static final Identifier BOOSTER_BLOCK_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "booster");
    public static final Block BOOSTER_BLOCK = registerBlock(BOOSTER_BLOCK_ID,
            new BoosterBlock(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.DEEPSLATE)
                    .setId(ResourceKey.create(Registries.BLOCK, BOOSTER_BLOCK_ID))));
    public static final Identifier GRIDSTONE_STAIRS_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "gridstone_stairs");
    public static final Block GRIDSTONE_STAIRS = registerBlock(GRIDSTONE_STAIRS_ID,
            new StairBlock(ModBlocks.GRIDSTONE_BLOCK.defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                            .setId(ResourceKey.create(Registries.BLOCK, GRIDSTONE_STAIRS_ID))));
    public static final Identifier GRIDSTONE_SLAB_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "gridstone_slab");
    public static final Block GRIDSTONE_SLAB = registerBlock(GRIDSTONE_SLAB_ID,
            new SlabBlock(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.DEEPSLATE)
                    .setId(ResourceKey.create(Registries.BLOCK, GRIDSTONE_SLAB_ID))));
    public static final Identifier GRIDSTONE_WALL_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "gridstone_wall");
    public static final Block GRIDSTONE_WALL = registerBlock(GRIDSTONE_WALL_ID,
            new WallBlock(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.DEEPSLATE)
                    .setId(ResourceKey.create(Registries.BLOCK, GRIDSTONE_WALL_ID))));
    public static final Identifier CRACKED_GRIDSTONE_STAIRS_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "cracked_gridstone_stairs");
    public static final Block CRACKED_GRIDSTONE_STAIRS = registerBlock(CRACKED_GRIDSTONE_STAIRS_ID,
            new StairBlock(ModBlocks.CRACKED_GRIDSTONE_BLOCK.defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                            .setId(ResourceKey.create(Registries.BLOCK, CRACKED_GRIDSTONE_STAIRS_ID))));
    public static final Identifier CRACKED_GRIDSTONE_SLAB_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "cracked_gridstone_slab");
    public static final Block CRACKED_GRIDSTONE_SLAB = registerBlock(CRACKED_GRIDSTONE_SLAB_ID,
            new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                    .setId(ResourceKey.create(Registries.BLOCK, CRACKED_GRIDSTONE_SLAB_ID))));
    public static final Identifier CRACKED_GRIDSTONE_WALL_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "cracked_gridstone_wall");
    public static final Block CRACKED_GRIDSTONE_WALL = registerBlock(CRACKED_GRIDSTONE_WALL_ID,
            new WallBlock(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.DEEPSLATE)
                    .setId(ResourceKey.create(Registries.BLOCK, CRACKED_GRIDSTONE_WALL_ID))));
    public static final Identifier GRIDSTONE_BRICKS_STAIRS_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "gridstone_bricks_stairs");
    public static final Block GRIDSTONE_BRICKS_STAIRS = registerBlock(GRIDSTONE_BRICKS_STAIRS_ID,
            new StairBlock(ModBlocks.GRIDSTONE_BRICKS_BLOCK.defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                            .setId(ResourceKey.create(Registries.BLOCK, GRIDSTONE_BRICKS_STAIRS_ID))));
    public static final Identifier GRIDSTONE_BRICKS_SLAB_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "gridstone_bricks_slab");
    public static final Block GRIDSTONE_BRICKS_SLAB = registerBlock(GRIDSTONE_BRICKS_SLAB_ID,
            new SlabBlock(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.DEEPSLATE)
                    .setId(ResourceKey.create(Registries.BLOCK, GRIDSTONE_BRICKS_SLAB_ID))));
    public static final Identifier GRIDSTONE_BRICKS_WALL_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "gridstone_bricks_wall");
    public static final Block GRIDSTONE_BRICKS_WALL = registerBlock(GRIDSTONE_BRICKS_WALL_ID,
            new WallBlock(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.DEEPSLATE)
                    .setId(ResourceKey.create(Registries.BLOCK, GRIDSTONE_BRICKS_WALL_ID))));
    public static final Identifier GRIDSTONE_PRESSURE_PLATE_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "gridstone_pressure_plate");
    public static final Block GRIDSTONE_PRESSURE_PLATE = registerBlock(GRIDSTONE_PRESSURE_PLATE_ID,
            new PressurePlateBlock(BlockSetType.STONE,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                            .setId(ResourceKey.create(Registries.BLOCK, GRIDSTONE_PRESSURE_PLATE_ID))));
    public static final Identifier KEYBOARD_BLOCK_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "keyboard");
    public static final Block KEYBOARD_BLOCK = registerBlock(KEYBOARD_BLOCK_ID,
            new KeyboardBlock(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.COMPARATOR)
                    .setId(ResourceKey.create(Registries.BLOCK, KEYBOARD_BLOCK_ID))));

    // Light the glowing lines give off: enough to see the streets by, with the dark between them still dim
    private static final int PATTERN_LUMINANCE = 6;

    // More patterns TBD.  Spiral, 90 deg left, straight (all with varying thicknesses and entry/exits
    public static final String[] GRIDSTONE_PATTERNS = new String[] {
            "full",
            "half",
            "quarter",
            "3quarter",
            "border_1",
            "border_1_inverted",
            "border_2",
            "border_2_inverted",
            "border_3",
            "border_3_inverted",
            "border_4",
            "border_4_inverted",
            "border_5",
            "border_5_inverted",
            "border_6",
            "border_6_inverted",
            "border_7",
            "border_7_inverted",
            "checker_2x2",
            "checker_4x4",
            "checker_8x8",
            "vintage_circles", // The old pattern from the original mod
            "center_stripe",
            "center_stripe_thin",
            "cross",
            "sharp_t",
            "sharp_corner",
            "curved_t",
            "curved_corner",
            "curved_corner_thin",
            "thick_ring",
            "thin_ring",
            "thick_target",
            "thin_target",
            "yang",
            "even_split_left_right",
            "even_split_left_straight",
            "even_split_right_straight",
            "partial_split_left_straight",
            "partial_split_right_straight",
            "shrink_4_to_2",
            "exchanger",
            "single_entry_circle",
    };

    public static final Map<String, Map<DyeColor, Block>> GRIDSTONE_PATTERNS_AND_COLORS = Arrays
            .stream(GRIDSTONE_PATTERNS)
            .collect(Collectors.toMap(
                    patternName -> patternName,
                    patternName -> {
                        return Arrays.stream(DyeColor.values())
                                .collect(Collectors.toMap(
                                        color -> color,
                                        color -> {
                                            Identifier id = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, String.format("gridstone_pattern_%s_%s", patternName, color.getSerializedName().toLowerCase()));
                                            return registerBlock(id, new PatternBlock(
                                                    BlockBehaviour.Properties.ofFullCopy(ModBlocks.GRIDSTONE_BLOCK)
                                                            .lightLevel(state -> PATTERN_LUMINANCE)
                                                            .setId(ResourceKey.create(Registries.BLOCK, id))));
                                        }
                                ));
                    }));

    public static final Identifier LIGHT_BARRIER_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "light_barrier");
    public static final Block LIGHT_BARRIER = registerBlock(LIGHT_BARRIER_ID,
            new LightBarrierBlock(BlockBehaviour.Properties
                    .of()
                    .setId(ResourceKey.create(Registries.BLOCK, LIGHT_BARRIER_ID))
                    .strength(-1f, 3600000f) // Hardness -1, unbreakable, same explosive resistance as bedrock
                    .noOcclusion()
                    .lightLevel((state) -> 4)
                    .emissiveRendering(state -> true) // Should always create light, regardless of state.  This may not be needed?
    ));

    public static final Identifier LIGHT_BARRIER_EMITTER_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "light_barrier_emitter");
    public static final Block LIGHT_BARRIER_EMITTER = registerBlock(LIGHT_BARRIER_EMITTER_ID,
            new LightBarrierEmitterBlock(BlockBehaviour.Properties
                    .of()
                    .setId(ResourceKey.create(Registries.BLOCK, LIGHT_BARRIER_EMITTER_ID))
                    .strength(1.5f)
//                    .nonOpaque()
                    .lightLevel((state) -> state.getValue(LightBarrierEmitterBlock.POWERED) ? 4 : 0)
                    .emissiveRendering(state -> state.getValue(LightBarrierEmitterBlock.POWERED)) // Should always create light, regardless of state.  This may not be needed?
            ));

    // Marks where players arrive in a grid terminal template; the generator looks for it when loading parts
    public static final Identifier ARRIVAL_PAD_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "arrival_pad");
    public static final Block ARRIVAL_PAD = registerBlock(ARRIVAL_PAD_ID,
            new Block(BlockBehaviour.Properties
                    .ofFullCopy(ModBlocks.GRIDSTONE_BLOCK)
                    .lightLevel(state -> 12)
                    .setId(ResourceKey.create(Registries.BLOCK, ARRIVAL_PAD_ID))));
    public static final Identifier DIGITIZING_LASER_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "digitizing_laser");
    public static final Block DIGITIZING_LASER = registerBlock(DIGITIZING_LASER_ID,
            new DigitizingLaserBlock(false, BlockBehaviour.Properties
                    .ofFullCopy(Blocks.IRON_BLOCK)
                    .lightLevel(state -> 7)
                    .setId(ResourceKey.create(Registries.BLOCK, DIGITIZING_LASER_ID))));
    // The upgraded laser: can also tune into grids other than the default one
    public static final Identifier GRID_TUNING_LASER_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "grid_tuning_laser");
    public static final Block GRID_TUNING_LASER = registerBlock(GRID_TUNING_LASER_ID,
            new DigitizingLaserBlock(true, BlockBehaviour.Properties
                    .ofFullCopy(Blocks.NETHERITE_BLOCK)
                    .lightLevel(state -> 10)
                    .setId(ResourceKey.create(Registries.BLOCK, GRID_TUNING_LASER_ID))));
    // Corruption-only blocks: generated where a grid is corrupted, and they don't drop anything
    public static final Identifier STATIC_BLOCK_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "static_block");
    public static final Block STATIC_BLOCK = registerBlock(STATIC_BLOCK_ID,
            new Block(BlockBehaviour.Properties
                    .ofFullCopy(ModBlocks.GRIDSTONE_BLOCK)
                    .lightLevel(state -> 4)
                    .noLootTable()
                    .setId(ResourceKey.create(Registries.BLOCK, STATIC_BLOCK_ID))));
    public static final Map<DyeColor, Block> GLITCHED_GRIDSTONE = Arrays.stream(DyeColor.values())
            .collect(Collectors.toMap(color -> color, color -> {
                Identifier id = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "glitched_gridstone_" + color.getSerializedName());
                return registerBlock(id, new Block(BlockBehaviour.Properties
                        .ofFullCopy(ModBlocks.GRIDSTONE_BLOCK)
                        .lightLevel(state -> PATTERN_LUMINANCE)
                        .noLootTable()
                        .setId(ResourceKey.create(Registries.BLOCK, id))));
            }, (a, b) -> a, () -> new EnumMap<>(DyeColor.class)));

    public static final Identifier STRONGHOLD_UPLINK_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "stronghold_uplink");
    public static final Block STRONGHOLD_UPLINK = registerBlock(STRONGHOLD_UPLINK_ID,
            new StrongholdUplinkBlock(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.OBSIDIAN)
                    .lightLevel(state -> state.getValue(StrongholdUplinkBlock.POWERED) ? 12 : 5)
                    .setId(ResourceKey.create(Registries.BLOCK, STRONGHOLD_UPLINK_ID))));

    // The way out of a grid; only generated at Portal sites, so it can't be broken or crafted
    public static final Identifier GRID_PORTAL_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "grid_portal");
    public static final Block GRID_PORTAL = registerBlock(GRID_PORTAL_ID,
            new GridPortalBlock(BlockBehaviour.Properties
                    .of()
                    .noCollision()
                    .noOcclusion()
                    .strength(-1f, 3600000f)
                    .noLootTable()
                    .lightLevel(state -> 15)
                    .isSuffocating(Blocks::never)
                    .isViewBlocking((state, world, pos, box) -> false)
                    .setId(ResourceKey.create(Registries.BLOCK, GRID_PORTAL_ID))));


    private static Block registerBlock(Identifier name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(BuiltInRegistries.BLOCK, name, block);
    }

    private static void registerBlockItem(Identifier name, Block block) {
        Registry.register(BuiltInRegistries.ITEM,
            name,
            new BlockItem(block, new Item.Properties()
                    .useBlockDescriptionPrefix()
                    .setId(ResourceKey.create(Registries.ITEM, name))));
    }

    public static void registerModBlocks() {
        DigitalFrontier.LOGGER.info("Registering ModBlocks for " + DigitalFrontier.MOD_ID);
    }
}
