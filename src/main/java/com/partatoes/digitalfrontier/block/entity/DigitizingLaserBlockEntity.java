package com.partatoes.digitalfrontier.block.entity;

import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.arrival.TerminalRef;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

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
        markDirty();
    }

    /** The linked terminal, linking to the default grid's nearest terminal first if there's none yet. */
    public Optional<TerminalRef> linkOrNearest(ServerWorld world) {
        if (link == null) {
            GridArrival.nearestTerminal(world.getServer(), ModDimensions.DEFAULT_GRID_ID, pos).ifPresent(this::setLink);
        }
        return Optional.ofNullable(link);
    }

    public boolean isScanning() {
        return scanning != null;
    }

    public void startScan(ServerPlayerEntity player) {
        scanning = player.getUuid();
        scanTicks = 0;
        player.getServerWorld().playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.0f, 1.2f);
    }

    public static void tick(World world, BlockPos pos, BlockState state, DigitizingLaserBlockEntity laser) {
        if (laser.scanning == null || !(world instanceof ServerWorld serverWorld)) {
            return;
        }
        ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(laser.scanning);
        Vec3d emitter = Vec3d.ofCenter(pos).add(0, 0.5, 0);
        if (player == null || player.getServerWorld() != serverWorld || !player.isAlive()
                || player.squaredDistanceTo(emitter) > MAX_RANGE * MAX_RANGE) {
            laser.scanning = null;
            if (player != null) {
                player.sendMessage(Text.translatable("message.digitalfrontier.scan_interrupted").formatted(Formatting.RED), true);
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
            player.sendMessage(Text.translatable("message.digitalfrontier.terminal_unreachable").formatted(Formatting.RED), true);
        }
    }

    // A beam from the emitter to the player, and a ring sweeping up the player as the scan progresses
    private static void spawnScanParticles(ServerWorld world, Vec3d emitter, ServerPlayerEntity player, float progress) {
        Vec3d target = player.getPos().add(0, player.getHeight() * progress, 0);
        Vec3d beam = target.subtract(emitter);
        int steps = Math.max(1, (int) (beam.length() * 4));
        for (int i = 0; i <= steps; i++) {
            Vec3d point = emitter.add(beam.multiply((double) i / steps));
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            world.spawnParticles(ParticleTypes.END_ROD, target.x + Math.cos(angle) * 0.6, target.y, target.z + Math.sin(angle) * 0.6,
                    1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        if (link != null) {
            TerminalRef.CODEC.encodeStart(NbtOps.INSTANCE, link).ifSuccess(encoded -> nbt.put(LINK_KEY, encoded));
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        link = nbt.contains(LINK_KEY)
                ? TerminalRef.CODEC.parse(NbtOps.INSTANCE, nbt.get(LINK_KEY)).result().orElse(null)
                : null;
    }
}
