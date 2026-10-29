package com.partatoes.digitalfrontier.block.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.block.entity.DigitizingLaserBlockEntity;
import com.partatoes.digitalfrontier.block.entity.ModBlockEntities;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.arrival.GridMemory;
import com.partatoes.digitalfrontier.grid.arrival.TerminalRef;
import com.partatoes.digitalfrontier.grid.tuning.GridTuningNetworking;
import com.partatoes.digitalfrontier.item.ModItems;
import com.partatoes.digitalfrontier.world.ModDimensions;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Digitizes the player who uses it and sends them to its linked grid terminal. Use it with a Laser Tuner to
 * cycle the link through the nearest terminal and the terminals the player has discovered. Doesn't work
 * inside a grid. The tunable version (the Grid Tuning Laser) can also be tuned to any grid through a screen.
 */
public class DigitizingLaserBlock extends BaseEntityBlock {
    public static final MapCodec<DigitizingLaserBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("tunable").forGetter(DigitizingLaserBlock::isTunable),
            propertiesCodec()
    ).apply(instance, DigitizingLaserBlock::new));

    // The Grid Tuning Laser: sneak-use opens a screen to tune it to a different grid
    private final boolean tunable;

    public DigitizingLaserBlock(boolean tunable, Properties settings) {
        super(settings);
        this.tunable = tunable;
    }

    public boolean isTunable() {
        return tunable;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DigitizingLaserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.DIGITIZING_LASER, DigitizingLaserBlockEntity::tick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.LASER_TUNER)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (world instanceof ServerLevel serverWorld && player instanceof ServerPlayer serverPlayer
                && serverWorld.getBlockEntity(pos) instanceof DigitizingLaserBlockEntity laser && usable(serverWorld, serverPlayer)) {
            tune(serverWorld, serverPlayer, laser);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(world instanceof ServerLevel serverWorld) || !(player instanceof ServerPlayer serverPlayer)
                || !(serverWorld.getBlockEntity(pos) instanceof DigitizingLaserBlockEntity laser)) {
            return InteractionResult.SUCCESS;
        }
        if (!usable(serverWorld, serverPlayer) || laser.isScanning()) {
            return InteractionResult.SUCCESS;
        }
        if (tunable && serverPlayer.isShiftKeyDown()) {
            GridTuningNetworking.openScreen(serverPlayer, laser);
            return InteractionResult.SUCCESS;
        }
        Optional<TerminalRef> link = laser.linkOrNearest(serverWorld);
        if (link.isEmpty()) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.digitalfrontier.terminal_unreachable").withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        laser.startScan(serverPlayer);
        serverPlayer.sendOverlayMessage(Component.translatable("message.digitalfrontier.digitizing", GridArrival.terminalName(link.get().terminal())));
        return InteractionResult.SUCCESS;
    }

    private static boolean usable(ServerLevel world, ServerPlayer player) {
        if (ModDimensions.isGrid(world)) {
            player.sendOverlayMessage(Component.translatable("message.digitalfrontier.laser_inside_grid").withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    // Cycles: the terminal nearest the laser, then every terminal the player has discovered in the same grid
    private static void tune(ServerLevel world, ServerPlayer player, DigitizingLaserBlockEntity laser) {
        // A laser tuned to a grid that has since been deleted falls back to the default grid
        if (laser.link() != null && GridArrival.gridWorld(world.getServer(), laser.link().grid()).isEmpty()) {
            GridArrival.nearestTerminal(world.getServer(), ModDimensions.DEFAULT_GRID_ID, laser.getBlockPos()).ifPresent(laser::setLink);
        }
        Optional<TerminalRef> current = laser.linkOrNearest(world);
        if (current.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.digitalfrontier.terminal_unreachable").withStyle(ChatFormatting.RED));
            return;
        }
        Identifier grid = current.get().grid();
        List<String> options = new ArrayList<>();
        GridArrival.nearestTerminal(world.getServer(), grid, laser.getBlockPos()).ifPresent(nearest -> options.add(nearest.terminal()));
        player.getAttachedOrCreate(GridMemory.TYPE).discovered(grid).stream()
                .filter(id -> !options.contains(id))
                .forEach(options::add);
        if (options.isEmpty()) {
            options.add(current.get().terminal());
        }

        int index = options.indexOf(current.get().terminal());
        TerminalRef next = new TerminalRef(grid, options.get((index + 1) % options.size()));
        laser.setLink(next);
        world.playSound(null, laser.getBlockPos(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8f,
                0.8f + 0.1f * (options.indexOf(next.terminal())));
        player.sendOverlayMessage(Component.translatable("message.digitalfrontier.laser_tuned", GridArrival.terminalName(next.terminal()),
                options.indexOf(next.terminal()) + 1, options.size()));
    }
}
