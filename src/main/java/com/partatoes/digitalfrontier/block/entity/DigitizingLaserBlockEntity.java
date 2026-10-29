package com.partatoes.digitalfrontier.block.entity;

import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.arrival.TerminalRef;
import com.partatoes.digitalfrontier.world.ModDimensions;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Remembers which grid terminal a Digitizing Laser sends players to, and runs the scan that digitizes a player.
 * An unlinked laser links itself to the default grid's terminal nearest to it the first time it's used.
 */
public class DigitizingLaserBlockEntity extends BlockEntity {
    public static final int SCAN_TICKS = 40;
    private static final double MAX_RANGE = 4.0;
    private static final String LINK_KEY = "link";

    @Nullable
    private TerminalRef link;
    // The scan in progress isn't saved: it's only a couple of seconds long
    @Nullable
    private UUID scanning;
    private int scanTicks;

    public DigitizingLaserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIGITIZING_LASER, pos, state);
    }

    @Nullable
    public TerminalRef link() {
        return link;
    }

    public void setLink(TerminalRef link) {
        this.link = link;
        setChanged();
    }

    /** The linked terminal, linking to the default grid's nearest terminal first if there's none yet. */
    public Optional<TerminalRef> linkOrNearest(ServerLevel world) {
        if (link == null) {
            GridArrival.nearestTerminal(world.getServer(), ModDimensions.DEFAULT_GRID_ID, worldPosition).ifPresent(this::setLink);
        }
        return Optional.ofNullable(link);
    }

    public boolean isScanning() {
        return scanning != null;
    }

    public void startScan(ServerPlayer player) {
        scanning = player.getUUID();
        scanTicks = 0;
        player.level().playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0f, 1.2f);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, DigitizingLaserBlockEntity laser) {
        if (laser.scanning == null || !(world instanceof ServerLevel serverWorld)) {
            return;
        }
        ServerPlayer player = serverWorld.getServer().getPlayerList().getPlayer(laser.scanning);
        Vec3 emitter = Vec3.atCenterOf(pos).add(0, 0.5, 0);
        if (player == null || player.level() != serverWorld || !player.isAlive()
                || player.distanceToSqr(emitter) > MAX_RANGE * MAX_RANGE) {
            laser.scanning = null;
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("message.digitalfrontier.scan_interrupted").withStyle(ChatFormatting.RED));
            }
            return;
        }

        laser.scanTicks++;
        spawnScanParticles(serverWorld, emitter, player, (float) laser.scanTicks / SCAN_TICKS);
        if (laser.scanTicks < SCAN_TICKS) {
            return;
        }

        laser.scanning = null;
        TerminalRef target = laser.link;
        if (target == null || !GridArrival.teleport(player, target)) {
            player.sendOverlayMessage(Component.translatable("message.digitalfrontier.terminal_unreachable").withStyle(ChatFormatting.RED));
        }
    }

    // A beam from the emitter to the player, and a ring sweeping up the player as the scan progresses
    private static void spawnScanParticles(ServerLevel world, Vec3 emitter, ServerPlayer player, float progress) {
        Vec3 target = player.position().add(0, player.getBbHeight() * progress, 0);
        Vec3 beam = target.subtract(emitter);
        int steps = Math.max(1, (int) (beam.length() * 4));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = emitter.add(beam.scale((double) i / steps));
            world.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            world.sendParticles(ParticleTypes.END_ROD, target.x + Math.cos(angle) * 0.6, target.y, target.z + Math.sin(angle) * 0.6,
                    1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.storeNullable(LINK_KEY, TerminalRef.CODEC, link);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        link = view.read(LINK_KEY, TerminalRef.CODEC).orElse(null);
    }
}
