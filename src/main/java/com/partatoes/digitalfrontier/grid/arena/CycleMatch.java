package com.partatoes.digitalfrontier.grid.arena;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.ModVehicles;
import com.partatoes.digitalfrontier.entity.custom.ChampionEntity;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleEntity;
import com.partatoes.digitalfrontier.grid.layout.ArenaGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The light cycle match: the challenger and one to three programs (more, the higher the champion's level) on arena
 * cycles that run flat out and lay walls of light behind them, turning a quarter at a time. Riding into a wall, off
 * the floor or out of the saddle ends a rider's round; the last rider left wins. The programs steer by
 * {@link CycleSteering}, keener and further-sighted at higher levels.
 */
public class CycleMatch extends Match {
    private static final int[][] OPPONENT_STARTS = {{0, 10, CycleSteering.NORTH}, {-10, 0, CycleSteering.EAST}, {10, 0, CycleSteering.WEST}};

    @Nullable
    private LightCycleEntity challengerCycle;
    private final List<LightCycleEntity> programCycles = new ArrayList<>();
    private final List<ChampionEntity> riders = new ArrayList<>();
    // Where each program last chose its way, so it chooses once per block
    private final Map<Integer, BlockPos> decidedAt = new HashMap<>();

    public CycleMatch(ServerLevel world, ArenaGeometry arena, ServerPlayer challenger, int level, boolean conscripted) {
        super(world, arena, ArenaGame.LIGHT_CYCLE, challenger, level, conscripted);
    }

    @Override
    protected void setUp(ServerPlayer player) {
        clearTrails();
        BlockPos start = arena.challengerStart();
        challengerCycle = cycle(start.getX() + 0.5, start.getZ() + 0.5, CycleSteering.SOUTH);
        if (challengerCycle != null) {
            player.teleportTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
            player.startRiding(challengerCycle, true, false);
        }
        for (int i = 0; i < Math.min(OPPONENT_STARTS.length, level); i++) {
            int[] spot = OPPONENT_STARTS[i];
            LightCycleEntity cycle = cycle(arena.centerX() + spot[0] + 0.5, arena.centerZ() + spot[1] + 0.5, spot[2]);
            ChampionEntity rider = ModEntities.CHAMPION.create(world, EntitySpawnReason.EVENT);
            if (cycle == null || rider == null) {
                continue;
            }
            rider.prepare(level, ChampionEntity.Mode.RIDER);
            rider.snapTo(cycle.getX(), cycle.getY(), cycle.getZ(), cycle.getYRot(), 0);
            world.addFreshEntity(rider);
            rider.startRiding(cycle, true, false);
            programCycles.add(cycle);
            riders.add(rider);
        }
    }

    @Nullable
    private LightCycleEntity cycle(double x, double z, int heading) {
        LightCycleEntity cycle = ModVehicles.LIGHTCYCLE.create(world, EntitySpawnReason.EVENT);
        if (cycle == null) {
            return null;
        }
        cycle.snapTo(x, ArenaGeometry.FLOOR + 1, z, CycleSteering.yawOf(heading), 0);
        cycle.setArena(LightCycleEntity.ARENA_WAITING);
        world.addFreshEntity(cycle);
        return cycle;
    }

    @Override
    protected void play(ServerPlayer player) {
        if (ticks == COUNTDOWN + 1) {
            if (challengerCycle != null) {
                challengerCycle.setArena(LightCycleEntity.ARENA_RACING);
            }
            programCycles.forEach(cycle -> cycle.setArena(LightCycleEntity.ARENA_RACING));
        }
        if (challengerCycle == null || challengerCycle.isRemoved() || player.getVehicle() != challengerCycle) {
            ArenaManager.announce(world, arena, Component.translatable("arena.digitalfrontier.derezzed", player.getDisplayName()));
            finish(false);
            return;
        }
        programCycles.removeIf(LightCycleEntity::isRemoved);
        if (programCycles.isEmpty()) {
            finish(true);
        }
    }

    /** A program's turn at the controls of its cycle, once per block it reaches. */
    public static void steer(LightCycleEntity cycle) {
        if (!(cycle.level() instanceof ServerLevel world) || !(ArenaManager.match(world) instanceof CycleMatch match)) {
            return;
        }
        BlockPos cell = cycle.blockPosition();
        if (cell.equals(match.decidedAt.get(cycle.getId()))) {
            return;
        }
        // Wait until it's near the middle of the block, so its turns line up
        double offX = cycle.getX() - (cell.getX() + 0.5);
        double offZ = cycle.getZ() - (cell.getZ() + 0.5);
        if (offX * offX + offZ * offZ > 0.06) {
            return;
        }
        match.decidedAt.put(cycle.getId(), cell);
        ServerPlayer target = match.challenger();
        int targetX = target != null ? target.getBlockX() : match.arena.centerX();
        int targetZ = target != null ? target.getBlockZ() : match.arena.centerZ();
        int y = cell.getY();
        CycleSteering.Turn turn = CycleSteering.choose((x, z) -> {
            if (!match.arena.onFloor(x, z)) {
                return true;
            }
            BlockState state = world.getBlockState(new BlockPos(x, y, z));
            return state.is(ModBlocks.TRAIL_WALL) || state.isSolid();
        }, cell.getX(), cell.getZ(), CycleSteering.headingOf(cycle.getYRot()), targetX, targetZ, 5 + 2 * match.level, 0.25 * match.level);
        if (turn != CycleSteering.Turn.STRAIGHT) {
            cycle.turn(turn);
        }
    }

    /** Whether an arena cycle has left the floor (or fallen through it). */
    public static boolean offFloor(ServerLevel world, LightCycleEntity cycle) {
        if (!(ArenaManager.match(world) instanceof CycleMatch match)) {
            return false;
        }
        return !match.arena.onFloor(cycle.getBlockX(), cycle.getBlockZ()) || cycle.getY() < ArenaGeometry.FLOOR + 0.5;
    }

    /** A cycle has crashed: it derezzes, and its rider is out of the round. */
    public static void crashed(ServerLevel world, LightCycleEntity cycle) {
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK, cycle.getX(), cycle.getY() + 0.5, cycle.getZ(), 30, 0.5, 0.5, 0.5, 0.2);
        world.playSound(null, cycle.getX(), cycle.getY(), cycle.getZ(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.5f, 0.8f);
        if (cycle.getControllingPassenger() instanceof ChampionEntity rider) {
            rider.stopRiding();
            rider.discard();
            if (ArenaManager.match(world) instanceof CycleMatch match) {
                ArenaManager.announce(world, match.arena, Component.translatable("arena.digitalfrontier.derezzed", rider.getDisplayName()));
            }
        } else {
            cycle.ejectPassengers();
        }
        cycle.discard();
    }

    // Every wall of light left on the floor
    private void clearTrails() {
        int floor = ArenaGeometry.FLOOR;
        for (int dx = -ArenaGeometry.PLAY; dx <= ArenaGeometry.PLAY; dx++) {
            for (int dz = -ArenaGeometry.PLAY; dz <= ArenaGeometry.PLAY; dz++) {
                for (int dy = 1; dy <= 2; dy++) {
                    BlockPos at = new BlockPos(arena.centerX() + dx, floor + dy, arena.centerZ() + dz);
                    if (world.getBlockState(at).is(ModBlocks.TRAIL_WALL)) {
                        world.removeBlock(at, false);
                    }
                }
            }
        }
    }

    @Override
    protected void cleanUp() {
        ServerPlayer player = challenger();
        if (player != null) {
            player.stopRiding();
        }
        if (challengerCycle != null) {
            challengerCycle.discard();
        }
        programCycles.forEach(LightCycleEntity::discard);
        riders.forEach(rider -> {
            if (rider.isAlive()) {
                rider.discard();
            }
        });
        clearTrails();
    }
}
