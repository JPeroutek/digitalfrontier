package com.partatoes.digitalfrontier.grid.regime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.world.ModSavedData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The regime of one grid, saved with that grid's world: each player's alignment with it, and its roster, the regime's
 * lasting members (conscripts and recruiters). Patrols that come and go aren't on the roster.
 */
public final class RegimeState extends SavedData {
    private record Saved(Map<UUID, Float> alignments, int roster) {
        static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.FLOAT).optionalFieldOf("alignments", Map.of()).forGetter(Saved::alignments),
                Codec.INT.optionalFieldOf("roster", 0).forGetter(Saved::roster)
        ).apply(instance, Saved::new));
    }

    private static final String KEY = "regime";
    private static final SavedDataType<RegimeState> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "regime"),
            RegimeState::new, CompoundTag.CODEC.xmap(RegimeState::fromNbt, state -> state.writeNbt(new CompoundTag())), null);

    // Concurrent: the F3 screen reads it from the client thread in single player
    private final Map<UUID, Float> alignments = new ConcurrentHashMap<>();
    private int roster;

    public static RegimeState of(ServerLevel grid) {
        return ModSavedData.computeIfAbsent(grid.getDataStorage(), TYPE);
    }

    /** A player's alignment, -1 (the regime's enemy) to 1 (its ally); 0 for a player it hasn't dealt with. */
    public float alignment(UUID player) {
        return alignments.getOrDefault(player, 0f);
    }

    public void setAlignment(UUID player, float alignment) {
        alignments.put(player, (float) Math.clamp(alignment, -1.0, 1.0));
        setDirty();
    }

    public void shiftAlignment(UUID player, float delta) {
        setAlignment(player, alignment(player) + delta);
    }

    public int roster() {
        return roster;
    }

    public void changeRoster(int delta) {
        roster = Math.max(0, roster + delta);
        setDirty();
    }

    private static RegimeState fromNbt(CompoundTag nbt) {
        RegimeState state = new RegimeState();
        Saved.CODEC.parse(NbtOps.INSTANCE, nbt.get(KEY))
                .resultOrPartial(error -> DigitalFrontier.LOGGER.error("Couldn't read the grid's regime: {}", error))
                .ifPresent(saved -> {
                    state.alignments.putAll(saved.alignments());
                    state.roster = saved.roster();
                });
        return state;
    }

    private CompoundTag writeNbt(CompoundTag nbt) {
        Saved.CODEC.encodeStart(NbtOps.INSTANCE, new Saved(alignments, roster)).ifSuccess(encoded -> nbt.put(KEY, encoded));
        return nbt;
    }
}
