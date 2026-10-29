package com.partatoes.digitalfrontier.block.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Gridstone with glowing pattern lines. Lightly corrupted ground makes some of them flicker: their glow drops out now
 * and then for a tick or two. Flickering is drawn by an animated texture, one timing per value, so neighbouring
 * blocks with different values don't flicker in step.
 */
public class PatternBlock extends Block {
    /** 0 for a steady glow; 1 and 2 flicker, on different timings. */
    public static final IntegerProperty FLICKER = IntegerProperty.create("flicker", 0, 2);

    public PatternBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(FLICKER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLICKER);
    }
}
