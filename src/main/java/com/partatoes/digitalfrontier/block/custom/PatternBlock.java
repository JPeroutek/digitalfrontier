package com.partatoes.digitalfrontier.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

/**
 * Gridstone with glowing pattern lines. Lightly corrupted ground makes some of them flicker: their glow drops out now
 * and then for a tick or two. Flickering is drawn by an animated texture, one timing per value, so neighbouring
 * blocks with different values don't flicker in step.
 */
public class PatternBlock extends Block {
    /** 0 for a steady glow; 1 and 2 flicker, on different timings. */
    public static final IntProperty FLICKER = IntProperty.of("flicker", 0, 2);

    public PatternBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FLICKER, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FLICKER);
    }
}
