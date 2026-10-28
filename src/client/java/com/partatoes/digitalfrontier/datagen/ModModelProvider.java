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
import net.minecraft.client.render.model.json.ModelVariant;
import net.minecraft.client.render.model.json.ModelVariantOperator;
import net.minecraft.client.render.model.json.WeightedVariant;
import net.minecraft.state.property.Properties;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AxisRotation;
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
                VariantsBlockModelDefinitionCreator.of(ModBlocks.PIXEL_LAMP)
                        .with(BlockStateModelGenerator.createBooleanModelMap(
                                Properties.LIT,
                                BlockStateModelGenerator.createWeightedVariant(blockStateModelGenerator.createSubModel(ModBlocks.PIXEL_LAMP, "_lit", Models.CUBE_ALL, TextureMap::all)),
                                BlockStateModelGenerator.createWeightedVariant(TexturedModel.CUBE_ALL.upload(ModBlocks.PIXEL_LAMP, blockStateModelGenerator.modelCollector))))
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
        blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createSingletonBlockState(ModBlocks.DIGITIZING_LASER, BlockStateModelGenerator.createWeightedVariant(laserModel)));
        Identifier tuningLaserModel = Models.CUBE_BOTTOM_TOP.upload(ModBlocks.GRID_TUNING_LASER, new TextureMap()
                .put(TextureKey.TOP, TextureMap.getId(ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get("thick_ring").get(DyeColor.ORANGE)))
                .put(TextureKey.SIDE, TextureMap.getId(ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get("border_1").get(DyeColor.CYAN)))
                .put(TextureKey.BOTTOM, TextureMap.getId(ModBlocks.GRIDSTONE_BLOCK)), blockStateModelGenerator.modelCollector);
        blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createSingletonBlockState(ModBlocks.GRID_TUNING_LASER, BlockStateModelGenerator.createWeightedVariant(tuningLaserModel)));

        blockStateModelGenerator.blockStateCollector.accept(
                VariantsBlockModelDefinitionCreator.of(ModBlocks.LIGHT_BARRIER_EMITTER)
                        .with(BlockStateModelGenerator.createBooleanModelMap(Properties.POWERED,
                                BlockStateModelGenerator.createWeightedVariant(ModelIds.getBlockSubModelId(ModBlocks.LIGHT_BARRIER_EMITTER, "_powered")),
                                BlockStateModelGenerator.createWeightedVariant(ModelIds.getBlockModelId(ModBlocks.LIGHT_BARRIER_EMITTER))))
                        .coordinate(faceRotations())
        );
        blockStateModelGenerator.blockStateCollector.accept(
                VariantsBlockModelDefinitionCreator.of(ModBlocks.LIGHT_BARRIER, BlockStateModelGenerator.createWeightedVariant(ModelIds.getBlockModelId(ModBlocks.LIGHT_BARRIER)))
                        .coordinate(faceRotations())
        );
    }

    // An emissive cube per flicker value: steady, then glows that drop out now and then (<name>_glow_flicker_<n>),
    // each flickering one in several phases so neighbouring blocks don't flicker together
    private static void registerPattern(BlockStateModelGenerator blockStateModelGenerator, Block block) {
        BlockStateVariantMap.SingleProperty<WeightedVariant, Integer> variants = BlockStateVariantMap.models(PatternBlock.FLICKER);
        for (int flicker : PatternBlock.FLICKER.getValues()) {
            if (flicker == 0) {
                TextureMap textures = new TextureMap()
                        .put(TextureKey.ALL, TextureMap.getId(block))
                        .put(GLOW, TextureMap.getSubId(block, "_glow"));
                Identifier modelId = EMISSIVE_CUBE_ALL.upload(block, textures, blockStateModelGenerator.modelCollector);
                variants.register(flicker, BlockStateModelGenerator.createWeightedVariant(modelId));
                continue;
            }
            List<ModelVariant> phases = new ArrayList<>();
            for (int phase = 0; phase < ModTextureProvider.FLICKER_PHASES; phase++) {
                String suffix = "_flicker_" + flicker + ModTextureProvider.phaseSuffix(phase);
                TextureMap textures = new TextureMap()
                        .put(TextureKey.ALL, TextureMap.getId(block))
                        .put(GLOW, TextureMap.getSubId(block, "_glow" + suffix));
                Identifier modelId = EMISSIVE_CUBE_ALL.upload(ModelIds.getBlockSubModelId(block, suffix), textures, blockStateModelGenerator.modelCollector);
                phases.add(new ModelVariant(modelId));
            }
            variants.register(flicker, BlockStateModelGenerator.createWeightedVariant(phases.toArray(ModelVariant[]::new)));
        }
        blockStateModelGenerator.blockStateCollector.accept(VariantsBlockModelDefinitionCreator.of(block).with(variants));
    }

    // Turns a cube can be shown at, so every face of an animated block can come out at a different angle
    private static final AxisRotation[][] TURNS = {
            {AxisRotation.R0, AxisRotation.R0},
            {AxisRotation.R90, AxisRotation.R0},
            {AxisRotation.R180, AxisRotation.R0},
            {AxisRotation.R270, AxisRotation.R0},
            {AxisRotation.R90, AxisRotation.R90},
            {AxisRotation.R270, AxisRotation.R90},
    };

    /**
     * An animated emissive cube drawn from several textures (<name><template suffix><phase suffix>, see
     * ModTextureProvider), each at several turns; the game picks one per block by position, so a patch of them
     * neither animates in step nor repeats one look.
     */
    private static void registerAnimated(BlockStateModelGenerator blockStateModelGenerator, Block block, int templates) {
        List<ModelVariant> variants = new ArrayList<>();
        for (int template = 0; template < templates; template++) {
            for (int phase = 0; phase < ModTextureProvider.ANIMATION_PHASES; phase++) {
                String suffix = ModTextureProvider.templateSuffix(template) + ModTextureProvider.phaseSuffix(phase);
                TextureMap textures = new TextureMap()
                        .put(TextureKey.ALL, TextureMap.getSubId(block, suffix))
                        .put(GLOW, TextureMap.getSubId(block, suffix + "_glow"));
                Identifier modelId = suffix.isEmpty()
                        ? EMISSIVE_CUBE_ALL.upload(block, textures, blockStateModelGenerator.modelCollector)
                        : EMISSIVE_CUBE_ALL.upload(ModelIds.getBlockSubModelId(block, suffix), textures, blockStateModelGenerator.modelCollector);
                for (AxisRotation[] turn : TURNS) {
                    variants.add(new ModelVariant(modelId).withRotationX(turn[0]).withRotationY(turn[1]));
                }
            }
        }
        blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createSingletonBlockState(block,
                BlockStateModelGenerator.createWeightedVariant(variants.toArray(ModelVariant[]::new))));
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
        blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createSingletonBlockState(block, BlockStateModelGenerator.createWeightedVariant(modelId)));
    }

    // Turns a model that faces north on the floor to each face and facing, as for buttons
    private static BlockStateVariantMap<ModelVariantOperator> faceRotations() {
        return BlockStateVariantMap.operations(Properties.BLOCK_FACE, Properties.HORIZONTAL_FACING)
                .register(BlockFace.FLOOR, Direction.EAST, BlockStateModelGenerator.ROTATE_Y_90)
                .register(BlockFace.FLOOR, Direction.WEST, BlockStateModelGenerator.ROTATE_Y_270)
                .register(BlockFace.FLOOR, Direction.SOUTH, BlockStateModelGenerator.ROTATE_Y_180)
                .register(BlockFace.FLOOR, Direction.NORTH, BlockStateModelGenerator.NO_OP)
                .register(BlockFace.WALL, Direction.EAST, BlockStateModelGenerator.ROTATE_Y_90.then(BlockStateModelGenerator.ROTATE_X_90))
                .register(BlockFace.WALL, Direction.WEST, BlockStateModelGenerator.ROTATE_Y_270.then(BlockStateModelGenerator.ROTATE_X_90))
                .register(BlockFace.WALL, Direction.SOUTH, BlockStateModelGenerator.ROTATE_Y_180.then(BlockStateModelGenerator.ROTATE_X_90))
                .register(BlockFace.WALL, Direction.NORTH, BlockStateModelGenerator.ROTATE_X_90)
                .register(BlockFace.CEILING, Direction.EAST, BlockStateModelGenerator.ROTATE_Y_270.then(BlockStateModelGenerator.ROTATE_X_180))
                .register(BlockFace.CEILING, Direction.WEST, BlockStateModelGenerator.ROTATE_Y_90.then(BlockStateModelGenerator.ROTATE_X_180))
                .register(BlockFace.CEILING, Direction.SOUTH, BlockStateModelGenerator.ROTATE_X_180)
                .register(BlockFace.CEILING, Direction.NORTH, BlockStateModelGenerator.ROTATE_Y_180.then(BlockStateModelGenerator.ROTATE_X_180));
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
