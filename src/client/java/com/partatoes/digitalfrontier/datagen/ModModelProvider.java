package com.partatoes.digitalfrontier.datagen;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.Block;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.client.data.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public class ModModelProvider extends FabricModelProvider {
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
                    dyeBlockMap.forEach((color, block) -> {
//                        translationBuilder.add(block, String.format("%s %s Patterned Gridstone", toTitle(color.asString()), toTitle(patternName)));
                        blockStateModelGenerator.registerSimpleCubeAll(block);
                    });
                });

        // This shortcut may not work forever, check with every update
//        blockStateModelGenerator.blockStateCollector.accept(
//                BlockStateModelGenerator.createButtonBlockState(ModBlocks.LIGHT_BARRIER_EMITTER, ModBlocks.LIGHT_BARRIER_EMITTER_ID, Identifier.of(DigitalFrontier.MOD_ID, "light_barrier_emitter_powered")));
//        blockStateModelGenerator.blockStateCollector.accept(
//                BlockStateModelGenerator.createButtonBlockState(ModBlocks.LIGHT_BARRIER, ModBlocks.LIGHT_BARRIER_ID, ModBlocks.LIGHT_BARRIER_ID));

//        blockStateModelGenerator.blockStateCollector.accept(
//                BlockStateModelGenerator.createButtonBlockState(ModBlocks.LIGHT_BARRIER_EMITTER, ModelIds.getBlockModelId(ModBlocks.LIGHT_BARRIER_EMITTER), ModelIds.getBlockSubModelId(ModBlocks.LIGHT_BARRIER_EMITTER, "_powered")));
//        blockStateModelGenerator.blockStateCollector.accept(
//                BlockStateModelGenerator.createButtonBlockState(ModBlocks.LIGHT_BARRIER, ModBlocks.LIGHT_BARRIER_ID, ModBlocks.LIGHT_BARRIER_ID));
        blockStateModelGenerator.blockStateCollector.accept(
                createFacingFacePoweredBlockStates(ModBlocks.LIGHT_BARRIER_EMITTER, ModelIds.getBlockModelId(ModBlocks.LIGHT_BARRIER_EMITTER), ModelIds.getBlockSubModelId(ModBlocks.LIGHT_BARRIER_EMITTER, "_powered"))
        );
        blockStateModelGenerator.blockStateCollector.accept(
                createFacingFaceBlockStates(ModBlocks.LIGHT_BARRIER, ModelIds.getBlockModelId(ModBlocks.LIGHT_BARRIER))
        );
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

    }
}
