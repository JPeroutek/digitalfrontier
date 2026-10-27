package com.partatoes.digitalfrontier.block.custom;

import com.partatoes.digitalfrontier.grid.exit.GridExit;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The column of light at a grid's Portal site. Players who step into it leave the grid. */
public class GridPortalBlock extends Block {
    public GridPortalBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        // A player touches several portal blocks in one tick; only the first one, while they're still here, counts
        if (entity instanceof ServerPlayerEntity player && player.getWorld() == world && ModDimensions.isGrid(world)) {
            GridExit.toRealWorld(player);
        }
    }
}
