package com.partatoes.digitalfrontier.block.custom;

import com.partatoes.digitalfrontier.grid.exit.GridExit;
import com.partatoes.digitalfrontier.grid.stronghold.GridEvent;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The column of light at a grid's Portal site, up to build height. Players who step into it leave the grid. The blocks
 * themselves are invisible and can't be targeted: what shows is the beam of light the client draws through them (see
 * {@code PortalBeamRenderer}). Stepping in still counts, as entities touch any block they're inside.
 */
public class GridPortalBlock extends Block {
    public GridPortalBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        // A player touches several portal blocks in one tick; only the first one, while they're still here, counts
        if (entity instanceof ServerPlayerEntity player && player.getWorld() == world && ModDimensions.isGrid(world)) {
            StrongholdManager.fire(player.getServerWorld(), new GridEvent.PortalUsed());
            GridExit.toRealWorld(player);
        }
    }
}
