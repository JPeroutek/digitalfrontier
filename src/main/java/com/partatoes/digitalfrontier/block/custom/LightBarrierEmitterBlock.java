package com.partatoes.digitalfrontier.block.custom;

import com.mojang.serialization.MapCodec;
import com.partatoes.digitalfrontier.block.ModBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

public class LightBarrierEmitterBlock extends WallMountedBlock {
    public float volume = 0.5F;
    public float pitch = 1.0F;
    public static final EnumProperty<Direction> FACING;
    public static final BooleanProperty POWERED;
    protected static final VoxelShape CEILING_EAST_SHAPE;
    protected static final VoxelShape CEILING_WEST_SHAPE;
    protected static final VoxelShape CEILING_NORTH_SHAPE;
    protected static final VoxelShape CEILING_SOUTH_SHAPE;
    protected static final VoxelShape CEILING_EAST_POWERED_SHAPE;
    protected static final VoxelShape CEILING_WEST_POWERED_SHAPE;
    protected static final VoxelShape CEILING_NORTH_POWERED_SHAPE;
    protected static final VoxelShape CEILING_SOUTH_POWERED_SHAPE;
    protected static final VoxelShape FLOOR_EAST_SHAPE;
    protected static final VoxelShape FLOOR_WEST_SHAPE;
    protected static final VoxelShape FLOOR_NORTH_SHAPE;
    protected static final VoxelShape FLOOR_SOUTH_SHAPE;
    protected static final VoxelShape FLOOR_EAST_POWERED_SHAPE;
    protected static final VoxelShape FLOOR_WEST_POWERED_SHAPE;
    protected static final VoxelShape FLOOR_NORTH_POWERED_SHAPE;
    protected static final VoxelShape FLOOR_SOUTH_POWERED_SHAPE;
    protected static final VoxelShape NORTH_SHAPE;
    protected static final VoxelShape SOUTH_SHAPE;
    protected static final VoxelShape WEST_SHAPE;
    protected static final VoxelShape EAST_SHAPE;
    protected static final VoxelShape NORTH_POWERED_SHAPE;
    protected static final VoxelShape SOUTH_POWERED_SHAPE;
    protected static final VoxelShape WEST_POWERED_SHAPE;
    protected static final VoxelShape EAST_POWERED_SHAPE;

    public LightBarrierEmitterBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState()).with(FACING, Direction.NORTH)).with(POWERED, false)).with(FACE, BlockFace.WALL));
    }

    protected MapCodec<? extends WallMountedBlock> getCodec() {
        return null;
    }

    public void GenerateBridge(Direction dir, WorldAccess world, int length, BlockPos pos, BlockState state) {
        Boolean bridgeEnded = false;

        for(int i = 1; i < 15; ++i) {
            BlockPos addPos;
            BlockState bs;
            addPos = new BlockPos(0, 0, 0);
            bs = (BlockState)((BlockState) ModBlocks.LIGHT_BARRIER.getDefaultState().with(FACING, dir)).with(FACE, (BlockFace)state.get(FACE));
            label30:
            switch ((BlockFace)state.get(FACE)) {
                case FLOOR:
                    addPos = new BlockPos(0, i, 0);
                    break;
                case WALL:
                    switch (dir) {
                        case EAST:
                            addPos = new BlockPos(i, 0, 0);
                            break label30;
                        case WEST:
                            addPos = new BlockPos(-i, 0, 0);
                            break label30;
                        case SOUTH:
                            addPos = new BlockPos(0, 0, i);
                            break label30;
                        case NORTH:
                            addPos = new BlockPos(0, 0, -i);
                        default:
                            break label30;
                    }
                case CEILING:
                    addPos = new BlockPos(0, -i, 0);
            }

            if (!world.getBlockState(pos.add(addPos)).isAir() && !world.getBlockState(pos.add(addPos)).equals(bs)) {
                return;
            }

            if (!bridgeEnded && i < length) {
                world.setBlockState(pos.add(addPos), bs, 3);
            } else {
                bridgeEnded = true;
                world.setBlockState(pos.add(addPos), Blocks.AIR.getDefaultState(), 3);
            }
        }

    }

    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        if (!world.isClient) {
            int redstonePower = world.getReceivedRedstonePower(pos);
            Direction dir = (Direction)state.get(FACING);
            boolean bl = (Boolean)state.get(POWERED);
            if (bl != world.isReceivingRedstonePower(pos)) {
                if (bl) {
                    world.scheduleBlockTick(pos, this, 4);
                } else {
                    world.setBlockState(pos, (BlockState)state.cycle(POWERED), 2);
//                    world.playSound((PlayerEntity)null, pos, LaserBridgesDoors.ON, SoundCategory.BLOCKS, this.volume, this.pitch);
                    this.GenerateBridge(dir, world, redstonePower, pos, state);
                }
            } else if (bl) {
                world.scheduleBlockTick(pos, this, 4);
            }

        }
    }

    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!world.isClient) {
            if ((Boolean)state.get(POWERED)) {
                Direction dir = (Direction)state.get(FACING);
                if (!world.isReceivingRedstonePower(pos)) {
                    world.setBlockState(pos, (BlockState)state.cycle(POWERED), 2);
                    this.GenerateBridge(dir, world, 0, pos, state);
//                    world.playSound((PlayerEntity)null, pos, LaserBridgesDoors.OFF, SoundCategory.BLOCKS, this.volume, this.pitch);
                } else {
                    int redstonePower = world.getReceivedRedstonePower(pos);
                    this.GenerateBridge(dir, world, redstonePower, pos, state);
                }
            }

        }
    }

    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (!world.isClient) {
            boolean bl = (Boolean)state.get(POWERED);
            if (bl != world.isReceivingRedstonePower(pos)) {
                if (bl) {
                    world.scheduleBlockTick(pos, this, 4);
                } else {
                    world.setBlockState(pos, (BlockState)state.cycle(POWERED), 2);
//                    world.playSound((PlayerEntity)null, pos, LaserBridgesDoors.ON, SoundCategory.BLOCKS, this.volume, this.pitch);
                    Direction dir = (Direction)state.get(FACING);
                    int redstonePower = world.getReceivedRedstonePower(pos);
                    this.GenerateBridge(dir, world, redstonePower, pos, state);
                }
            }

        }
    }

    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            world.playSound((PlayerEntity)null, pos, SoundEvents.BLOCK_METAL_BREAK, SoundCategory.BLOCKS, 0.8F, 0.8F);
        }

        Direction dir = (Direction)state.get(FACING);
        this.GenerateBridge(dir, world, 0, pos, state);
//        world.playSound((PlayerEntity)null, pos, LaserBridgesDoors.OFF, SoundCategory.BLOCKS, this.volume, this.pitch);
        return state;
    }

    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        Direction dir = (Direction)state.get(FACING);
        if (!state.canPlaceAt(world, pos)) {
            this.GenerateBridge(dir, (WorldAccess)world, 0, pos, state);
//            ((World)world).playSound((PlayerEntity)null, pos, LaserBridgesDoors.OFF, SoundCategory.BLOCKS, this.volume, this.pitch);
            return Blocks.AIR.getDefaultState();
        } else {
            return state;
        }
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        Direction dir = (Direction)state.get(FACING);
        boolean powered = (Boolean)state.get(POWERED);
        switch ((BlockFace)state.get(FACE)) {
            case FLOOR:
                switch (dir) {
                    case EAST:
                        return powered ? FLOOR_EAST_POWERED_SHAPE : FLOOR_EAST_SHAPE;
                    case WEST:
                        return powered ? FLOOR_WEST_POWERED_SHAPE : FLOOR_WEST_SHAPE;
                    case SOUTH:
                        return powered ? FLOOR_SOUTH_POWERED_SHAPE : FLOOR_SOUTH_SHAPE;
                    default:
                        return powered ? FLOOR_NORTH_POWERED_SHAPE : FLOOR_NORTH_SHAPE;
                }
            case WALL:
                switch (dir) {
                    case EAST:
                        return powered ? EAST_POWERED_SHAPE : EAST_SHAPE;
                    case WEST:
                        return powered ? WEST_POWERED_SHAPE : WEST_SHAPE;
                    case SOUTH:
                        return powered ? SOUTH_POWERED_SHAPE : SOUTH_SHAPE;
                    default:
                        return powered ? NORTH_POWERED_SHAPE : NORTH_SHAPE;
                }
            default:
                switch (dir) {
                    case EAST:
                        return powered ? CEILING_EAST_POWERED_SHAPE : CEILING_EAST_SHAPE;
                    case WEST:
                        return powered ? CEILING_WEST_POWERED_SHAPE : CEILING_WEST_SHAPE;
                    case SOUTH:
                        return powered ? CEILING_SOUTH_POWERED_SHAPE : CEILING_SOUTH_SHAPE;
                    default:
                        return powered ? CEILING_NORTH_POWERED_SHAPE : CEILING_NORTH_SHAPE;
                }
        }
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, FACE, POWERED});
    }

    static {
        FACING = Properties.HORIZONTAL_FACING;
        POWERED = Properties.POWERED;
        CEILING_EAST_SHAPE = VoxelShapes.cuboid(0.375, 0.875, 0.0, 0.6875, 1.0, 1.0);
        CEILING_WEST_SHAPE = VoxelShapes.cuboid(0.3125, 0.875, 0.0, 0.625, 1.0, 1.0);
        CEILING_NORTH_SHAPE = VoxelShapes.cuboid(0.0, 0.875, 0.3125, 1.0, 1.0, 0.625);
        CEILING_SOUTH_SHAPE = VoxelShapes.cuboid(0.0, 0.875, 0.375, 1.0, 1.0, 0.6875);
        CEILING_EAST_POWERED_SHAPE = VoxelShapes.combine(CEILING_EAST_SHAPE, VoxelShapes.cuboid(0.5, 0.0, 0.0, 0.5625, 0.875, 1.0), BooleanBiFunction.OR);
        CEILING_WEST_POWERED_SHAPE = VoxelShapes.combine(CEILING_WEST_SHAPE, VoxelShapes.cuboid(0.4375, 0.0, 0.0, 0.5, 0.875, 1.0), BooleanBiFunction.OR);
        CEILING_NORTH_POWERED_SHAPE = VoxelShapes.combine(CEILING_NORTH_SHAPE, VoxelShapes.cuboid(0.0, 0.0, 0.4375, 1.0, 1.0, 0.5), BooleanBiFunction.OR);
        CEILING_SOUTH_POWERED_SHAPE = VoxelShapes.combine(CEILING_SOUTH_SHAPE, VoxelShapes.cuboid(0.0, 0.0, 0.5, 1.0, 1.0, 0.5625), BooleanBiFunction.OR);
        FLOOR_EAST_SHAPE = VoxelShapes.cuboid(0.375, 0.0, 0.0, 0.6875, 0.125, 1.0);
        FLOOR_WEST_SHAPE = VoxelShapes.cuboid(0.3125, 0.0, 0.0, 0.625, 0.125, 1.0);
        FLOOR_NORTH_SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.3125, 1.0, 0.125, 0.625);
        FLOOR_SOUTH_SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.375, 1.0, 0.125, 0.6875);
        FLOOR_EAST_POWERED_SHAPE = VoxelShapes.combine(FLOOR_EAST_SHAPE, VoxelShapes.cuboid(0.5, 0.0, 0.0, 0.5625, 1.0, 1.0), BooleanBiFunction.OR);
        FLOOR_WEST_POWERED_SHAPE = VoxelShapes.combine(FLOOR_WEST_SHAPE, VoxelShapes.cuboid(0.4375, 0.0, 0.0, 0.5, 1.0, 1.0), BooleanBiFunction.OR);
        FLOOR_NORTH_POWERED_SHAPE = VoxelShapes.combine(FLOOR_NORTH_SHAPE, VoxelShapes.cuboid(0.0, 0.0, 0.4375, 1.0, 1.0, 0.5), BooleanBiFunction.OR);
        FLOOR_SOUTH_POWERED_SHAPE = VoxelShapes.combine(FLOOR_SOUTH_SHAPE, VoxelShapes.cuboid(0.0, 0.0, 0.5, 1.0, 1.0, 0.5625), BooleanBiFunction.OR);
        NORTH_SHAPE = VoxelShapes.cuboid(0.0, 0.3125, 0.875, 1.0, 0.625, 1.0);
        SOUTH_SHAPE = VoxelShapes.cuboid(0.0, 0.3125, 0.0, 1.0, 0.625, 0.125);
        WEST_SHAPE = VoxelShapes.cuboid(0.875, 0.3125, 0.0, 1.0, 0.625, 1.0);
        EAST_SHAPE = VoxelShapes.cuboid(0.0, 0.3125, 0.0, 0.125, 0.625, 1.0);
        NORTH_POWERED_SHAPE = VoxelShapes.combine(NORTH_SHAPE, VoxelShapes.cuboid(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0), BooleanBiFunction.OR);
        SOUTH_POWERED_SHAPE = VoxelShapes.combine(SOUTH_SHAPE, VoxelShapes.cuboid(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0), BooleanBiFunction.OR);
        WEST_POWERED_SHAPE = VoxelShapes.combine(WEST_SHAPE, VoxelShapes.cuboid(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0), BooleanBiFunction.OR);
        EAST_POWERED_SHAPE = VoxelShapes.combine(EAST_SHAPE, VoxelShapes.cuboid(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0), BooleanBiFunction.OR);
    }
}
