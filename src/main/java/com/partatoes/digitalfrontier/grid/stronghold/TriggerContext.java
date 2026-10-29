package com.partatoes.digitalfrontier.grid.stronghold;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

/** What a trigger can see about the stronghold whose lieutenant it might bring back. */
public interface TriggerContext {
    ServerLevel grid();

    /** The center of the stronghold, at street level. */
    BlockPos center();

    Identifier lieutenant();

    /** The stronghold's site id, e.g. {@code stronghold_0}. */
    String stronghold();

    /** Game time (of the grid world) when the lieutenant was defeated. */
    long defeatedAt();

    /** Whether every stronghold of this grid has a defeated lieutenant. */
    boolean allDefeated();

    /** How many times a leaf trigger has fired (events) or latched (states under a chance) since the defeat. */
    int count(Trigger leaf);

    /** The combined probability of the {@code chance} combinators around a leaf trigger; 1 if there are none. */
    double chance(Trigger leaf);
}
