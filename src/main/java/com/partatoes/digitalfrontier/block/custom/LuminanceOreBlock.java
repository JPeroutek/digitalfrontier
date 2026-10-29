package com.partatoes.digitalfrontier.block.custom;

import com.partatoes.digitalfrontier.particle.LuminanceDustParticleEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class LuminanceOreBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public LuminanceOreBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(LIT);
    }

    @Override
    public void attack(BlockState state, Level world, BlockPos pos, Player player) {
        LuminanceOreBlock.light(state, world, pos);
        super.attack(state, world, pos, player);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully()) {
            LuminanceOreBlock.light(state, world, pos);
        }
        super.stepOn(world, pos, state, entity);
    }
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()) {
            LuminanceOreBlock.spawnParticles(world, pos);
        } else {
            LuminanceOreBlock.light(state, world, pos);
        }

        return stack.getItem() instanceof BlockItem && (new BlockPlaceContext(player, hand, stack, hit)).canPlace() ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel world, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, world, pos, tool, dropExperience);
        if (dropExperience) {
            this.tryDropExperience(world, pos, tool, UniformInt.of(1, 5));
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            world.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            LuminanceOreBlock.spawnParticles(world, pos);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    private static void light(BlockState state, Level world, BlockPos pos) {
        LuminanceOreBlock.spawnParticles(world, pos);
        if (!state.getValue(LIT)) {
            world.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        }
    }

    private static void spawnParticles(Level world, BlockPos pos) {
        double d = 0.5625;
        RandomSource random = world.random;
        for (Direction direction : Direction.values()) {
            BlockPos blockPos = pos.relative(direction);
            if (world.getBlockState(blockPos).isSolidRender()) continue;
            Direction.Axis axis = direction.getAxis();
            double e = axis == Direction.Axis.X ? 0.5 + d * (double)direction.getStepX() : (double)random.nextFloat();
            double f = axis == Direction.Axis.Y ? 0.5 + d * (double)direction.getStepY() : (double)random.nextFloat();
            double g = axis == Direction.Axis.Z ? 0.5 + d * (double)direction.getStepZ() : (double)random.nextFloat();
            world.addParticle(LuminanceDustParticleEffect.DEFAULT, (double)pos.getX() + e, (double)pos.getY() + f, (double)pos.getZ() + g, 0.0, 0.0, 0.0);
        }
    }
}