package com.partatoes.digitalfrontier.block.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The wall of light an arena light cycle leaves behind it (see {@code CycleMatch}): cyan from the challenger's cycle,
 * orange from a program's. Riding into one ends a rider's round. The match clears them all when it's over.
 */
public class TrailWallBlock extends Block {
    /** Whether a program left it, rather than the challenger. */
    public static final BooleanProperty PROGRAM = BooleanProperty.create("program");

    public TrailWallBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any().setValue(PROGRAM, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROGRAM);
    }
}
