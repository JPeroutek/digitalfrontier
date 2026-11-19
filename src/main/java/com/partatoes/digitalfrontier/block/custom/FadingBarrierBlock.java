package com.partatoes.digitalfrontier.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A short-lived wall of light, left by a Light Grenade's burst: solid while it lasts, then gone. It schedules its own
 * removal when placed, so nothing has to remember to clear it.
 */
public class FadingBarrierBlock extends Block {
    /** How long a fading barrier stands, in ticks. */
    public static final int LIFETIME = 100;

    public FadingBarrierBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            level.scheduleTick(pos, this, LIFETIME);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }
}
