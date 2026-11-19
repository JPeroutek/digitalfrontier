package com.partatoes.digitalfrontier.grid.core;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.entity.custom.SystemCoreEntity;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.core.CoreState.Phase;
import com.partatoes.digitalfrontier.grid.layout.CoreChamber;
import com.partatoes.digitalfrontier.grid.layout.GridLayout;
import com.partatoes.digitalfrontier.grid.regime.Regime;
import com.partatoes.digitalfrontier.grid.stronghold.StrongholdManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The System Core's life in each grid: dormant behind its seal while the regime's lieutenants stand, awake (the seal
 * open, the core waiting in its chamber) once every stronghold has lost its lieutenant, and defeated for good once
 * derezzed. A defeated core weakens the regime: see {@link #defeated}.
 */
public final class CoreManager {
    // A player this close to the core brings it into being, as a stronghold does its lieutenant
    private static final int PRESENCE_RADIUS = 48;

    private CoreManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) {
                return;
            }
            for (ServerLevel world : server.getAllLevels()) {
                if (GridArrival.generator(world) != null) {
                    tick(world);
                }
            }
        });
    }

    public static Phase phase(ServerLevel grid) {
        return CoreState.of(grid).phase();
    }

    /** Whether this grid's core has been derezzed, which leaves the regime weaker for good. */
    public static boolean defeated(ServerLevel grid) {
        return GridArrival.generator(grid) != null && CoreState.of(grid).phase() == Phase.DEFEATED;
    }

    private static void tick(ServerLevel grid) {
        CoreState state = CoreState.of(grid);
        if (state.phase() == Phase.DORMANT && regimeFallen(grid)) {
            wake(grid, state);
        }
        updateSeal(grid, state.phase() == Phase.DORMANT);
        if (state.phase() == Phase.AWAKE) {
            ensureSpawned(grid, state);
        }
    }

    // Every stronghold has lost its lieutenant: defeated, redeemed, or never had one
    private static boolean regimeFallen(ServerLevel grid) {
        var strongholds = StrongholdManager.strongholds(grid);
        return !strongholds.isEmpty()
                && strongholds.stream().noneMatch(view -> view.stronghold().alive() && view.stronghold().lieutenant() != null);
    }

    private static void wake(ServerLevel grid, CoreState state) {
        state.setPhase(Phase.AWAKE);
        BlockPos shaft = shaftTop();
        grid.players().forEach(player -> player.sendSystemMessage(Component.translatable("message.digitalfrontier.core_awake",
                shaft.getX() + ", " + shaft.getZ()).withStyle(ChatFormatting.DARK_RED)));
    }

    /** The middle of the shaft's mouth, on the street at the city center. */
    public static BlockPos shaftTop() {
        int middle = (CoreChamber.SHAFT_MIN + CoreChamber.SHAFT_MAX + 1) / 2;
        return new BlockPos(middle, GridLayout.FLOOR_Y + 1, middle);
    }

    // Closed while the core is dormant, open otherwise; only ever swaps the seal for the shaft's own air and ladder
    private static void updateSeal(ServerLevel grid, boolean closed) {
        if (!grid.hasChunk(CoreChamber.SHAFT_MIN >> 4, CoreChamber.SHAFT_MIN >> 4)) {
            return;
        }
        for (int x = CoreChamber.SHAFT_MIN; x <= CoreChamber.SHAFT_MAX; x++) {
            for (int z = CoreChamber.SHAFT_MIN; z <= CoreChamber.SHAFT_MAX; z++) {
                BlockPos pos = new BlockPos(x, CoreChamber.SEAL_Y, z);
                BlockState open = x == CoreChamber.LADDER_X
                        ? Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST)
                        : Blocks.AIR.defaultBlockState();
                BlockState current = grid.getBlockState(pos);
                BlockState wanted = closed ? ModBlocks.CORE_SEAL.defaultBlockState() : open;
                if (!current.equals(wanted) && (current.is(ModBlocks.CORE_SEAL) || current.equals(open))) {
                    grid.setBlockAndUpdate(pos, wanted);
                }
            }
        }
    }

    private static void ensureSpawned(ServerLevel grid, CoreState state) {
        BlockPos core = CoreChamber.core();
        if (!grid.hasChunk(core.getX() >> 4, core.getZ() >> 4)) {
            return;
        }
        Entity existing = state.entity() == null ? null : grid.getEntity(state.entity());
        if (existing != null && existing.isAlive()) {
            return;
        }
        boolean playerNear = grid.players().stream()
                .anyMatch(player -> !player.isSpectator() && player.distanceToSqr(Vec3.atCenterOf(core)) < PRESENCE_RADIUS * PRESENCE_RADIUS);
        if (!playerNear) {
            return;
        }
        SystemCoreEntity entity = ModEntities.SYSTEM_CORE.create(grid, EntitySpawnReason.EVENT);
        if (entity == null) {
            return;
        }
        entity.snapTo(core.getX() + 0.5, core.getY(), core.getZ() + 0.5, 0, 0);
        state.setEntity(entity.getUUID());
        grid.addFreshEntity(entity);
    }

    /** Whether this core entity is the one its grid expects; stray copies remove themselves. */
    public static boolean isCurrent(SystemCoreEntity entity) {
        if (!(entity.level() instanceof ServerLevel grid)) {
            return false;
        }
        CoreState state = CoreState.of(grid);
        return state.phase() == Phase.AWAKE && entity.getUUID().equals(state.entity());
    }

    public static void onDefeated(SystemCoreEntity entity) {
        if (!(entity.level() instanceof ServerLevel grid) || !isCurrent(entity)) {
            return;
        }
        CoreState state = CoreState.of(grid);
        state.setPhase(Phase.DEFEATED);
        state.setEntity(null);
        Component message = Component.translatable("message.digitalfrontier.core_defeated").withStyle(ChatFormatting.GOLD);
        for (ServerPlayer player : grid.players()) {
            player.sendSystemMessage(message);
            if (player.distanceToSqr(entity) < PRESENCE_RADIUS * PRESENCE_RADIUS) {
                Regime.shiftAlignment(player, Regime.rules().alignment().defeatCore());
            }
        }
    }

    /**
     * Back to dormant, for testing: the core goes, its seal closes, and every fallen lieutenant returns (or the core
     * would wake again at once). Defeating them all wakes it again.
     */
    public static void reset(ServerLevel grid) {
        StrongholdManager.strongholds(grid).stream()
                .filter(view -> !view.stronghold().alive() && view.stronghold().lieutenant() != null)
                .forEach(view -> StrongholdManager.forceRespawn(grid, view.site().id()));
        CoreState state = CoreState.of(grid);
        Entity existing = state.entity() == null ? null : grid.getEntity(state.entity());
        if (existing != null) {
            existing.discard();
        }
        state.setEntity(null);
        state.setPhase(Phase.DORMANT);
        updateSeal(grid, true);
    }
}
