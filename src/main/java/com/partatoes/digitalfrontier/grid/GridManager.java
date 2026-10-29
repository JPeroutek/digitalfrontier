package com.partatoes.digitalfrontier.grid;

import com.mojang.serialization.Codec;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.gen.GridBiomeSource;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import com.partatoes.digitalfrontier.world.ModDimensions;
import com.partatoes.digitalfrontier.world.ModSavedData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tuned grids: grids other than the default, created at runtime when a Grid Tuning Laser tunes into new settings.
 * Each is a persistent Fantasy world whose id is {@link GridKey#of} its settings. The settings of every tuned grid
 * are saved with the world so the grids can be reopened when the server starts; Fantasy doesn't do that itself.
 * The default grid is an ordinary datapack dimension and isn't managed here.
 */
public final class GridManager {
    public static final GameRule<Integer> MAX_TUNED_GRIDS = GameRuleBuilder.forInteger(8).minValue(0).category(GameRuleCategory.MISC)
            .buildAndRegister(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "max_tuned_grids"));

    private GridManager() {
    }

    /** How many tuned grids the server may have (the {@code digitalfrontier:max_tuned_grids} game rule). */
    public static int maxTunedGrids(MinecraftServer server) {
        return server.overworld().getGameRules().get(MAX_TUNED_GRIDS);
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
    public static Optional<ServerLevel> getOrCreate(MinecraftServer server, GridLayoutSettings settings) {
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
            state.setDirty();
            DigitalFrontier.LOGGER.info("Created tuned grid {}", id);
        }
        return Optional.of(open(server, id, settings).asLevel());
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
        state.setDirty();
        open(server, id, settings).delete();
        DigitalFrontier.LOGGER.info("Deleted tuned grid {}", id);
        return true;
    }

    private static RuntimeLevelHandle open(MinecraftServer server, Identifier id, GridLayoutSettings settings) {
        RuntimeLevelConfig config = new RuntimeLevelConfig()
                .setDimensionType(ModDimensions.DIGITAL_FRONTIER_TYPE_KEY)
                .setGenerator(new GridChunkGenerator(new GridBiomeSource(settings, defaultGenerator(server).gridBiomeSource().biomes())))
                .setMirrorOverworldGameRules(true)
                .setMirrorOverworldDifficulty(true)
                .setShouldTickTime(false);
        return Fantasy.get(server).getOrOpenPersistentLevel(id, config);
    }

    // Tuned grids use the same zone biomes as the default grid
    private static GridChunkGenerator defaultGenerator(MinecraftServer server) {
        return GridArrival.gridWorld(server, ModDimensions.DEFAULT_GRID_ID)
                .map(GridArrival::generator)
                .orElseThrow(() -> new IllegalStateException("The default grid " + ModDimensions.DEFAULT_GRID_ID + " isn't loaded"));
    }

    private static State state(MinecraftServer server) {
        return ModSavedData.computeIfAbsent(server.getDataStorage(), State.TYPE);
    }

    private static final class State extends SavedData {
        private static final Codec<Map<Identifier, GridLayoutSettings>> CODEC = Codec.unboundedMap(Identifier.CODEC, GridLayoutSettings.CODEC);
        private static final String GRIDS_KEY = "grids";
        // Saved through the same NBT as before 1.21.5, so a bad entry only drops that grid
        static final SavedDataType<State> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "grids"), State::new,
                CompoundTag.CODEC.xmap(State::fromNbt, state -> state.writeNbt(new CompoundTag())), null);

        final Map<Identifier, GridLayoutSettings> grids = new LinkedHashMap<>();

        private static State fromNbt(CompoundTag nbt) {
            State state = new State();
            CODEC.parse(NbtOps.INSTANCE, nbt.get(GRIDS_KEY))
                    .resultOrPartial(error -> DigitalFrontier.LOGGER.error("Couldn't read tuned grids: {}", error))
                    .ifPresent(state.grids::putAll);
            return state;
        }

        CompoundTag writeNbt(CompoundTag nbt) {
            CODEC.encodeStart(NbtOps.INSTANCE, grids).ifSuccess(encoded -> nbt.put(GRIDS_KEY, encoded));
            return nbt;
        }
    }
}
