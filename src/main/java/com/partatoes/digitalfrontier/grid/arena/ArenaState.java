package com.partatoes.digitalfrontier.grid.arena;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.world.ModSavedData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What a grid's arena remembers, saved with the grid: how many times each game's champion has been beaten (which sets
 * its level), how many titles have been won (one trophy each), and when each player was last conscripted.
 */
public final class ArenaState extends SavedData {
    /** A champion's level tops out here: the title champion. */
    public static final int TITLE_LEVEL = 3;

    private static final Codec<ArenaState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("wins", Map.of()).forGetter(state -> state.wins),
            Codec.INT.optionalFieldOf("titles", 0).forGetter(state -> state.titles),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("conscripted", Map.of()).forGetter(state -> state.conscripted)
    ).apply(instance, ArenaState::new));
    private static final SavedDataType<ArenaState> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "arena"),
            ArenaState::new, CODEC, null);

    private final Map<String, Integer> wins = new HashMap<>();
    private int titles;
    private final Map<String, Long> conscripted = new HashMap<>();

    private ArenaState() {
    }

    private ArenaState(Map<String, Integer> wins, int titles, Map<String, Long> conscripted) {
        this.wins.putAll(wins);
        this.titles = titles;
        this.conscripted.putAll(conscripted);
    }

    public static ArenaState of(ServerLevel grid) {
        return ModSavedData.computeIfAbsent(grid.getDataStorage(), TYPE);
    }

    /** The level of a game's champion: 1 to begin with, one more for each time it's been beaten, up to the title. */
    public int championLevel(ArenaGame game) {
        return Math.min(TITLE_LEVEL, 1 + wins.getOrDefault(game.id(), 0));
    }

    public void recordWin(ArenaGame game) {
        wins.merge(game.id(), 1, Integer::sum);
        setDirty();
    }

    public int titles() {
        return titles;
    }

    public void recordTitle() {
        titles++;
        setDirty();
    }

    /** The game day a player was last conscripted, or -1. */
    public long lastConscripted(UUID player) {
        return conscripted.getOrDefault(player.toString(), -1L);
    }

    public void recordConscripted(UUID player, long day) {
        conscripted.put(player.toString(), day);
        setDirty();
    }

    public void reset() {
        wins.clear();
        titles = 0;
        conscripted.clear();
        setDirty();
    }
}
