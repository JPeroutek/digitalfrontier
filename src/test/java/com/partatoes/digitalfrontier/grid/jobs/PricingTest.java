package com.partatoes.digitalfrontier.grid.jobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PricingTest {
    @Test
    void likeMindsGetADiscountAndOppositesPayMore() {
        assertEquals(0.75f, Pricing.multiplier(0.9f, 0.9f), 1e-6);
        assertEquals(1.25f, Pricing.multiplier(-1f, 1f), 1e-6);
        assertEquals(1.0f, Pricing.multiplier(0f, 1f), 1e-6, "halfway apart, list price");
        // A loyal keeper overcharges rebels, and the other way round
        assertTrue(Pricing.multiplier(-0.8f, 0.9f) > Pricing.multiplier(0.8f, 0.9f));
        assertTrue(Pricing.multiplier(0.8f, -0.9f) > Pricing.multiplier(-0.8f, -0.9f));
    }

    @Test
    void theSwingIsAQuarterEitherWay() {
        for (float alignment = -1; alignment <= 1; alignment += 0.25f) {
            for (float sentiment = -1; sentiment <= 1; sentiment += 0.25f) {
                float multiplier = Pricing.multiplier(alignment, sentiment);
                assertTrue(multiplier >= 0.75f - 1e-6 && multiplier <= 1.25f + 1e-6);
            }
        }
    }

    @Test
    void pricesStayWholeAndAtLeastOne() {
        assertEquals(1, Pricing.price(1, 0.75f));
        assertEquals(12, Pricing.price(16, 0.75f));
        assertEquals(20, Pricing.price(16, 1.25f));
        assertEquals(40, Pricing.price(32, 1.25f));
    }
}
