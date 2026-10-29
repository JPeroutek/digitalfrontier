package com.partatoes.digitalfrontier.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class KeyboardBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public KeyboardBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        switch (state.getValue(FACING)) {
            case NORTH:
                return Block.box(2, 0, 2, 13, 1.4, 6);
            case SOUTH:
                return Block.box(3, 0, 10, 14, 1.4, 14);
            case EAST:
                return Block.box(10, 0, 2, 14, 1.4, 13);
            case WEST:
                return Block.box(2, 0, 3, 6, 1.4, 14);
            default:
                return Block.box(2, 0, 2, 13, 1.4, 6);
        }
//        return Block.createCuboidShape(0, 0, 0, 16, 1.4, 16);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
