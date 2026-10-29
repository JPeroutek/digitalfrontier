package com.partatoes.digitalfrontier.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LightBarrierBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<Direction> FACING;
    public static final EnumProperty<AttachFace> FACE;
    protected static final VoxelShape CEILING_EAST_SHAPE;
    protected static final VoxelShape CEILING_WEST_SHAPE;
    protected static final VoxelShape CEILING_NORTH_SHAPE;
    protected static final VoxelShape CEILING_SOUTH_SHAPE;
    protected static final VoxelShape FLOOR_EAST_SHAPE;
    protected static final VoxelShape FLOOR_WEST_SHAPE;
    protected static final VoxelShape FLOOR_NORTH_SHAPE;
    protected static final VoxelShape FLOOR_SOUTH_SHAPE;
    protected static final VoxelShape NORTH_SHAPE;
    protected static final VoxelShape SOUTH_SHAPE;
    protected static final VoxelShape WEST_SHAPE;
    protected static final VoxelShape EAST_SHAPE;

    public LightBarrierBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(FACE, AttachFace.WALL));
    }

    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        Direction dir = (Direction)state.getValue(FACING);
        switch ((AttachFace)state.getValue(FACE)) {
            case FLOOR:
                switch (dir) {
                    case EAST:
                        return FLOOR_EAST_SHAPE;
                    case WEST:
                        return FLOOR_WEST_SHAPE;
                    case SOUTH:
                        return FLOOR_SOUTH_SHAPE;
                    default:
                        return FLOOR_NORTH_SHAPE;
                }
            case WALL:
                switch (dir) {
                    case EAST:
                        return EAST_SHAPE;
                    case WEST:
                        return WEST_SHAPE;
                    case SOUTH:
                        return SOUTH_SHAPE;
                    default:
                        return NORTH_SHAPE;
                }
            default:
                switch (dir) {
                    case EAST:
                        return CEILING_EAST_SHAPE;
                    case WEST:
                        return CEILING_WEST_SHAPE;
                    case SOUTH:
                        return CEILING_SOUTH_SHAPE;
                    default:
                        return CEILING_NORTH_SHAPE;
                }
        }
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, FACE});
    }

    static {
        FACING = BlockStateProperties.HORIZONTAL_FACING;
        FACE = BlockStateProperties.ATTACH_FACE;
        CEILING_EAST_SHAPE = Shapes.box(0.5, 0.0, 0.0, 0.5625, 1, 1.0);
        CEILING_WEST_SHAPE = Shapes.box(0.4375, 0.0, 0.0, 0.5, 1, 1.0);
        CEILING_NORTH_SHAPE = Shapes.box(0.0, 0.0, 0.4375, 1.0, 1.0, 0.5);
        CEILING_SOUTH_SHAPE = Shapes.box(0.0, 0.0, 0.5, 1.0, 1.0, 0.5625);
        FLOOR_EAST_SHAPE = Shapes.box(0.5, 0.0, 0.0, 0.5625, 1.0, 1.0);
        FLOOR_WEST_SHAPE = Shapes.box(0.4375, 0.0, 0.0, 0.5, 1.0, 1.0);
        FLOOR_NORTH_SHAPE = Shapes.box(0.0, 0.0, 0.4375, 1.0, 1.0, 0.5);
        FLOOR_SOUTH_SHAPE = Shapes.box(0.0, 0.0, 0.5, 1.0, 1.0, 0.5625);
        NORTH_SHAPE = Shapes.box(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0);
        SOUTH_SHAPE = Shapes.box(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0);
        WEST_SHAPE = Shapes.box(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0);
        EAST_SHAPE = Shapes.box(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0);
    }
}
