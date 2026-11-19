package com.partatoes.digitalfrontier.grid.stronghold;

import com.partatoes.digitalfrontier.entity.custom.LieutenantEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * What a trigger can see about the stronghold whose lieutenant it might bring back, or, for a {@code restore}
 * trigger, redeem.
 */
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

    /**
     * The defeat being judged, while a {@code restore} trigger decides whether a lieutenant is redeemed; null at any
     * other time, so triggers about the defeat never bring a lieutenant back.
     */
    @Nullable
    default Defeat defeat() {
        return null;
    }

    /** A lieutenant brought down, and the player who did it, if a player did. */
    record Defeat(LieutenantEntity lieutenant, @Nullable Player killer) {
    }
}
