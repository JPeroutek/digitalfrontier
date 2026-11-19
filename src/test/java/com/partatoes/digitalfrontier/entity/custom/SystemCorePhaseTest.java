package com.partatoes.digitalfrontier.entity.custom;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SystemCorePhaseTest {
    @Test
    void phasesFollowItsHealth() {
        assertEquals(SystemCorePhase.ABSORBING, SystemCorePhase.of(1f));
        assertEquals(SystemCorePhase.ABSORBING, SystemCorePhase.of(0.5f));
        assertEquals(SystemCorePhase.EXPOSED, SystemCorePhase.of(0.49f));
        assertEquals(SystemCorePhase.EXPOSED, SystemCorePhase.of(0.2f));
        assertEquals(SystemCorePhase.ENRAGED, SystemCorePhase.of(0.19f));
        assertEquals(SystemCorePhase.ENRAGED, SystemCorePhase.of(0f));
    }

    @Test
    void onlyAWholeCoreFeedsAndOnlyABrokenOneFights() {
        assertTrue(SystemCorePhase.ABSORBING.absorbs());
        assertTrue(SystemCorePhase.ABSORBING.feedEvery() > 0);
        assertEquals(0, SystemCorePhase.ABSORBING.boltEvery());
        for (SystemCorePhase phase : new SystemCorePhase[]{SystemCorePhase.EXPOSED, SystemCorePhase.ENRAGED}) {
            assertFalse(phase.absorbs());
            assertEquals(0, phase.feedEvery());
            assertTrue(phase.boltEvery() > 0 && phase.squadEvery() > 0);
        }
        assertTrue(SystemCorePhase.ENRAGED.boltEvery() < SystemCorePhase.EXPOSED.boltEvery(), "enraged is faster");
        assertTrue(SystemCorePhase.ENRAGED.squadEvery() < SystemCorePhase.EXPOSED.squadEvery());
    }

    @Test
    void boltsVaryAndEnragedAddsTheGlitch() {
        assertTrue(SystemCorePhase.ABSORBING.bolts().isEmpty());
        assertFalse(SystemCorePhase.EXPOSED.bolts().contains(CoreBoltEffect.GLITCH));
        assertEquals(List.of(CoreBoltEffect.values()), SystemCorePhase.ENRAGED.bolts());
        // Every bolt of a phase comes up across the rolls
        for (SystemCorePhase phase : new SystemCorePhase[]{SystemCorePhase.EXPOSED, SystemCorePhase.ENRAGED}) {
            Set<CoreBoltEffect> fired = EnumSet.noneOf(CoreBoltEffect.class);
            for (int i = 0; i < 100; i++) {
                fired.add(phase.bolt(i / 100.0));
            }
            assertEquals(Set.copyOf(phase.bolts()), fired);
            assertEquals(phase.bolts().getLast(), phase.bolt(0.9999));
        }
    }
}
