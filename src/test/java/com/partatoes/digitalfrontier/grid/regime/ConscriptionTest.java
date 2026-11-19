package com.partatoes.digitalfrontier.grid.regime;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConscriptionTest {
    @Test
    void loyalistsJoinReadilyAndRebelsAlmostNever() {
        double previous = -1;
        for (float sentiment = -1; sentiment <= 1; sentiment += 0.1f) {
            double chance = Conscription.chance(0.35, sentiment, 0, 10);
            assertTrue(chance >= previous, "sentiment " + sentiment);
            previous = chance;
        }
        assertEquals(0.35, Conscription.chance(0.35, 1f, 0, 10), 1e-9);
        assertTrue(Conscription.chance(0.35, -0.9f, 0, 10) < 0.001);
    }

    @Test
    void anUnderstaffedRegimeRecruitsHarder() {
        double empty = Conscription.chance(0.15, 0.5f, 0, 10);
        double half = Conscription.chance(0.15, 0.5f, 5, 10);
        double full = Conscription.chance(0.15, 0.5f, 10, 10);
        double over = Conscription.chance(0.15, 0.5f, 20, 10);
        double far = Conscription.chance(0.15, 0.5f, 60, 10);
        assertTrue(empty > half && half > full && full > over && over > far);
        assertTrue(over < full / 2, "past its ideal, recruiting drops off quickly");
        assertTrue(far > 0, "but never stops");
    }

    @Test
    void withNoStrongholdStandingNobodyIsRecruited() {
        assertEquals(0, Conscription.ideal(0, 8, 0.9f));
        assertEquals(0, Conscription.chance(1, 1f, 0, 0));
        assertEquals(8, Conscription.ideal(1, 8, 0.5f));
        assertEquals(36, Conscription.ideal(3, 8, 1f));
    }

    @Test
    void rulesFillInWhatADatapackLeavesOut() {
        RegimeRules rules = RegimeRules.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"recruiting\": {\"bribe_chance\": 1.0}, \"alignment\": {\"tribute\": 0.2}}")).getOrThrow();
        assertEquals(1f, rules.recruiting().bribeChance());
        assertEquals(RegimeRules.Recruiting.DEFAULT.exposureChance(), rules.recruiting().exposureChance());
        assertEquals(0.2f, rules.alignment().tribute());
        assertEquals(RegimeRules.Sentiments.DEFAULT, rules.sentiment());
        assertEquals(RegimeRules.DEFAULT, RegimeRules.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow());
        assertTrue(RegimeRules.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"recruiting\": {\"bribe_chance\": 3}}")).isError());
    }
}
