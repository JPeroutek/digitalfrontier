package com.partatoes.digitalfrontier.block.custom;

import com.partatoes.digitalfrontier.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The light a glowing entity casts (see {@link Emitter}): an invisible, intangible block placed where the entity is.
 * Vanilla has no light that moves, so emitters keep placing these as they go, and each one clears itself away as soon
 * as no emitter is beside it, checking twice a second. Nothing has to remember to remove them, so a crash, an
 * unloaded chunk or a despawn can't leave one behind for long.
 */
public class MovingLightBlock extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 15);
    private static final int CHECK_TICKS = 10;
    // How far an emitter's light block may be from it before it goes dark
    private static final double REACH = 1.5;

    /** Something that lights the world around it as it moves. */
    public interface Emitter {
        /** How brightly it shines, 0 (dark) to 15. */
        int emittedLight();
    }

    public MovingLightBlock(Properties settings) {
        super(settings);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 15));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    /**
     * Lights the spot an emitter is in, at its level, if the spot is open air (or already lit more dimly). Called
     * from the emitter's server tick.
     *
     * @param at where the light should be, usually the block at the emitter's middle
     */
    public static void shine(Level level, BlockPos at, int brightness) {
        if (level.isClientSide() || brightness <= 0) {
            return;
        }
        int clamped = Math.min(brightness, 15);
        BlockState here = level.getBlockState(at);
        if (here.isAir() || here.is(ModBlocks.MOVING_LIGHT) && here.getValue(LEVEL) < clamped) {
            level.setBlock(at, ModBlocks.MOVING_LIGHT.defaultBlockState().setValue(LEVEL, clamped), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            level.scheduleTick(pos, this, CHECK_TICKS);
        }
    }

    // Goes dark once no emitter is beside it
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean lit = !level.getEntitiesOfClass(Entity.class, new AABB(pos).inflate(REACH),
                entity -> entity.isAlive() && entity instanceof Emitter emitter && emitter.emittedLight() > 0).isEmpty();
        if (lit) {
            level.scheduleTick(pos, this, CHECK_TICKS);
        } else {
            level.removeBlock(pos, false);
        }
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
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return true;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return true;
    }
}
