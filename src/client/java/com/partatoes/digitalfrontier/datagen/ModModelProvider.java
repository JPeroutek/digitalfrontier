package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.block.custom.PatternBlock;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.Block;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.client.data.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModModelProvider extends FabricModelProvider {
    // A cube with a second, full-bright overlay layer: the vanilla way to get per-pixel emissive textures
    private static final TextureKey GLOW = TextureKey.of("glow");
    private static final Model EMISSIVE_CUBE_ALL = new Model(
            Optional.of(Identifier.of(DigitalFrontier.MOD_ID, "block/emissive_cube_all")), Optional.empty(), TextureKey.ALL, GLOW);

    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        BlockStateModelGenerator.BlockTexturePool gridstonePool = blockStateModelGenerator.registerCubeAllModelTexturePool(ModBlocks.GRIDSTONE_BLOCK);
        BlockStateModelGenerator.BlockTexturePool crackedGridstonePool = blockStateModelGenerator.registerCubeAllModelTexturePool(ModBlocks.CRACKED_GRIDSTONE_BLOCK);
        BlockStateModelGenerator.BlockTexturePool gridstoneBricksPool = blockStateModelGenerator.registerCubeAllModelTexturePool(ModBlocks.GRIDSTONE_BRICKS_BLOCK);

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.PIXEL_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.LUMINANCE_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.BOOSTER_BLOCK);

        gridstonePool.wall(ModBlocks.GRIDSTONE_WALL);
        gridstonePool.stairs(ModBlocks.GRIDSTONE_STAIRS);
        gridstonePool.slab(ModBlocks.GRIDSTONE_SLAB);
        gridstonePool.pressurePlate(ModBlocks.GRIDSTONE_PRESSURE_PLATE);

        crackedGridstonePool.wall(ModBlocks.CRACKED_GRIDSTONE_WALL);
        crackedGridstonePool.stairs(ModBlocks.CRACKED_GRIDSTONE_STAIRS);
        crackedGridstonePool.slab(ModBlocks.CRACKED_GRIDSTONE_SLAB);

        gridstoneBricksPool.wall(ModBlocks.GRIDSTONE_BRICKS_WALL);
        gridstoneBricksPool.stairs(ModBlocks.GRIDSTONE_BRICKS_STAIRS);
        gridstoneBricksPool.slab(ModBlocks.GRIDSTONE_BRICKS_SLAB);

        // Horrifying mess to create blockstates and models for Lamp
        blockStateModelGenerator.blockStateCollector.accept(
                VariantsBlockStateSupplier.create(ModBlocks.PIXEL_LAMP)
                        .coordinate(BlockStateModelGenerator.createBooleanModelMap(
                                Properties.LIT,
                                blockStateModelGenerator.createSubModel(ModBlocks.PIXEL_LAMP, "_lit", Models.CUBE_ALL, TextureMap::all),
                                TexturedModel.CUBE_ALL.upload(ModBlocks.PIXEL_LAMP, blockStateModelGenerator.modelCollector)))
        );

        blockStateModelGenerator.registerNorthDefaultHorizontalRotation(ModBlocks.KEYBOARD_BLOCK);

        // This will need to be replaced later to support directionality and stuff
        ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS
                .forEach((patternName, dyeBlockMap) -> {
                    dyeBlockMap.forEach((color, block) -> registerPattern(blockStateModelGenerator, block));
                });

        
        // Placeholder art borrowed from the pattern blocks until these get their own textures
        Block padPattern = ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get("thick_target").get(DyeColor.CYAN);
        registerEmissiveCubeAll(blockStateModelGenerator, ModBlocks.ARRIVAL_PAD, padPattern);
        registerEmissiveCubeAll(blockStateModelGenerator, ModBlocks.GRID_PORTAL, ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get("full").get(DyeColor.WHITE));
        registerAnimated(blockStateModelGenerator, ModBlocks.STATIC_BLOCK, 1);
        registerEmissiveCubeAll(blockStateModelGenerator, ModBlocks.STRONGHOLD_UPLINK, ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get("exchanger").get(DyeColor.RED));
        ModBlocks.GLITCHED_GRIDSTONE.values().forEach(block -> registerAnimated(blockStateModelGenerator, block, ModTextureProvider.GLITCH_TEMPLATES.length));
        Identifier laserModel = Models.CUBE_BOTTOM_TOP.upload(ModBlocks.DIGITIZING_LASER, new TextureMap()
                .put(TextureKey.TOP, TextureMap.getId(ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get("thick_ring").get(DyeColor.CYAN)))
                .put(TextureKey.SIDE, TextureMap.getId(ModBlocks.GRIDSTONE_BRICKS_BLOCK))
                .put(TextureKey.BOTTOM, TextureMap.getId(ModBlocks.GRIDSTONE_BLOCK)), blockStateModelGenerator.modelCollector);
        blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createSingletonBlockState(ModBlocks.DIGITIZING_LASER, laserModel));
        Identifier tuningLaserModel = Models.CUBE_BOTTOM_TOP.upload(ModBlocks.GRID_TUNING_LASER, new TextureMap()
                .put(TextureKey.TOP, TextureMap.getId(ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get("thick_ring").get(DyeColor.ORANGE)))
                .put(TextureKey.SIDE, TextureMap.getId(ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get("border_1").get(DyeColor.CYAN)))
                .put(TextureKey.BOTTOM, TextureMap.getId(ModBlocks.GRIDSTONE_BLOCK)), blockStateModelGenerator.modelCollector);
        blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createSingletonBlockState(ModBlocks.GRID_TUNING_LASER, tuningLaserModel));

        blockStateModelGenerator.blockStateCollector.accept(
                createFacingFacePoweredBlockStates(ModBlocks.LIGHT_BARRIER_EMITTER, ModelIds.getBlockModelId(ModBlocks.LIGHT_BARRIER_EMITTER), ModelIds.getBlockSubModelId(ModBlocks.LIGHT_BARRIER_EMITTER, "_powered"))
        );
        blockStateModelGenerator.blockStateCollector.accept(
                createFacingFaceBlockStates(ModBlocks.LIGHT_BARRIER, ModelIds.getBlockModelId(ModBlocks.LIGHT_BARRIER))
        );
    }

    // An emissive cube per flicker value: steady, then glows that drop out now and then (<name>_glow_flicker_<n>),
    // each flickering one in several phases so neighbouring blocks don't flicker together
    private static void registerPattern(BlockStateModelGenerator blockStateModelGenerator, Block block) {
        BlockStateVariantMap.SingleProperty<Integer> variants = BlockStateVariantMap.create(PatternBlock.FLICKER);
        for (int flicker : PatternBlock.FLICKER.getValues()) {
            if (flicker == 0) {
                TextureMap textures = new TextureMap()
                        .put(TextureKey.ALL, TextureMap.getId(block))
                        .put(GLOW, TextureMap.getSubId(block, "_glow"));
                Identifier modelId = EMISSIVE_CUBE_ALL.upload(block, textures, blockStateModelGenerator.modelCollector);
                variants.register(flicker, BlockStateVariant.create().put(VariantSettings.MODEL, modelId));
                continue;
            }
            List<BlockStateVariant> phases = new ArrayList<>();
            for (int phase = 0; phase < ModTextureProvider.FLICKER_PHASES; phase++) {
                String suffix = "_flicker_" + flicker + ModTextureProvider.phaseSuffix(phase);
                TextureMap textures = new TextureMap()
                        .put(TextureKey.ALL, TextureMap.getId(block))
                        .put(GLOW, TextureMap.getSubId(block, "_glow" + suffix));
                Identifier modelId = EMISSIVE_CUBE_ALL.upload(ModelIds.getBlockSubModelId(block, suffix), textures, blockStateModelGenerator.modelCollector);
                phases.add(BlockStateVariant.create().put(VariantSettings.MODEL, modelId));
            }
            variants.register(flicker, phases);
        }
        blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(block).coordinate(variants));
    }

    // Turns a cube can be shown at, so every face of an animated block can come out at a different angle
    private static final VariantSettings.Rotation[][] TURNS = {
            {VariantSettings.Rotation.R0, VariantSettings.Rotation.R0},
            {VariantSettings.Rotation.R90, VariantSettings.Rotation.R0},
            {VariantSettings.Rotation.R180, VariantSettings.Rotation.R0},
            {VariantSettings.Rotation.R270, VariantSettings.Rotation.R0},
            {VariantSettings.Rotation.R90, VariantSettings.Rotation.R90},
            {VariantSettings.Rotation.R270, VariantSettings.Rotation.R90},
    };

    /**
     * An animated emissive cube drawn from several textures (<name><template suffix><phase suffix>, see
     * ModTextureProvider), each at several turns; the game picks one per block by position, so a patch of them
     * neither animates in step nor repeats one look.
     */
    private static void registerAnimated(BlockStateModelGenerator blockStateModelGenerator, Block block, int templates) {
        List<BlockStateVariant> variants = new ArrayList<>();
        for (int template = 0; template < templates; template++) {
            for (int phase = 0; phase < ModTextureProvider.ANIMATION_PHASES; phase++) {
                String suffix = ModTextureProvider.templateSuffix(template) + ModTextureProvider.phaseSuffix(phase);
                TextureMap textures = new TextureMap()
                        .put(TextureKey.ALL, TextureMap.getSubId(block, suffix))
                        .put(GLOW, TextureMap.getSubId(block, suffix + "_glow"));
                Identifier modelId = suffix.isEmpty()
                        ? EMISSIVE_CUBE_ALL.upload(block, textures, blockStateModelGenerator.modelCollector)
                        : EMISSIVE_CUBE_ALL.upload(ModelIds.getBlockSubModelId(block, suffix), textures, blockStateModelGenerator.modelCollector);
                for (VariantSettings.Rotation[] turn : TURNS) {
                    variants.add(BlockStateVariant.create()
                            .put(VariantSettings.MODEL, modelId)
                            .put(VariantSettings.X, turn[0])
                            .put(VariantSettings.Y, turn[1]));
                }
            }
        }
        blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(block, variants.toArray(BlockStateVariant[]::new)));
    }

    // Expects the block's texture plus a <name>_glow texture holding only the pixels that should glow
    private static void registerEmissiveCubeAll(BlockStateModelGenerator blockStateModelGenerator, Block block) {
        registerEmissiveCubeAll(blockStateModelGenerator, block, block);
    }

    private static void registerEmissiveCubeAll(BlockStateModelGenerator blockStateModelGenerator, Block block, Block textureSource) {
        TextureMap textures = new TextureMap()
                .put(TextureKey.ALL, TextureMap.getId(textureSource))
                .put(GLOW, TextureMap.getSubId(textureSource, "_glow"));
        Identifier modelId = EMISSIVE_CUBE_ALL.upload(block, textures, blockStateModelGenerator.modelCollector);
        blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createSingletonBlockState(block, modelId));
    }

    public static BlockStateSupplier createFacingFacePoweredBlockStates(Block block, Identifier regularModelId, Identifier pressedModelId) {
        return VariantsBlockStateSupplier.create(block)
                .coordinate(
                        BlockStateVariantMap.create(Properties.POWERED)
                                .register(false, BlockStateVariant.create().put(VariantSettings.MODEL, regularModelId))
                                .register(true, BlockStateVariant.create().put(VariantSettings.MODEL, pressedModelId))
                )
                .coordinate(
                        BlockStateVariantMap.create(Properties.BLOCK_FACE, Properties.HORIZONTAL_FACING)
                                .register(BlockFace.FLOOR, Direction.EAST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R90))
                                .register(BlockFace.FLOOR, Direction.WEST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R270))
                                .register(BlockFace.FLOOR, Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R180))
                                .register(BlockFace.FLOOR, Direction.NORTH, BlockStateVariant.create())
                                .register(
                                        BlockFace.WALL,
                                        Direction.EAST,
                                        BlockStateVariant.create()
                                                .put(VariantSettings.Y, VariantSettings.Rotation.R90)
                                                .put(VariantSettings.X, VariantSettings.Rotation.R90)
//                                                .put(VariantSettings.UVLOCK, true)
                                )
                                .register(
                                        BlockFace.WALL,
                                        Direction.WEST,
                                        BlockStateVariant.create()
                                                .put(VariantSettings.Y, VariantSettings.Rotation.R270)
                                                .put(VariantSettings.X, VariantSettings.Rotation.R90)
//                                                .put(VariantSettings.UVLOCK, true)
                                )
                                .register(
                                        BlockFace.WALL,
                                        Direction.SOUTH,
                                        BlockStateVariant.create()
                                                .put(VariantSettings.Y, VariantSettings.Rotation.R180)
                                                .put(VariantSettings.X, VariantSettings.Rotation.R90)
//                                                .put(VariantSettings.UVLOCK, true)
                                )
                                .register(
                                        BlockFace.WALL, Direction.NORTH, BlockStateVariant.create()
                                                .put(VariantSettings.X, VariantSettings.Rotation.R90)
//                                                .put(VariantSettings.UVLOCK, true)
                                )
                                .register(
                                        BlockFace.CEILING,
                                        Direction.EAST,
                                        BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R270).put(VariantSettings.X, VariantSettings.Rotation.R180)
                                )
                                .register(
                                        BlockFace.CEILING,
                                        Direction.WEST,
                                        BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R90).put(VariantSettings.X, VariantSettings.Rotation.R180)
                                )
                                .register(BlockFace.CEILING, Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R180))
                                .register(
                                        BlockFace.CEILING,
                                        Direction.NORTH,
                                        BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R180).put(VariantSettings.X, VariantSettings.Rotation.R180)
                                )
                );
    }
    public static BlockStateSupplier createFacingFaceBlockStates(Block block, Identifier regularModelId) {
        return VariantsBlockStateSupplier.create(block, BlockStateVariant.create().put(VariantSettings.MODEL, regularModelId))
//                .coordinate(
//                        BlockStateVariantMap.create(Properties.POWERED)
//                                .register(false, BlockStateVariant.create().put(VariantSettings.MODEL, regularModelId))
//                                .register(true, BlockStateVariant.create().put(VariantSettings.MODEL, pressedModelId))
//                )
                .coordinate(
                        BlockStateVariantMap.create(Properties.BLOCK_FACE, Properties.HORIZONTAL_FACING)
                                .register(BlockFace.FLOOR, Direction.EAST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R90))
                                .register(BlockFace.FLOOR, Direction.WEST, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R270))
                                .register(BlockFace.FLOOR, Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R180))
                                .register(BlockFace.FLOOR, Direction.NORTH, BlockStateVariant.create())
                                .register(
                                        BlockFace.WALL,
                                        Direction.EAST,
                                        BlockStateVariant.create()
                                                .put(VariantSettings.Y, VariantSettings.Rotation.R90)
                                                .put(VariantSettings.X, VariantSettings.Rotation.R90)
//                                                .put(VariantSettings.UVLOCK, true)
                                )
                                .register(
                                        BlockFace.WALL,
                                        Direction.WEST,
                                        BlockStateVariant.create()
                                                .put(VariantSettings.Y, VariantSettings.Rotation.R270)
                                                .put(VariantSettings.X, VariantSettings.Rotation.R90)
//                                                .put(VariantSettings.UVLOCK, true)
                                )
                                .register(
                                        BlockFace.WALL,
                                        Direction.SOUTH,
                                        BlockStateVariant.create()
                                                .put(VariantSettings.Y, VariantSettings.Rotation.R180)
                                                .put(VariantSettings.X, VariantSettings.Rotation.R90)
//                                                .put(VariantSettings.UVLOCK, true)
                                )
                                .register(
                                        BlockFace.WALL, Direction.NORTH, BlockStateVariant.create()
                                                .put(VariantSettings.X, VariantSettings.Rotation.R90)
//                                                .put(VariantSettings.UVLOCK, true)
                                )
                                .register(
                                        BlockFace.CEILING,
                                        Direction.EAST,
                                        BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R270).put(VariantSettings.X, VariantSettings.Rotation.R180)
                                )
                                .register(
                                        BlockFace.CEILING,
                                        Direction.WEST,
                                        BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R90).put(VariantSettings.X, VariantSettings.Rotation.R180)
                                )
                                .register(BlockFace.CEILING, Direction.SOUTH, BlockStateVariant.create().put(VariantSettings.X, VariantSettings.Rotation.R180))
                                .register(
                                        BlockFace.CEILING,
                                        Direction.NORTH,
                                        BlockStateVariant.create().put(VariantSettings.Y, VariantSettings.Rotation.R180).put(VariantSettings.X, VariantSettings.Rotation.R180)
                                )
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
        itemModelGenerator.register(ModItems.LIGHTCYCLE_BATON, Models.GENERATED);

        // Placeholder: borrow the vanilla snowball model until it has its own art
        itemModelGenerator.output.accept(ModItems.DIMENSION_TELEPORTER, ItemModels.basic(Identifier.ofVanilla("item/snowball")));
        // Placeholder: the vanilla compass until the tuner has its own art
        itemModelGenerator.output.accept(ModItems.LASER_TUNER, ItemModels.basic(Identifier.ofVanilla("item/compass_16")));
        itemModelGenerator.output.accept(ModItems.ESCAPE_KEY, ItemModels.basic(Identifier.ofVanilla("item/trial_key")));
        itemModelGenerator.output.accept(ModItems.MEMORY_PAGING, ItemModels.basic(Identifier.ofVanilla("item/recovery_compass_16")));
        itemModelGenerator.output.accept(ModItems.CORE_DUMP, ItemModels.basic(Identifier.ofVanilla("item/heart_of_the_sea")));

    }
}
