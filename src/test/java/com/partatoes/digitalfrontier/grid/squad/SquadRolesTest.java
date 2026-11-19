package com.partatoes.digitalfrontier.grid.squad;

import com.partatoes.digitalfrontier.grid.layout.ControlTier;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SquadRolesTest {
    private static Map<SquadRoles, Integer> counts(ControlTier tier) {
        Map<SquadRoles, Integer> counts = new EnumMap<>(SquadRoles.class);
        for (int i = 0; i < 1000; i++) {
            counts.merge(SquadRoles.roll(tier, i / 1000.0), 1, Integer::sum);
        }
        return counts;
    }

    @Test
    void everyRoleTurnsUpAndPlainLeadsOnPatrol() {
        Map<SquadRoles, Integer> patrolled = counts(ControlTier.PATROLLED);
        assertEquals(4, patrolled.size(), patrolled::toString);
        assertEquals(700, patrolled.get(SquadRoles.PLAIN));
    }

    @Test
    void theSeatFieldsHeavierSquads() {
        Map<SquadRoles, Integer> patrolled = counts(ControlTier.PATROLLED);
        Map<SquadRoles, Integer> seat = counts(ControlTier.SEAT);
        assertTrue(seat.get(SquadRoles.HEAVY) > patrolled.get(SquadRoles.HEAVY));
        assertTrue(seat.get(SquadRoles.GRENADIER) > patrolled.get(SquadRoles.GRENADIER));
        assertTrue(seat.get(SquadRoles.PLAIN) < patrolled.get(SquadRoles.PLAIN));
    }

    @Test
    void rollsAtTheEdgesStayInRange() {
        assertEquals(SquadRoles.PLAIN, SquadRoles.roll(ControlTier.SEAT, 0));
        assertEquals(SquadRoles.GRENADIER, SquadRoles.roll(ControlTier.SEAT, 0.99999));
    }

    @Test
    void squadsGrowWithControl() {
        assertEquals(2, SquadRoles.size(ControlTier.PATROLLED, 0.9));
        assertEquals(2, SquadRoles.size(ControlTier.CONTROLLED, 0));
        assertEquals(3, SquadRoles.size(ControlTier.CONTROLLED, 0.99));
        assertEquals(3, SquadRoles.size(ControlTier.SEAT, 0));
        assertEquals(4, SquadRoles.size(ControlTier.SEAT, 0.99));
    }
}
