package com.partatoes.digitalfrontier.block.custom;

import com.partatoes.digitalfrontier.grid.stronghold.GridEvent;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;

/**
 * Found in strongholds. A redstone signal reaching it (the rising edge) is a {@code redstone} trigger event, so
 * players can wire their own ways of rebooting a lieutenant.
 */
public class StrongholdUplinkBlock extends Block {
    public static final BooleanProperty POWERED = Properties.POWERED;

    public StrongholdUplinkBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        boolean powered = world.isReceivingRedstonePower(pos);
        if (powered == state.get(POWERED)) {
            return;
        }
        world.setBlockState(pos, state.with(POWERED, powered), Block.NOTIFY_LISTENERS);
        if (powered) {
            world.playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.0f, 0.6f);
            StrongholdManager.fire(serverWorld, new GridEvent.Redstone(pos));
        }
    }
}
