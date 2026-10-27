package com.partatoes.digitalfrontier.grid.stronghold;

import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.custom.LieutenantEntity;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.layout.GridRandom;
import com.partatoes.digitalfrontier.grid.layout.Site;
import com.partatoes.digitalfrontier.grid.stronghold.Lieutenants.Lieutenant;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdState.Stronghold;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Runs every grid's strongholds: picks each one's lieutenant, spawns it when players come near, records its
 * defeat, counts {@link GridEvent}s toward its respawn trigger, and brings it back once the trigger is met.
 */
public final class StrongholdManager {
    /** Players within this many blocks of a stronghold count as being at it. */
    public static final int PRESENCE_RADIUS = 48;
    private static final int CHECK_INTERVAL = 20;
    private static final int SALT_LIEUTENANT = 6001;

    private StrongholdManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % CHECK_INTERVAL == 0) {
                for (ServerWorld world : server.getWorlds()) {
                    GridChunkGenerator generator = GridArrival.generator(world);
                    if (generator != null) {
                        tick(world, generator.layout());
                    }
                }
            }
        });
    }

    /** The outcome of an event: whether any trigger counted it, and whether one asked for the used item. */
    public record Fired(boolean counted, boolean consume) {
        static final Fired NONE = new Fired(false, false);
    }

    /** Fires an event at every stronghold of one grid. */
    public static Fired fire(ServerWorld grid, GridEvent event) {
        return fire(grid, event, null);
    }

    // With onlyStronghold set, the event is about that one stronghold (e.g. a player arriving at it)
    private static Fired fire(ServerWorld grid, GridEvent event, @Nullable String onlyStronghold) {
        GridChunkGenerator generator = GridArrival.generator(grid);
        if (generator == null) {
            return Fired.NONE;
        }
        boolean counted = false;
        boolean consume = false;
        StrongholdState state = StrongholdState.of(grid);
        for (Site site : strongholds(generator.layout())) {
            if (onlyStronghold != null && !onlyStronghold.equals(site.id())) {
                continue;
            }
            Stronghold stronghold = stronghold(grid, generator.layout(), state, site);
            Lieutenant lieutenant = stronghold.lieutenant() == null ? null : Lieutenants.get(stronghold.lieutenant());
            if (lieutenant == null || stronghold.alive()) {
                continue;
            }
            stronghold.fitProgress(lieutenant.respawn().leaves().size());
            Context context = new Context(grid, state, site.id(), stronghold, lieutenant, center(site));
            boolean countedHere = false;
            for (Trigger leaf : lieutenant.respawn().leaves()) {
                if (leaf instanceof Trigger.EventTrigger eventTrigger && eventTrigger.matches(event, context)
                        && grid.random.nextDouble() < context.chance(leaf)) {
                    int index = lieutenant.respawn().index(leaf);
                    stronghold.progress.set(index, stronghold.progress.get(index) + 1);
                    countedHere = true;
                    consume |= leaf instanceof Triggers.ItemUsed itemUsed && itemUsed.consume();
                }
            }
            if (countedHere) {
                counted = true;
                state.markDirty();
                tryRespawn(grid, site, stronghold, lieutenant, context);
            }
        }
        return new Fired(counted, consume);
    }

    /** Fires an event in every grid, for things that happen outside any one grid (like crafting). */
    public static void fireEverywhere(MinecraftServer server, GridEvent event) {
        for (ServerWorld world : server.getWorlds()) {
            if (GridArrival.generator(world) != null) {
                fire(world, event);
            }
        }
    }

    public static void onDefeated(LieutenantEntity entity) {
        if (!(entity.getWorld() instanceof ServerWorld grid) || entity.strongholdId() == null) {
            return;
        }
        StrongholdState state = StrongholdState.of(grid);
        Stronghold stronghold = state.get(entity.strongholdId());
        if (stronghold == null || !entity.getUuid().equals(stronghold.entity)) {
            return;
        }
        defeat(grid, state, entity.strongholdId(), stronghold, entity.getDisplayName());
    }

    /** Marks a lieutenant defeated without a fight (removing it if it's loaded), e.g. to test respawn triggers. */
    public static boolean forceDefeat(ServerWorld grid, String strongholdId) {
        StrongholdState state = StrongholdState.of(grid);
        Stronghold stronghold = state.get(strongholdId);
        if (stronghold == null || !stronghold.alive() || stronghold.lieutenant() == null) {
            return false;
        }
        Entity existing = stronghold.entity == null ? null : grid.getEntity(stronghold.entity);
        if (existing != null) {
            existing.discard();
        }
        Lieutenant lieutenant = Lieutenants.get(stronghold.lieutenant());
        defeat(grid, state, strongholdId, stronghold, lieutenant != null ? lieutenant.def().name() : Text.literal(stronghold.lieutenant().toString()));
        return true;
    }

    private static void defeat(ServerWorld grid, StrongholdState state, String strongholdId, Stronghold stronghold, Text name) {
        stronghold.alive = false;
        stronghold.defeatedAt = grid.getTime();
        stronghold.entity = null;
        stronghold.progress.replaceAll(count -> 0);
        state.markDirty();
        broadcast(grid, Text.translatable("message.digitalfrontier.lieutenant_defeated", name).formatted(Formatting.AQUA));
        if (stronghold.lieutenant() != null) {
            fire(grid, new GridEvent.LieutenantDefeated(stronghold.lieutenant(), strongholdId));
        }
    }

    /** Whether this lieutenant entity is the one its stronghold expects; stray copies remove themselves. */
    public static boolean isCurrent(LieutenantEntity entity) {
        if (!(entity.getWorld() instanceof ServerWorld grid) || entity.strongholdId() == null) {
            return false;
        }
        Stronghold stronghold = StrongholdState.of(grid).get(entity.strongholdId());
        return stronghold != null && stronghold.alive() && entity.getUuid().equals(stronghold.entity);
    }

    /** Brings a lieutenant back now, whatever its trigger says. */
    public static boolean forceRespawn(ServerWorld grid, String strongholdId) {
        StrongholdState state = StrongholdState.of(grid);
        Stronghold stronghold = state.get(strongholdId);
        if (stronghold == null || stronghold.alive() || stronghold.lieutenant() == null) {
            return false;
        }
        respawn(grid, state, stronghold);
        return true;
    }

    /** Every stronghold of a grid, creating their records (and picking lieutenants) if needed. */
    public static List<StrongholdView> strongholds(ServerWorld grid) {
        GridChunkGenerator generator = GridArrival.generator(grid);
        if (generator == null) {
            return List.of();
        }
        StrongholdState state = StrongholdState.of(grid);
        List<StrongholdView> views = new ArrayList<>();
        for (Site site : strongholds(generator.layout())) {
            Stronghold stronghold = stronghold(grid, generator.layout(), state, site);
            Lieutenant lieutenant = stronghold.lieutenant() == null ? null : Lieutenants.get(stronghold.lieutenant());
            views.add(new StrongholdView(site, stronghold, lieutenant, center(site)));
        }
        return views;
    }

    public record StrongholdView(Site site, Stronghold stronghold, @Nullable Lieutenant lieutenant, BlockPos center) {
    }

    private static void tick(ServerWorld grid, GridLayout layout) {
        StrongholdState state = StrongholdState.of(grid);
        long now = grid.getTime();
        for (Site site : strongholds(layout)) {
            Stronghold stronghold = stronghold(grid, layout, state, site);
            Lieutenant lieutenant = stronghold.lieutenant() == null ? null : Lieutenants.get(stronghold.lieutenant());
            if (lieutenant == null) {
                continue;
            }
            stronghold.fitProgress(lieutenant.respawn().leaves().size());
            BlockPos center = center(site);
            Context context = new Context(grid, state, site.id(), stronghold, lieutenant, center);

            boolean near = playerNear(grid, center);
            if (near && !stronghold.occupied && !stronghold.alive()) {
                fire(grid, new GridEvent.StrongholdEntered(now - stronghold.lastOccupied), site.id());
            }
            stronghold.occupied = near;
            if (near) {
                stronghold.lastOccupied = now;
                state.markDirty();
            }

            if (!stronghold.alive()) {
                latchStates(grid, stronghold, lieutenant, context);
                tryRespawn(grid, site, stronghold, lieutenant, context);
            } else if (near) {
                ensureSpawned(grid, state, site, stronghold, lieutenant, center);
            }
        }
    }

    // States under a chance roll once per check while they hold, and stay satisfied once a roll succeeds
    private static void latchStates(ServerWorld grid, Stronghold stronghold, Lieutenant lieutenant, Context context) {
        for (Trigger leaf : lieutenant.respawn().leaves()) {
            double chance = context.chance(leaf);
            if (leaf instanceof Trigger.StateTrigger state && chance < 1 && state.holds(context) && grid.random.nextDouble() < chance) {
                int index = lieutenant.respawn().index(leaf);
                if (stronghold.progress.get(index) == 0) {
                    stronghold.progress.set(index, 1);
                    StrongholdState.of(grid).markDirty();
                }
            }
        }
    }

    private static void tryRespawn(ServerWorld grid, Site site, Stronghold stronghold, Lieutenant lieutenant, Context context) {
        LieutenantDef.Respawn respawn = lieutenant.def().respawn();
        boolean delayPassed = grid.getTime() - stronghold.defeatedAt() >= respawn.minDelay();
        boolean respawnsLeft = respawn.maxRespawns() < 0 || stronghold.respawns() < respawn.maxRespawns();
        if (!stronghold.alive() && delayPassed && respawnsLeft && lieutenant.respawn().root().test(context)) {
            respawn(grid, StrongholdState.of(grid), stronghold);
        }
    }

    private static void respawn(ServerWorld grid, StrongholdState state, Stronghold stronghold) {
        stronghold.alive = true;
        stronghold.respawns++;
        stronghold.progress.replaceAll(count -> 0);
        state.markDirty();
        Lieutenant lieutenant = Lieutenants.get(stronghold.lieutenant());
        Text name = lieutenant != null ? lieutenant.def().name() : Text.literal(String.valueOf(stronghold.lieutenant()));
        broadcast(grid, Text.translatable("message.digitalfrontier.lieutenant_returned", name).formatted(Formatting.RED));
    }

    private static void ensureSpawned(ServerWorld grid, StrongholdState state, Site site, Stronghold stronghold, Lieutenant lieutenant,
                                      BlockPos center) {
        if (!grid.isChunkLoaded(site.chunkX(), site.chunkZ())) {
            return;
        }
        Entity existing = stronghold.entity == null ? null : grid.getEntity(stronghold.entity);
        if (existing != null && existing.isAlive()) {
            return;
        }
        LieutenantEntity entity = ModEntities.LIEUTENANT.create(grid, SpawnReason.EVENT);
        if (entity == null) {
            return;
        }
        BlockPos spawn = center;
        while (!grid.getBlockState(spawn).isAir() && spawn.getY() < center.getY() + 12) {
            spawn = spawn.up();
        }
        entity.refreshPositionAndAngles(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, grid.random.nextFloat() * 360, 0);
        entity.setup(lieutenant, site.id(), center);
        stronghold.entity = entity.getUuid();
        state.markDirty();
        grid.spawnEntity(entity);
    }

    private static Stronghold stronghold(ServerWorld grid, GridLayout layout, StrongholdState state, Site site) {
        Stronghold stronghold = state.get(site.id());
        if (stronghold == null) {
            stronghold = new Stronghold(pickLieutenant(layout, state, site).orElse(null));
            state.put(site.id(), stronghold);
        }
        return stronghold;
    }

    // Strongholds pick in site order (stronghold_0 first), each preferring a lieutenant no other stronghold of the
    // grid has; once every eligible lieutenant is taken, they can repeat
    private static Optional<Identifier> pickLieutenant(GridLayout layout, StrongholdState state, Site site) {
        List<Lieutenant> eligible = Lieutenants.eligible(layout.settings().corruption());
        List<Identifier> taken = state.all().values().stream().map(Stronghold::lieutenant).toList();
        List<Lieutenant> untaken = eligible.stream().filter(lieutenant -> !taken.contains(lieutenant.id())).toList();
        if (!untaken.isEmpty()) {
            eligible = untaken;
        }
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        int total = eligible.stream().mapToInt(lieutenant -> lieutenant.def().weight()).sum();
        int target = GridRandom.nextInt(GridRandom.hash(layout.settings().gridSeed(), site.chunkX(), site.chunkZ(), SALT_LIEUTENANT), total);
        for (Lieutenant lieutenant : eligible) {
            target -= lieutenant.def().weight();
            if (target < 0) {
                return Optional.of(lieutenant.id());
            }
        }
        return Optional.of(eligible.getLast().id());
    }

    private static List<Site> strongholds(GridLayout layout) {
        return layout.sites().stream().filter(site -> site.type() == Site.Type.STRONGHOLD).toList();
    }

    private static BlockPos center(Site site) {
        return new BlockPos(site.chunkX() * 16 + 8, GridLayout.FLOOR_Y + 1, site.chunkZ() * 16 + 8);
    }

    private static boolean playerNear(ServerWorld grid, BlockPos center) {
        for (ServerPlayerEntity player : grid.getPlayers()) {
            if (!player.isSpectator() && player.squaredDistanceTo(center.toCenterPos()) <= (double) PRESENCE_RADIUS * PRESENCE_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static void broadcast(ServerWorld grid, Text message) {
        grid.getPlayers().forEach(player -> player.sendMessage(message, false));
    }

    private record Context(ServerWorld grid, StrongholdState state, String stronghold, Stronghold record, Lieutenant lieutenantEntry,
                           BlockPos center)
            implements TriggerContext {
        @Override
        public Identifier lieutenant() {
            return lieutenantEntry.id();
        }

        @Override
        public long defeatedAt() {
            return record.defeatedAt();
        }

        @Override
        public boolean allDefeated() {
            return state.all().values().stream().filter(other -> other.lieutenant() != null).noneMatch(Stronghold::alive);
        }

        @Override
        public int count(Trigger leaf) {
            int index = lieutenantEntry.respawn().index(leaf);
            return index < record.progress().size() ? record.progress().get(index) : 0;
        }

        @Override
        public double chance(Trigger leaf) {
            return lieutenantEntry.respawn().chance(leaf);
        }
    }
}
