package com.partatoes.digitalfrontier.block.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A tile of the arena's disc duel field, floating over the pit. In a duel the tiles drop away ring by ring from the
 * outside in, each one flashing a warning first (see {@code DiscDuel}); they're all back when the round ends.
 */
public class ArenaTileBlock extends Block {
    public static final BooleanProperty WARNING = BooleanProperty.create("warning");

    public ArenaTileBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any().setValue(WARNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WARNING);
    }
}
