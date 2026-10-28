package com.partatoes.digitalfrontier.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class LightBarrierBlock extends HorizontalFacingBlock {
    public static final EnumProperty<Direction> FACING;
    public static final EnumProperty<BlockFace> FACE;
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

    public LightBarrierBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState()).with(FACING, Direction.NORTH)).with(FACE, BlockFace.WALL));
    }

    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return null;
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        Direction dir = (Direction)state.get(FACING);
        switch ((BlockFace)state.get(FACE)) {
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

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, FACE});
    }

    static {
        FACING = Properties.HORIZONTAL_FACING;
        FACE = Properties.BLOCK_FACE;
        CEILING_EAST_SHAPE = VoxelShapes.cuboid(0.5, 0.0, 0.0, 0.5625, 1, 1.0);
        CEILING_WEST_SHAPE = VoxelShapes.cuboid(0.4375, 0.0, 0.0, 0.5, 1, 1.0);
        CEILING_NORTH_SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.4375, 1.0, 1.0, 0.5);
        CEILING_SOUTH_SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.5, 1.0, 1.0, 0.5625);
        FLOOR_EAST_SHAPE = VoxelShapes.cuboid(0.5, 0.0, 0.0, 0.5625, 1.0, 1.0);
        FLOOR_WEST_SHAPE = VoxelShapes.cuboid(0.4375, 0.0, 0.0, 0.5, 1.0, 1.0);
        FLOOR_NORTH_SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.4375, 1.0, 1.0, 0.5);
        FLOOR_SOUTH_SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.5, 1.0, 1.0, 0.5625);
        NORTH_SHAPE = VoxelShapes.cuboid(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0);
        SOUTH_SHAPE = VoxelShapes.cuboid(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0);
        WEST_SHAPE = VoxelShapes.cuboid(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0);
        EAST_SHAPE = VoxelShapes.cuboid(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0);
    }
}
