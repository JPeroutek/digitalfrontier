package com.partatoes.digitalfrontier.grid;

import com.partatoes.digitalfrontier.grid.squad.SquadRoles;
import java.util.UUID;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.custom.BoolEntity;
import com.partatoes.digitalfrontier.entity.custom.CorruptedProgramEntity;
import com.partatoes.digitalfrontier.entity.custom.EnforcerEntity;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import com.partatoes.digitalfrontier.entity.custom.RecruiterEntity;
import com.partatoes.digitalfrontier.grid.regime.Regime;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.core.CoreManager;
import com.partatoes.digitalfrontier.grid.layout.ChunkPlan;
import com.partatoes.digitalfrontier.grid.layout.ControlTier;
import com.partatoes.digitalfrontier.grid.layout.CorruptionTier;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.grid.layout.Zone;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdState;
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
 * Who appears around players in a grid depends on where they'd stand. On the regime's ground (see
 * {@link #effectiveControl}):
 * <ul>
 *   <li>PATROLLED: pairs of enforcers, or programs going about their business. While the regime has fewer lasting
 *       members than it wants (see {@link Regime#ideal}), a patrol may bring a recruiter.</li>
 *   <li>CONTROLLED and SEAT: enforcer patrols, bigger at the seat. Lieutenants never spawn this way; their
 *       strongholds bring them.</li>
 * </ul>
 * Elsewhere, by decay tier:
 * <ul>
 *   <li>CLEAN / LOW: programs, in the city, waypoints and outskirts, and on the regime's watched ground. Rarely,
 *       in the free city, a wild Bool.</li>
 *   <li>UNSTABLE: corrupted programs, which glitch away when hit.</li>
 *   <li>CORRUPTED: corrupted programs that fight back.</li>
 *   <li>SEVERE: corrupted programs that fight back, in pairs.</li>
 * </ul>
 * Grids don't use biome spawn lists: the tiers are exact to the chunk, where biomes only mark the strongest decay.
 */
public final class GridSpawner {
    private static final int INTERVAL = 100;
    private static final int MIN_DISTANCE = 24;
    private static final int MAX_DISTANCE = 48;
    /** Grid mobs allowed within {@link #MAX_DISTANCE} of a player before no more spawn near them. */
    private static final int CAP = 8;
    /** A recruiter only comes with a patrol if there's no other this close. */
    private static final int RECRUITER_SPACING = 64;
    private static final float BOOL_CHANCE = 0.03f;
    private static final int BOOL_SPACING = 64;

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
                mob -> mob instanceof ProgramEntity || mob instanceof CorruptedProgramEntity || mob instanceof EnforcerEntity
                        || mob instanceof RecruiterEntity).size();
        if (nearby >= CAP) {
            return;
        }
        double angle = world.getRandom().nextDouble() * Math.PI * 2;
        double distance = MIN_DISTANCE + world.getRandom().nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
        if (!world.hasChunk(x >> 4, z >> 4)) {
            return;
        }
        // WORLD_SURFACE: grid chunks generated before the generator kept MOTION_BLOCKING have it at the world's bottom
        BlockPos pos = new BlockPos(x, world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
        if (!world.getBlockState(pos.below()).isRedstoneConductor(world, pos.below())
                || !world.getBlockState(pos).isAir() || !world.getBlockState(pos.above()).isAir()) {
            return;
        }

        GridLayout layout = generator.layout();
        ChunkPlan plan = layout.planAt(x >> 4, z >> 4);
        float control = effectiveControl(world, layout, plan, x >> 4, z >> 4);
        ControlTier held = ControlTier.fromIntensity(control);
        // With its core derezzed, the regime holds everything a step less firmly
        if (CoreManager.defeated(world)) {
            held = held.weaker();
        }
        boolean hostilesAllowed = world.getDifficulty() != Difficulty.PEACEFUL;
        // Where both run high (rare: control suppresses decay), the stronger wins
        if (held.compareTo(ControlTier.PATROLLED) >= 0 && (plan.tier().compareTo(CorruptionTier.UNSTABLE) < 0 || control >= plan.corruption())) {
            switch (held) {
                case PATROLLED -> {
                    if (hostilesAllowed && world.getRandom().nextFloat() < 0.5f) {
                        spawnPatrol(world, pos, held);
                    } else {
                        spawn(world, ModEntities.PROGRAM, pos, program -> { });
                    }
                }
                case CONTROLLED -> {
                    if (hostilesAllowed) {
                        spawnPatrol(world, pos, held);
                    }
                }
                default -> {
                    if (hostilesAllowed) {
                        spawnPatrol(world, pos, held);
                    }
                }
            }
            return;
        }
        switch (plan.tier()) {
            case CLEAN, LOW -> {
                // Now and then a wild Bool, in the free city, never two close together
                if ((plan.zone() == Zone.CITY_CORE || plan.zone() == Zone.CITY) && held == ControlTier.FREE
                        && world.getRandom().nextFloat() < BOOL_CHANCE
                        && world.getEntitiesOfClass(BoolEntity.class, new AABB(pos).inflate(BOOL_SPACING), bool -> true).isEmpty()) {
                    spawn(world, ModEntities.BOOL, pos.above(2), bool -> { });
                    return;
                }
                // Programs live in the city, the waypoints and the outskirts, and as the regime's citizens on its watched ground
                if (plan.zone() == Zone.CITY_CORE || plan.zone() == Zone.CITY || plan.zone() == Zone.WAYPOINT || plan.zone() == Zone.OUTSKIRTS
                        || held == ControlTier.WATCHED) {
                    spawn(world, ModEntities.PROGRAM, pos, program -> { });
                }
            }
            case UNSTABLE -> spawn(world, ModEntities.CORRUPTED_PROGRAM, pos, program -> program.setProvokable(false));
            case CORRUPTED -> spawn(world, ModEntities.CORRUPTED_PROGRAM, pos, program -> program.setProvokable(true));
            // The deep wilds: corrupted programs that fight back, two at a time
            case SEVERE -> {
                spawn(world, ModEntities.CORRUPTED_PROGRAM, pos, program -> program.setProvokable(true));
                spawn(world, ModEntities.CORRUPTED_PROGRAM, pos.offset(1, 0, 0), program -> program.setProvokable(true));
            }
        }
    }

    /**
     * The regime's control at a chunk as spawning sees it: a stronghold whose lieutenant is defeated holds its ground
     * at half strength until the lieutenant returns. The generated terrain doesn't change.
     */
    public static float effectiveControl(ServerLevel world, GridLayout layout, ChunkPlan plan, int chunkX, int chunkZ) {
        if (plan.control() <= 0) {
            return 0;
        }
        Site source = layout.controlSourceAt(chunkX, chunkZ);
        StrongholdState.Stronghold stronghold = source == null ? null : StrongholdState.of(world).get(source.id());
        return stronghold != null && !stronghold.alive() ? plan.control() * 0.5f : plan.control();
    }

    // A squad (see SquadRoles): its size and roles by how firmly the regime holds the land, the first one its leader.
    // While the regime is short of members, a patrol brings a recruiter along, one per area
    private static void spawnPatrol(ServerLevel world, BlockPos pos, ControlTier tier) {
        UUID squad = UUID.randomUUID();
        int size = SquadRoles.size(tier, world.getRandom().nextDouble());
        for (int i = 0; i < size; i++) {
            SquadRoles role = SquadRoles.roll(tier, world.getRandom().nextDouble());
            boolean leader = i == 0;
            spawn(world, ModEntities.ENFORCER, pos.offset(world.getRandom().nextInt(3) - 1, 0, world.getRandom().nextInt(3) - 1),
                    enforcer -> enforcer.joinSquad(squad, role, leader));
        }
        if (!CoreManager.defeated(world) && Regime.population(world) < Regime.ideal(world)
                && world.getEntitiesOfClass(RecruiterEntity.class, new AABB(pos).inflate(RECRUITER_SPACING), recruiter -> true).isEmpty()) {
            spawn(world, ModEntities.RECRUITER, pos, recruiter -> { });
        }
    }

    private static <T extends Mob> void spawn(ServerLevel world, EntityType<T> type, BlockPos pos, Consumer<T> setup) {
        T mob = type.create(world, EntitySpawnReason.NATURAL);
        if (mob == null) {
            return;
        }
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, world.getRandom().nextFloat() * 360, 0);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        setup.accept(mob);
        world.addFreshEntity(mob);
    }
}
