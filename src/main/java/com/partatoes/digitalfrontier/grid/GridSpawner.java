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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
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
            if (server.getTickCount() % INTERVAL != 0) {
                return;
            }
            for (ServerLevel world : server.getAllLevels()) {
                GridChunkGenerator generator = GridArrival.generator(world);
                if (generator != null && world.getGameRules().get(GameRules.SPAWN_MOBS)) {
                    world.getPlayers(player -> !player.isSpectator()).forEach(player -> trySpawn(world, generator, player));
                }
            }
        });
    }

    private static void trySpawn(ServerLevel world, GridChunkGenerator generator, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(MAX_DISTANCE);
        int nearby = world.getEntitiesOfClass(Mob.class, area,
                mob -> mob instanceof ProgramEntity || mob instanceof CorruptedProgramEntity || mob instanceof EnforcerEntity).size();
        if (nearby >= CAP) {
            return;
        }
        double angle = world.random.nextDouble() * Math.PI * 2;
        double distance = MIN_DISTANCE + world.random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
        if (!world.hasChunk(x >> 4, z >> 4)) {
            return;
        }
        BlockPos pos = new BlockPos(x, world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        if (!world.getBlockState(pos.below()).isRedstoneConductor(world, pos.below())
                || !world.getBlockState(pos).isAir() || !world.getBlockState(pos.above()).isAir()) {
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

    private static void spawnPatrol(ServerLevel world, BlockPos pos, int size) {
        for (int i = 0; i < size; i++) {
            spawn(world, ModEntities.ENFORCER, pos.offset(world.random.nextInt(3) - 1, 0, world.random.nextInt(3) - 1), enforcer -> { });
        }
    }

    private static <T extends Mob> void spawn(ServerLevel world, EntityType<T> type, BlockPos pos, Consumer<T> setup) {
        T mob = type.create(world, EntitySpawnReason.NATURAL);
        if (mob == null) {
            return;
        }
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, world.random.nextFloat() * 360, 0);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        setup.accept(mob);
        world.addFreshEntity(mob);
    }
}
