package com.partatoes.digitalfrontier.grid.jobs;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * A program with a job: it stays at its post, trades (offers from {@code data/<namespace>/grid/job/<job>.json}, see
 * {@link JobTrades}), and never drifts into decay or joins the regime. Buildings name the jobs that work in them (see
 * {@code residents} in {@link com.partatoes.digitalfrontier.grid.parts.GridBuildingDef}).
 */
public enum ProgramJob implements StringRepresentable {
    /** Works on light cycles in a garage. */
    MECHANIC("mechanic"),
    /** Keeps the Portal: explains the way out, and sells it. */
    PORTAL_KEEPER("portal_keeper");

    public static final Codec<ProgramJob> CODEC = StringRepresentable.fromEnum(ProgramJob::values);

    private final String name;

    ProgramJob(String name) {
        this.name = name;
    }

    @Nullable
    public static ProgramJob byName(String name) {
        for (ProgramJob job : values()) {
            if (job.name.equals(name)) {
                return job;
            }
        }
        return null;
    }

    /** The job's title, shown over the program: {@code entity.digitalfrontier.program.<job>}. */
    public String translationKey() {
        return "entity.digitalfrontier.program." + name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
