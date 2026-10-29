package com.partatoes.digitalfrontier.block.custom;

import com.partatoes.digitalfrontier.grid.exit.GridExit;
import com.partatoes.digitalfrontier.grid.stronghold.GridEvent;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The column of light at a grid's Portal site, up to build height. Players who step into it leave the grid. The blocks
 * themselves are invisible and can't be targeted: what shows is the beam of light the client draws through them (see
 * {@code PortalBeamRenderer}). Stepping in still counts, as entities touch any block they're inside.
 */
public class GridPortalBlock extends Block {
    public GridPortalBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean bl) {
        // A player touches several portal blocks in one tick; only the first one, while they're still here, counts
        if (entity instanceof ServerPlayer player && player.level() == world && ModDimensions.isGrid(world)) {
            StrongholdManager.fire(player.level(), new GridEvent.PortalUsed());
            GridExit.toRealWorld(player);
        }
    }
}
