package com.partatoes.digitalfrontier.grid.stronghold;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** The strongholds of one grid, saved with that grid's world. */
public final class StrongholdState extends PersistentState {
    private static final String STRONGHOLDS_KEY = "strongholds";
    private static final Codec<Map<String, Stronghold>> CODEC = Codec.unboundedMap(Codec.STRING, Stronghold.CODEC);
    private static final Type<StrongholdState> TYPE = new Type<>(StrongholdState::new, StrongholdState::fromNbt, null);

    private final Map<String, Stronghold> strongholds = new HashMap<>();

    public static StrongholdState of(ServerWorld grid) {
        return grid.getPersistentStateManager().getOrCreate(TYPE, DigitalFrontier.MOD_ID + "_strongholds");
    }

    @Nullable
    public Stronghold get(String strongholdId) {
        return strongholds.get(strongholdId);
    }

    public Map<String, Stronghold> all() {
        return strongholds;
    }

    void put(String strongholdId, Stronghold stronghold) {
        strongholds.put(strongholdId, stronghold);
        markDirty();
    }

    private static StrongholdState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        StrongholdState state = new StrongholdState();
        CODEC.parse(NbtOps.INSTANCE, nbt.get(STRONGHOLDS_KEY))
                .resultOrPartial(error -> DigitalFrontier.LOGGER.error("Couldn't read grid strongholds: {}", error))
                .ifPresent(state.strongholds::putAll);
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        CODEC.encodeStart(NbtOps.INSTANCE, strongholds).ifSuccess(encoded -> nbt.put(STRONGHOLDS_KEY, encoded));
        return nbt;
    }

    /**
     * One stronghold: its lieutenant (empty if no lieutenant definition fit the grid), whether that lieutenant is
     * alive, and since its last defeat, how far each of its respawn trigger's leaves has got.
     */
    public static final class Stronghold {
        static final Codec<Stronghold> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.optionalFieldOf("lieutenant").forGetter(s -> Optional.ofNullable(s.lieutenant)),
                Codec.BOOL.fieldOf("alive").forGetter(s -> s.alive),
                Codec.LONG.optionalFieldOf("defeated_at", 0L).forGetter(s -> s.defeatedAt),
                Codec.INT.optionalFieldOf("respawns", 0).forGetter(s -> s.respawns),
                Uuids.CODEC.optionalFieldOf("entity").forGetter(s -> Optional.ofNullable(s.entity)),
                Codec.INT.listOf().optionalFieldOf("progress", List.of()).forGetter(s -> s.progress),
                Codec.LONG.optionalFieldOf("last_occupied", Long.MIN_VALUE / 2).forGetter(s -> s.lastOccupied)
        ).apply(instance, (lieutenant, alive, defeatedAt, respawns, entity, progress, lastOccupied) -> {
            Stronghold stronghold = new Stronghold(lieutenant.orElse(null));
            stronghold.alive = alive;
            stronghold.defeatedAt = defeatedAt;
            stronghold.respawns = respawns;
            stronghold.entity = entity.orElse(null);
            stronghold.progress.addAll(progress);
            stronghold.lastOccupied = lastOccupied;
            return stronghold;
        }));

        @Nullable
        final Identifier lieutenant;
        boolean alive = true;
        long defeatedAt;
        int respawns;
        @Nullable
        UUID entity;
        final List<Integer> progress = new ArrayList<>();
        // Game time a player was last near; long ago for a stronghold nobody has visited
        long lastOccupied = Long.MIN_VALUE / 2;
        // Not saved: worked out again on the first check after loading
        boolean occupied;

        Stronghold(@Nullable Identifier lieutenant) {
            this.lieutenant = lieutenant;
        }

        @Nullable
        public Identifier lieutenant() {
            return lieutenant;
        }

        public boolean alive() {
            return alive;
        }

        public long defeatedAt() {
            return defeatedAt;
        }

        public int respawns() {
            return respawns;
        }

        public List<Integer> progress() {
            return progress;
        }

        /** Makes room for one counter per leaf; a datapack reload may have changed the trigger. */
        void fitProgress(int leaves) {
            while (progress.size() < leaves) {
                progress.add(0);
            }
            while (progress.size() > leaves) {
                progress.removeLast();
            }
        }
    }
}
