package com.partatoes.digitalfrontier.grid.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.world.ModSavedData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/** The System Core of one grid, saved with that grid's world: how far it has got, and which entity it is. */
public final class CoreState extends SavedData {
    public enum Phase implements StringRepresentable {
        /** Sealed in its chamber while the regime's lieutenants stand. */
        DORMANT("dormant"),
        /** The last lieutenant has fallen: its seal is open, and it waits below. */
        AWAKE("awake"),
        /** Derezzed: the regime is broken in this grid. */
        DEFEATED("defeated");

        public static final Codec<Phase> CODEC = StringRepresentable.fromEnum(Phase::values);

        private final String name;

        Phase(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private record Saved(Phase phase, Optional<UUID> entity) {
        static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Phase.CODEC.optionalFieldOf("phase", Phase.DORMANT).forGetter(Saved::phase),
                UUIDUtil.AUTHLIB_CODEC.optionalFieldOf("entity").forGetter(Saved::entity)
        ).apply(instance, Saved::new));
    }

    private static final String KEY = "core";
    private static final SavedDataType<CoreState> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "core"),
            CoreState::new, CompoundTag.CODEC.xmap(CoreState::fromNbt, state -> state.writeNbt(new CompoundTag())), null);

    private Phase phase = Phase.DORMANT;
    @Nullable
    private UUID entity;

    public static CoreState of(ServerLevel grid) {
        return ModSavedData.computeIfAbsent(grid.getDataStorage(), TYPE);
    }

    public Phase phase() {
        return phase;
    }

    void setPhase(Phase phase) {
        this.phase = phase;
        setDirty();
    }

    @Nullable
    UUID entity() {
        return entity;
    }

    void setEntity(@Nullable UUID entity) {
        this.entity = entity;
        setDirty();
    }

    private static CoreState fromNbt(CompoundTag nbt) {
        CoreState state = new CoreState();
        if (nbt.get(KEY) != null) {
            Saved.CODEC.parse(NbtOps.INSTANCE, nbt.get(KEY))
                    .resultOrPartial(error -> DigitalFrontier.LOGGER.error("Couldn't read the grid's System Core: {}", error))
                    .ifPresent(saved -> {
                        state.phase = saved.phase();
                        state.entity = saved.entity().orElse(null);
                    });
        }
        return state;
    }

    private CompoundTag writeNbt(CompoundTag nbt) {
        Saved.CODEC.encodeStart(NbtOps.INSTANCE, new Saved(phase, Optional.ofNullable(entity))).ifSuccess(encoded -> nbt.put(KEY, encoded));
        return nbt;
    }
}
