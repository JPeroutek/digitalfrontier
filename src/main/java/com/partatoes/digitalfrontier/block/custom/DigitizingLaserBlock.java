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
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Digitizes the player who uses it and sends them to its linked grid terminal. Use it with a Laser Tuner to
 * cycle the link through the nearest terminal and the terminals the player has discovered. Doesn't work
 * inside a grid. The tunable version (the Grid Tuning Laser) can also be tuned to any grid through a screen.
 */
public class DigitizingLaserBlock extends BlockWithEntity {
    public static final MapCodec<DigitizingLaserBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("tunable").forGetter(DigitizingLaserBlock::isTunable),
            createSettingsCodec()
    ).apply(instance, DigitizingLaserBlock::new));

    // The Grid Tuning Laser: sneak-use opens a screen to tune it to a different grid
    private final boolean tunable;

    public DigitizingLaserBlock(boolean tunable, Settings settings) {
        super(settings);
        this.tunable = tunable;
    }

    public boolean isTunable() {
        return tunable;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DigitizingLaserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, ModBlockEntities.DIGITIZING_LASER, DigitizingLaserBlockEntity::tick);
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!stack.isOf(ModItems.LASER_TUNER)) {
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        }
        if (world instanceof ServerWorld serverWorld && player instanceof ServerPlayerEntity serverPlayer
                && serverWorld.getBlockEntity(pos) instanceof DigitizingLaserBlockEntity laser && usable(serverWorld, serverPlayer)) {
            tune(serverWorld, serverPlayer, laser);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)
                || !(serverWorld.getBlockEntity(pos) instanceof DigitizingLaserBlockEntity laser)) {
            return ActionResult.SUCCESS;
        }
        if (!usable(serverWorld, serverPlayer) || laser.isScanning()) {
            return ActionResult.SUCCESS;
        }
        if (tunable && serverPlayer.isSneaking()) {
            GridTuningNetworking.openScreen(serverPlayer, laser);
            return ActionResult.SUCCESS;
        }
        Optional<TerminalRef> link = laser.linkOrNearest(serverWorld);
        if (link.isEmpty()) {
            serverPlayer.sendMessage(Text.translatable("message.digitalfrontier.terminal_unreachable").formatted(Formatting.RED), true);
            return ActionResult.SUCCESS;
        }
        laser.startScan(serverPlayer);
        serverPlayer.sendMessage(Text.translatable("message.digitalfrontier.digitizing", GridArrival.terminalName(link.get().terminal())), true);
        return ActionResult.SUCCESS;
    }

    private static boolean usable(ServerWorld world, ServerPlayerEntity player) {
        if (ModDimensions.isGrid(world)) {
            player.sendMessage(Text.translatable("message.digitalfrontier.laser_inside_grid").formatted(Formatting.RED), true);
            return false;
        }
        return true;
    }

    // Cycles: the terminal nearest the laser, then every terminal the player has discovered in the same grid
    private static void tune(ServerWorld world, ServerPlayerEntity player, DigitizingLaserBlockEntity laser) {
        // A laser tuned to a grid that has since been deleted falls back to the default grid
        if (laser.link() != null && GridArrival.gridWorld(world.getServer(), laser.link().grid()).isEmpty()) {
            GridArrival.nearestTerminal(world.getServer(), ModDimensions.DEFAULT_GRID_ID, laser.getPos()).ifPresent(laser::setLink);
        }
        Optional<TerminalRef> current = laser.linkOrNearest(world);
        if (current.isEmpty()) {
            player.sendMessage(Text.translatable("message.digitalfrontier.terminal_unreachable").formatted(Formatting.RED), true);
            return;
        }
        Identifier grid = current.get().grid();
        List<String> options = new ArrayList<>();
        GridArrival.nearestTerminal(world.getServer(), grid, laser.getPos()).ifPresent(nearest -> options.add(nearest.terminal()));
        player.getAttachedOrCreate(GridMemory.TYPE).discovered(grid).stream()
                .filter(id -> !options.contains(id))
                .forEach(options::add);
        if (options.isEmpty()) {
            options.add(current.get().terminal());
        }

        int index = options.indexOf(current.get().terminal());
        TerminalRef next = new TerminalRef(grid, options.get((index + 1) % options.size()));
        laser.setLink(next);
        world.playSound(null, laser.getPos(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 0.8f,
                0.8f + 0.1f * (options.indexOf(next.terminal())));
        player.sendMessage(Text.translatable("message.digitalfrontier.laser_tuned", GridArrival.terminalName(next.terminal()),
                options.indexOf(next.terminal()) + 1, options.size()), true);
    }
}
