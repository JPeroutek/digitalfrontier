package com.partatoes.digitalfrontier.datagen;

import com.mojang.math.Quadrant;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.block.custom.PatternBlock;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.*;
import net.minecraft.client.data.models.blockstates.*;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModModelProvider extends FabricModelProvider {
    // A cube with a second, full-bright overlay layer: the vanilla way to get per-pixel emissive textures
    private static final TextureSlot GLOW = TextureSlot.create("glow");
    private static final ModelTemplate EMISSIVE_CUBE_ALL = new ModelTemplate(
            Optional.of(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "block/emissive_cube_all")), Optional.empty(), TextureSlot.ALL, GLOW);

    public ModModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        BlockModelGenerators.BlockFamilyProvider gridstonePool = blockStateModelGenerator.family(ModBlocks.GRIDSTONE_BLOCK);
        BlockModelGenerators.BlockFamilyProvider crackedGridstonePool = blockStateModelGenerator.family(ModBlocks.CRACKED_GRIDSTONE_BLOCK);
        BlockModelGenerators.BlockFamilyProvider gridstoneBricksPool = blockStateModelGenerator.family(ModBlocks.GRIDSTONE_BRICKS_BLOCK);

        blockStateModelGenerator.createTrivialCube(ModBlocks.PIXEL_BLOCK);
        blockStateModelGenerator.createTrivialCube(ModBlocks.LUMINANCE_ORE);
        blockStateModelGenerator.createTrivialCube(ModBlocks.BOOSTER_BLOCK);

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
        blockStateModelGenerator.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(ModBlocks.PIXEL_LAMP)
                        .with(BlockModelGenerators.createBooleanModelDispatch(
                                BlockStateProperties.LIT,
                                BlockModelGenerators.plainVariant(blockStateModelGenerator.createSuffixedVariant(ModBlocks.PIXEL_LAMP, "_lit", ModelTemplates.CUBE_ALL, TextureMapping::cube)),
                                BlockModelGenerators.plainVariant(TexturedModel.CUBE.create(ModBlocks.PIXEL_LAMP, blockStateModelGenerator.modelOutput))))
        );

        blockStateModelGenerator.createNonTemplateHorizontalBlock(ModBlocks.KEYBOARD_BLOCK);

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
        Identifier laserModel = ModelTemplates.CUBE_BOTTOM_TOP.create(ModBlocks.DIGITIZING_LASER, new TextureMapping()
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get("thick_ring").get(DyeColor.CYAN)))
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(ModBlocks.GRIDSTONE_BRICKS_BLOCK))
                .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(ModBlocks.GRIDSTONE_BLOCK)), blockStateModelGenerator.modelOutput);
        blockStateModelGenerator.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(ModBlocks.DIGITIZING_LASER, BlockModelGenerators.plainVariant(laserModel)));
        Identifier tuningLaserModel = ModelTemplates.CUBE_BOTTOM_TOP.create(ModBlocks.GRID_TUNING_LASER, new TextureMapping()
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get("thick_ring").get(DyeColor.ORANGE)))
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get("border_1").get(DyeColor.CYAN)))
                .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(ModBlocks.GRIDSTONE_BLOCK)), blockStateModelGenerator.modelOutput);
        blockStateModelGenerator.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(ModBlocks.GRID_TUNING_LASER, BlockModelGenerators.plainVariant(tuningLaserModel)));

        blockStateModelGenerator.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(ModBlocks.LIGHT_BARRIER_EMITTER)
                        .with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.POWERED,
                                BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(ModBlocks.LIGHT_BARRIER_EMITTER, "_powered")),
                                BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(ModBlocks.LIGHT_BARRIER_EMITTER))))
                        .with(faceRotations())
        );
        blockStateModelGenerator.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(ModBlocks.LIGHT_BARRIER, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(ModBlocks.LIGHT_BARRIER)))
                        .with(faceRotations())
        );
    }

    // An emissive cube per flicker value: steady, then glows that drop out now and then (<name>_glow_flicker_<n>),
    // each flickering one in several phases so neighbouring blocks don't flicker together
    private static void registerPattern(BlockModelGenerators blockStateModelGenerator, Block block) {
        PropertyDispatch.C1<MultiVariant, Integer> variants = PropertyDispatch.initial(PatternBlock.FLICKER);
        for (int flicker : PatternBlock.FLICKER.getPossibleValues()) {
            if (flicker == 0) {
                TextureMapping textures = new TextureMapping()
                        .put(TextureSlot.ALL, TextureMapping.getBlockTexture(block))
                        .put(GLOW, TextureMapping.getBlockTexture(block, "_glow"));
                Identifier modelId = EMISSIVE_CUBE_ALL.create(block, textures, blockStateModelGenerator.modelOutput);
                variants.select(flicker, BlockModelGenerators.plainVariant(modelId));
                continue;
            }
            List<Variant> phases = new ArrayList<>();
            for (int phase = 0; phase < ModTextureProvider.FLICKER_PHASES; phase++) {
                String suffix = "_flicker_" + flicker + ModTextureProvider.phaseSuffix(phase);
                TextureMapping textures = new TextureMapping()
                        .put(TextureSlot.ALL, TextureMapping.getBlockTexture(block))
                        .put(GLOW, TextureMapping.getBlockTexture(block, "_glow" + suffix));
                Identifier modelId = EMISSIVE_CUBE_ALL.create(ModelLocationUtils.getModelLocation(block, suffix), textures, blockStateModelGenerator.modelOutput);
                phases.add(new Variant(modelId));
            }
            variants.select(flicker, BlockModelGenerators.variants(phases.toArray(Variant[]::new)));
        }
        blockStateModelGenerator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(variants));
    }

    // Turns a cube can be shown at, so every face of an animated block can come out at a different angle
    private static final Quadrant[][] TURNS = {
            {Quadrant.R0, Quadrant.R0},
            {Quadrant.R90, Quadrant.R0},
            {Quadrant.R180, Quadrant.R0},
            {Quadrant.R270, Quadrant.R0},
            {Quadrant.R90, Quadrant.R90},
            {Quadrant.R270, Quadrant.R90},
    };

    /**
     * An animated emissive cube drawn from several textures (<name><template suffix><phase suffix>, see
     * ModTextureProvider), each at several turns; the game picks one per block by position, so a patch of them
     * neither animates in step nor repeats one look.
     */
    private static void registerAnimated(BlockModelGenerators blockStateModelGenerator, Block block, int templates) {
        List<Variant> variants = new ArrayList<>();
        for (int template = 0; template < templates; template++) {
            for (int phase = 0; phase < ModTextureProvider.ANIMATION_PHASES; phase++) {
                String suffix = ModTextureProvider.templateSuffix(template) + ModTextureProvider.phaseSuffix(phase);
                TextureMapping textures = new TextureMapping()
                        .put(TextureSlot.ALL, TextureMapping.getBlockTexture(block, suffix))
                        .put(GLOW, TextureMapping.getBlockTexture(block, suffix + "_glow"));
                Identifier modelId = suffix.isEmpty()
                        ? EMISSIVE_CUBE_ALL.create(block, textures, blockStateModelGenerator.modelOutput)
                        : EMISSIVE_CUBE_ALL.create(ModelLocationUtils.getModelLocation(block, suffix), textures, blockStateModelGenerator.modelOutput);
                for (Quadrant[] turn : TURNS) {
                    variants.add(new Variant(modelId).withXRot(turn[0]).withYRot(turn[1]));
                }
            }
        }
        blockStateModelGenerator.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block,
                BlockModelGenerators.variants(variants.toArray(Variant[]::new))));
    }

    // Expects the block's texture plus a <name>_glow texture holding only the pixels that should glow
    private static void registerEmissiveCubeAll(BlockModelGenerators blockStateModelGenerator, Block block) {
        registerEmissiveCubeAll(blockStateModelGenerator, block, block);
    }

    private static void registerEmissiveCubeAll(BlockModelGenerators blockStateModelGenerator, Block block, Block textureSource) {
        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.ALL, TextureMapping.getBlockTexture(textureSource))
                .put(GLOW, TextureMapping.getBlockTexture(textureSource, "_glow"));
        Identifier modelId = EMISSIVE_CUBE_ALL.create(block, textures, blockStateModelGenerator.modelOutput);
        blockStateModelGenerator.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(modelId)));
    }

    // Turns a model that faces north on the floor to each face and facing, as for buttons
    private static PropertyDispatch<VariantMutator> faceRotations() {
        return PropertyDispatch.modify(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING)
                .select(AttachFace.FLOOR, Direction.EAST, BlockModelGenerators.Y_ROT_90)
                .select(AttachFace.FLOOR, Direction.WEST, BlockModelGenerators.Y_ROT_270)
                .select(AttachFace.FLOOR, Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                .select(AttachFace.FLOOR, Direction.NORTH, BlockModelGenerators.NOP)
                .select(AttachFace.WALL, Direction.EAST, BlockModelGenerators.Y_ROT_90.then(BlockModelGenerators.X_ROT_90))
                .select(AttachFace.WALL, Direction.WEST, BlockModelGenerators.Y_ROT_270.then(BlockModelGenerators.X_ROT_90))
                .select(AttachFace.WALL, Direction.SOUTH, BlockModelGenerators.Y_ROT_180.then(BlockModelGenerators.X_ROT_90))
                .select(AttachFace.WALL, Direction.NORTH, BlockModelGenerators.X_ROT_90)
                .select(AttachFace.CEILING, Direction.EAST, BlockModelGenerators.Y_ROT_270.then(BlockModelGenerators.X_ROT_180))
                .select(AttachFace.CEILING, Direction.WEST, BlockModelGenerators.Y_ROT_90.then(BlockModelGenerators.X_ROT_180))
                .select(AttachFace.CEILING, Direction.SOUTH, BlockModelGenerators.X_ROT_180)
                .select(AttachFace.CEILING, Direction.NORTH, BlockModelGenerators.Y_ROT_180.then(BlockModelGenerators.X_ROT_180));
    }
    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
        itemModelGenerator.generateFlatItem(ModItems.BINARY_0, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.BINARY_1, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.BYTE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.COAL_DUST, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.PIXEL_DUST, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.LUMINESSENCE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.LIGHTCYCLE_BATON, ModelTemplates.FLAT_ITEM);

        // Placeholder: borrow the vanilla snowball model until it has its own art
        itemModelGenerator.itemModelOutput.accept(ModItems.DIMENSION_TELEPORTER, ItemModelUtils.plainModel(Identifier.withDefaultNamespace("item/snowball")));
        // Placeholder: the vanilla compass until the tuner has its own art
        itemModelGenerator.itemModelOutput.accept(ModItems.LASER_TUNER, ItemModelUtils.plainModel(Identifier.withDefaultNamespace("item/compass_16")));
        itemModelGenerator.itemModelOutput.accept(ModItems.ESCAPE_KEY, ItemModelUtils.plainModel(Identifier.withDefaultNamespace("item/trial_key")));
        itemModelGenerator.itemModelOutput.accept(ModItems.MEMORY_PAGING, ItemModelUtils.plainModel(Identifier.withDefaultNamespace("item/recovery_compass_16")));
        itemModelGenerator.itemModelOutput.accept(ModItems.CORE_DUMP, ItemModelUtils.plainModel(Identifier.withDefaultNamespace("item/heart_of_the_sea")));

    }
}
