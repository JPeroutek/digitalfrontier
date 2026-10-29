package com.partatoes.digitalfrontier.grid;

import com.mojang.serialization.Codec;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.gen.GridBiomeSource;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleCategory;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tuned grids: grids other than the default, created at runtime when a Grid Tuning Laser tunes into new settings.
 * Each is a persistent Fantasy world whose id is {@link GridKey#of} its settings. The settings of every tuned grid
 * are saved with the overworld so the grids can be reopened when the server starts; Fantasy doesn't do that itself.
 * The default grid is an ordinary datapack dimension and isn't managed here.
 */
public final class GridManager {
    public static final GameRule<Integer> MAX_TUNED_GRIDS = GameRuleBuilder.forInteger(8).minValue(0).category(GameRuleCategory.MISC)
            .buildAndRegister(Identifier.of(DigitalFrontier.MOD_ID, "max_tuned_grids"));

    private GridManager() {
    }

    /** How many tuned grids the server may have (the {@code digitalfrontier:max_tuned_grids} game rule). */
    public static int maxTunedGrids(MinecraftServer server) {
        return server.getOverworld().getGameRules().getValue(MAX_TUNED_GRIDS);
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> state(server).grids.forEach((id, settings) -> {
            open(server, id, settings);
            DigitalFrontier.LOGGER.info("Reopened tuned grid {}", id);
        }));
    }

    /**
     * The grid with these settings, opening or creating it if needed. Empty if it would be a new tuned grid and the
     * server already has as many as the {@code digitalfrontier:max_tuned_grids} game rule allows.
     */
    public static Optional<ServerWorld> getOrCreate(MinecraftServer server, GridLayoutSettings settings) {
        Identifier id = GridKey.of(settings);
        if (id.equals(ModDimensions.DEFAULT_GRID_ID)) {
            return GridArrival.gridWorld(server, id);
        }
        State state = state(server);
        if (!state.grids.containsKey(id)) {
            if (state.grids.size() >= maxTunedGrids(server)) {
                return Optional.empty();
            }
            state.grids.put(id, settings);
            state.markDirty();
            DigitalFrontier.LOGGER.info("Created tuned grid {}", id);
        }
        return Optional.of(open(server, id, settings).asWorld());
    }

    public static Map<Identifier, GridLayoutSettings> tunedGrids(MinecraftServer server) {
        return Map.copyOf(state(server).grids);
    }

    /** Deletes a tuned grid and its saved chunks; players inside are sent to the overworld spawn. */
    public static boolean delete(MinecraftServer server, Identifier id) {
        State state = state(server);
        GridLayoutSettings settings = state.grids.remove(id);
        if (settings == null) {
            return false;
        }
        state.markDirty();
        open(server, id, settings).delete();
        DigitalFrontier.LOGGER.info("Deleted tuned grid {}", id);
        return true;
    }

    private static RuntimeWorldHandle open(MinecraftServer server, Identifier id, GridLayoutSettings settings) {
        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setDimensionType(ModDimensions.DIGITAL_FRONTIER_TYPE_KEY)
                .setGenerator(new GridChunkGenerator(new GridBiomeSource(settings, defaultGenerator(server).gridBiomeSource().biomes())))
                .setMirrorOverworldGameRules(true)
                .setMirrorOverworldDifficulty(true)
                .setShouldTickTime(false);
        return Fantasy.get(server).getOrOpenPersistentWorld(id, config);
    }

    // Tuned grids use the same zone biomes as the default grid
    private static GridChunkGenerator defaultGenerator(MinecraftServer server) {
        return GridArrival.gridWorld(server, ModDimensions.DEFAULT_GRID_ID)
                .map(GridArrival::generator)
                .orElseThrow(() -> new IllegalStateException("The default grid " + ModDimensions.DEFAULT_GRID_ID + " isn't loaded"));
    }

    private static State state(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(State.TYPE);
    }

    private static final class State extends PersistentState {
        private static final Codec<Map<Identifier, GridLayoutSettings>> CODEC = Codec.unboundedMap(Identifier.CODEC, GridLayoutSettings.CODEC);
        private static final String GRIDS_KEY = "grids";
        // Saved through the same NBT as before 1.21.5, so a bad entry only drops that grid
        static final PersistentStateType<State> TYPE = new PersistentStateType<>(DigitalFrontier.MOD_ID + "_grids", State::new,
                NbtCompound.CODEC.xmap(State::fromNbt, state -> state.writeNbt(new NbtCompound())), null);

        final Map<Identifier, GridLayoutSettings> grids = new LinkedHashMap<>();

        private static State fromNbt(NbtCompound nbt) {
            State state = new State();
            CODEC.parse(NbtOps.INSTANCE, nbt.get(GRIDS_KEY))
                    .resultOrPartial(error -> DigitalFrontier.LOGGER.error("Couldn't read tuned grids: {}", error))
                    .ifPresent(state.grids::putAll);
            return state;
        }

        NbtCompound writeNbt(NbtCompound nbt) {
            CODEC.encodeStart(NbtOps.INSTANCE, grids).ifSuccess(encoded -> nbt.put(GRIDS_KEY, encoded));
            return nbt;
        }
    }
}
