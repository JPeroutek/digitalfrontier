package com.partatoes.digitalfrontier.grid;

import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.custom.CorruptedProgramEntity;
import com.partatoes.digitalfrontier.entity.custom.EnforcerEntity;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.layout.ChunkPlan;
import com.partatoes.digitalfrontier.grid.layout.Zone;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;

import java.util.function.Consumer;

/**
 * Who appears around players in a grid depends on the corruption tier where they'd stand:
 * <ul>
 *   <li>CLEAN / LOW: programs, in the city, waypoints and outskirts.</li>
 *   <li>UNSTABLE: corrupted programs, which glitch away when hit.</li>
 *   <li>CORRUPTED: corrupted programs that fight back, and enforcer patrols.</li>
 *   <li>SEVERE: enforcer patrols. Lieutenants never spawn this way; their strongholds bring them.</li>
 * </ul>
 * Grids don't use biome spawn lists: the tier is exact to the chunk, where biomes only mark the strongest corruption.
 */
public final class GridSpawner {
    private static final int INTERVAL = 100;
    private static final int MIN_DISTANCE = 24;
    private static final int MAX_DISTANCE = 48;
    /** Grid mobs allowed within {@link #MAX_DISTANCE} of a player before no more spawn near them. */
    private static final int CAP = 8;

    private GridSpawner() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % INTERVAL != 0) {
                return;
            }
            for (ServerWorld world : server.getWorlds()) {
                GridChunkGenerator generator = GridArrival.generator(world);
                if (generator != null && world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
                    world.getPlayers(player -> !player.isSpectator()).forEach(player -> trySpawn(world, generator, player));
                }
            }
        });
    }

    private static void trySpawn(ServerWorld world, GridChunkGenerator generator, ServerPlayerEntity player) {
        Box area = player.getBoundingBox().expand(MAX_DISTANCE);
        int nearby = world.getEntitiesByClass(MobEntity.class, area,
                mob -> mob instanceof ProgramEntity || mob instanceof CorruptedProgramEntity || mob instanceof EnforcerEntity).size();
        if (nearby >= CAP) {
            return;
        }
        double angle = world.random.nextDouble() * Math.PI * 2;
        double distance = MIN_DISTANCE + world.random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }
        BlockPos pos = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z), z);
        if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
                || !world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) {
            return;
        }

        ChunkPlan plan = generator.layout().planAt(x >> 4, z >> 4);
        boolean hostilesAllowed = world.getDifficulty() != Difficulty.PEACEFUL;
        switch (plan.tier()) {
            case CLEAN, LOW -> {
                if (plan.zone() == Zone.CITY_CORE || plan.zone() == Zone.CITY || plan.zone() == Zone.WAYPOINT || plan.zone() == Zone.OUTSKIRTS) {
                    spawn(world, ModEntities.PROGRAM, pos, program -> { });
                }
            }
            case UNSTABLE -> spawn(world, ModEntities.CORRUPTED_PROGRAM, pos, program -> program.setProvokable(false));
            case CORRUPTED -> {
                if (!hostilesAllowed || world.random.nextFloat() < 0.6f) {
                    spawn(world, ModEntities.CORRUPTED_PROGRAM, pos, program -> program.setProvokable(true));
                } else {
                    spawnPatrol(world, pos, 2 + world.random.nextInt(2));
                }
            }
            case SEVERE -> {
                if (hostilesAllowed) {
                    spawnPatrol(world, pos, 2 + world.random.nextInt(3));
                }
            }
        }
    }

    private static void spawnPatrol(ServerWorld world, BlockPos pos, int size) {
        for (int i = 0; i < size; i++) {
            spawn(world, ModEntities.ENFORCER, pos.add(world.random.nextInt(3) - 1, 0, world.random.nextInt(3) - 1), enforcer -> { });
        }
    }

    private static <T extends MobEntity> void spawn(ServerWorld world, EntityType<T> type, BlockPos pos, Consumer<T> setup) {
        T mob = type.create(world, SpawnReason.NATURAL);
        if (mob == null) {
            return;
        }
        mob.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, world.random.nextFloat() * 360, 0);
        mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.NATURAL, null);
        setup.accept(mob);
        world.spawnEntity(mob);
    }
}
