package com.partatoes.digitalfrontier.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

public class KeyboardBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public KeyboardBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        switch (state.get(FACING)) {
            case NORTH:
                return Block.createCuboidShape(2, 0, 2, 13, 1.4, 6);
            case SOUTH:
                return Block.createCuboidShape(3, 0, 10, 14, 1.4, 14);
            case EAST:
                return Block.createCuboidShape(10, 0, 2, 14, 1.4, 13);
            case WEST:
                return Block.createCuboidShape(2, 0, 3, 6, 1.4, 14);
            default:
                return Block.createCuboidShape(2, 0, 2, 13, 1.4, 6);
        }
//        return Block.createCuboidShape(0, 0, 0, 16, 1.4, 16);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
