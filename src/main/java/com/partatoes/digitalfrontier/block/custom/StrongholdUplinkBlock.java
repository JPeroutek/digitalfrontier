package com.partatoes.digitalfrontier.block.custom;

import com.partatoes.digitalfrontier.grid.stronghold.GridEvent;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

/**
 * Found in strongholds. A redstone signal reaching it (the rising edge) is a {@code redstone} trigger event, so
 * players can wire their own ways of rebooting a lieutenant.
 */
public class StrongholdUplinkBlock extends Block {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public StrongholdUplinkBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }
        boolean powered = world.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) {
            return;
        }
        world.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (powered) {
            world.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0f, 0.6f);
            StrongholdManager.fire(serverWorld, new GridEvent.Redstone(pos));
        }
    }
}
