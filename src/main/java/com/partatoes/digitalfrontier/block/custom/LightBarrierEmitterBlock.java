package com.partatoes.digitalfrontier.block.custom;

import com.mojang.serialization.MapCodec;
import com.partatoes.digitalfrontier.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class LightBarrierEmitterBlock extends FaceAttachedHorizontalDirectionalBlock {
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

    public LightBarrierEmitterBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(POWERED, false)).setValue(FACE, AttachFace.WALL));
    }

    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return null;
    }

    public void GenerateBridge(Direction dir, LevelAccessor world, int length, BlockPos pos, BlockState state) {
        Boolean bridgeEnded = false;

        for(int i = 1; i < 15; ++i) {
            BlockPos addPos;
            BlockState bs;
            addPos = new BlockPos(0, 0, 0);
            bs = (BlockState)((BlockState) ModBlocks.LIGHT_BARRIER.defaultBlockState().setValue(FACING, dir)).setValue(FACE, (AttachFace)state.getValue(FACE));
            label30:
            switch ((AttachFace)state.getValue(FACE)) {
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

            if (!world.getBlockState(pos.offset(addPos)).isAir() && !world.getBlockState(pos.offset(addPos)).equals(bs)) {
                return;
            }

            if (!bridgeEnded && i < length) {
                world.setBlock(pos.offset(addPos), bs, 3);
            } else {
                bridgeEnded = true;
                world.setBlock(pos.offset(addPos), Blocks.AIR.defaultBlockState(), 3);
            }
        }

    }

    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        if (!world.isClientSide()) {
            int redstonePower = world.getBestNeighborSignal(pos);
            Direction dir = (Direction)state.getValue(FACING);
            boolean bl = (Boolean)state.getValue(POWERED);
            if (bl != world.hasNeighborSignal(pos)) {
                if (bl) {
                    world.scheduleTick(pos, this, 4);
                } else {
                    world.setBlock(pos, (BlockState)state.cycle(POWERED), 2);
//                    world.playSound((PlayerEntity)null, pos, LaserBridgesDoors.ON, SoundCategory.BLOCKS, this.volume, this.pitch);
                    this.GenerateBridge(dir, world, redstonePower, pos, state);
                }
            } else if (bl) {
                world.scheduleTick(pos, this, 4);
            }

        }
    }

    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!world.isClientSide()) {
            if ((Boolean)state.getValue(POWERED)) {
                Direction dir = (Direction)state.getValue(FACING);
                if (!world.hasNeighborSignal(pos)) {
                    world.setBlock(pos, (BlockState)state.cycle(POWERED), 2);
                    this.GenerateBridge(dir, world, 0, pos, state);
//                    world.playSound((PlayerEntity)null, pos, LaserBridgesDoors.OFF, SoundCategory.BLOCKS, this.volume, this.pitch);
                } else {
                    int redstonePower = world.getBestNeighborSignal(pos);
                    this.GenerateBridge(dir, world, redstonePower, pos, state);
                }
            }

        }
    }

    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (!world.isClientSide()) {
            boolean bl = (Boolean)state.getValue(POWERED);
            if (bl != world.hasNeighborSignal(pos)) {
                if (bl) {
                    world.scheduleTick(pos, this, 4);
                } else {
                    world.setBlock(pos, (BlockState)state.cycle(POWERED), 2);
//                    world.playSound((PlayerEntity)null, pos, LaserBridgesDoors.ON, SoundCategory.BLOCKS, this.volume, this.pitch);
                    Direction dir = (Direction)state.getValue(FACING);
                    int redstonePower = world.getBestNeighborSignal(pos);
                    this.GenerateBridge(dir, world, redstonePower, pos, state);
                }
            }

        }
    }

    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide()) {
            world.playSound((Player)null, pos, SoundEvents.METAL_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F);
        }

        Direction dir = (Direction)state.getValue(FACING);
        this.GenerateBridge(dir, world, 0, pos, state);
//        world.playSound((PlayerEntity)null, pos, LaserBridgesDoors.OFF, SoundCategory.BLOCKS, this.volume, this.pitch);
        return state;
    }

    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        Direction dir = (Direction)state.getValue(FACING);
        if (!state.canSurvive(world, pos)) {
            this.GenerateBridge(dir, (LevelAccessor)world, 0, pos, state);
//            ((World)world).playSound((PlayerEntity)null, pos, LaserBridgesDoors.OFF, SoundCategory.BLOCKS, this.volume, this.pitch);
            return Blocks.AIR.defaultBlockState();
        } else {
            return state;
        }
    }

    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        Direction dir = (Direction)state.getValue(FACING);
        boolean powered = (Boolean)state.getValue(POWERED);
        switch ((AttachFace)state.getValue(FACE)) {
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

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, FACE, POWERED});
    }

    static {
        FACING = BlockStateProperties.HORIZONTAL_FACING;
        POWERED = BlockStateProperties.POWERED;
        CEILING_EAST_SHAPE = Shapes.box(0.375, 0.875, 0.0, 0.6875, 1.0, 1.0);
        CEILING_WEST_SHAPE = Shapes.box(0.3125, 0.875, 0.0, 0.625, 1.0, 1.0);
        CEILING_NORTH_SHAPE = Shapes.box(0.0, 0.875, 0.3125, 1.0, 1.0, 0.625);
        CEILING_SOUTH_SHAPE = Shapes.box(0.0, 0.875, 0.375, 1.0, 1.0, 0.6875);
        CEILING_EAST_POWERED_SHAPE = Shapes.joinUnoptimized(CEILING_EAST_SHAPE, Shapes.box(0.5, 0.0, 0.0, 0.5625, 0.875, 1.0), BooleanOp.OR);
        CEILING_WEST_POWERED_SHAPE = Shapes.joinUnoptimized(CEILING_WEST_SHAPE, Shapes.box(0.4375, 0.0, 0.0, 0.5, 0.875, 1.0), BooleanOp.OR);
        CEILING_NORTH_POWERED_SHAPE = Shapes.joinUnoptimized(CEILING_NORTH_SHAPE, Shapes.box(0.0, 0.0, 0.4375, 1.0, 1.0, 0.5), BooleanOp.OR);
        CEILING_SOUTH_POWERED_SHAPE = Shapes.joinUnoptimized(CEILING_SOUTH_SHAPE, Shapes.box(0.0, 0.0, 0.5, 1.0, 1.0, 0.5625), BooleanOp.OR);
        FLOOR_EAST_SHAPE = Shapes.box(0.375, 0.0, 0.0, 0.6875, 0.125, 1.0);
        FLOOR_WEST_SHAPE = Shapes.box(0.3125, 0.0, 0.0, 0.625, 0.125, 1.0);
        FLOOR_NORTH_SHAPE = Shapes.box(0.0, 0.0, 0.3125, 1.0, 0.125, 0.625);
        FLOOR_SOUTH_SHAPE = Shapes.box(0.0, 0.0, 0.375, 1.0, 0.125, 0.6875);
        FLOOR_EAST_POWERED_SHAPE = Shapes.joinUnoptimized(FLOOR_EAST_SHAPE, Shapes.box(0.5, 0.0, 0.0, 0.5625, 1.0, 1.0), BooleanOp.OR);
        FLOOR_WEST_POWERED_SHAPE = Shapes.joinUnoptimized(FLOOR_WEST_SHAPE, Shapes.box(0.4375, 0.0, 0.0, 0.5, 1.0, 1.0), BooleanOp.OR);
        FLOOR_NORTH_POWERED_SHAPE = Shapes.joinUnoptimized(FLOOR_NORTH_SHAPE, Shapes.box(0.0, 0.0, 0.4375, 1.0, 1.0, 0.5), BooleanOp.OR);
        FLOOR_SOUTH_POWERED_SHAPE = Shapes.joinUnoptimized(FLOOR_SOUTH_SHAPE, Shapes.box(0.0, 0.0, 0.5, 1.0, 1.0, 0.5625), BooleanOp.OR);
        NORTH_SHAPE = Shapes.box(0.0, 0.3125, 0.875, 1.0, 0.625, 1.0);
        SOUTH_SHAPE = Shapes.box(0.0, 0.3125, 0.0, 1.0, 0.625, 0.125);
        WEST_SHAPE = Shapes.box(0.875, 0.3125, 0.0, 1.0, 0.625, 1.0);
        EAST_SHAPE = Shapes.box(0.0, 0.3125, 0.0, 0.125, 0.625, 1.0);
        NORTH_POWERED_SHAPE = Shapes.joinUnoptimized(NORTH_SHAPE, Shapes.box(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0), BooleanOp.OR);
        SOUTH_POWERED_SHAPE = Shapes.joinUnoptimized(SOUTH_SHAPE, Shapes.box(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0), BooleanOp.OR);
        WEST_POWERED_SHAPE = Shapes.joinUnoptimized(WEST_SHAPE, Shapes.box(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0), BooleanOp.OR);
        EAST_POWERED_SHAPE = Shapes.joinUnoptimized(EAST_SHAPE, Shapes.box(0.0, 0.4375, 0.0, 1.0, 0.5, 1.0), BooleanOp.OR);
    }
}
