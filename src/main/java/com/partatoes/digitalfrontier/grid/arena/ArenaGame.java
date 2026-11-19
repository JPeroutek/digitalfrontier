package com.partatoes.digitalfrontier.grid.arena;

import java.util.Locale;
import java.util.Optional;

/** The games the arena holds (see {@link ArenaManager}). */
public enum ArenaGame {
    /** One on one with the champion, discs only, on tiles that fall away. */
    DISC_DUEL,
    /** Light cycles that lay walls; last rider standing. */
    LIGHT_CYCLE,
    /** Catch and throw a ball through the other side's ring; first to three. */
    RING_GAME;

    /** Its name in commands and saved data, e.g. {@code disc_duel}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "arena.digitalfrontier." + id();
    }

    public static Optional<ArenaGame> byId(String id) {
        for (ArenaGame game : values()) {
            if (game.id().equals(id)) {
                return Optional.of(game);
            }
        }
        return Optional.empty();
    }
}
