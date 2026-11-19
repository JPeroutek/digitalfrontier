package com.partatoes.digitalfrontier.entity.custom;

/** What a System Core bolt does to whatever it hits (see {@link CoreBolt}); which ones it fires depends on its phase. */
public enum CoreBoltEffect {
    /** Holds the target in place for a second, frosting over. */
    FREEZE,
    /** Throws the target away from the core. */
    KNOCKBACK,
    /** Lifts the target off the floor for a few seconds. */
    LEVITATE,
    /** Blacks out the target's view for a few seconds. */
    GLITCH
}
