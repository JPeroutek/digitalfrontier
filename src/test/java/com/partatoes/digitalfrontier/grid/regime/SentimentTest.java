package com.partatoes.digitalfrontier.grid.regime;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SentimentTest {
    private static double[] sample(float control, float regime) {
        Random random = new Random(42);
        double[] values = new double[20000];
        for (int i = 0; i < values.length; i++) {
            values[i] = Sentiment.initial(random.nextDouble(), random.nextDouble(), random.nextDouble(), control, regime);
        }
        return values;
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private static double spread(double[] values) {
        double mean = mean(values);
        double sum = 0;
        for (double value : values) {
            sum += (value - mean) * (value - mean);
        }
        return Math.sqrt(sum / values.length);
    }

    @Test
    void newProgramsLeanLoyalWhereTheRegimeHoldsTheGround() {
        double free = mean(sample(0f, 0.5f));
        double held = mean(sample(1f, 0.5f));
        assertEquals(0, free, 0.02);
        assertEquals(0.3, held, 0.02);
        assertTrue(mean(sample(0f, 1f)) > free, "a stronger regime has more loyal subjects");
    }

    @Test
    void sentimentsVaryWidelyButStayInRange() {
        double[] values = sample(0f, 0.5f);
        assertEquals(Sentiment.SPREAD, spread(values), 0.02);
        long rebels = 0;
        long loyal = 0;
        for (double value : values) {
            assertTrue(value >= -1 && value <= 1);
            if (value <= Sentiment.REBEL) {
                rebels++;
            } else if (value >= Sentiment.LOYAL) {
                loyal++;
            }
        }
        // Each side a real minority in a free city, not absent and not the norm
        assertTrue(rebels > values.length * 0.03 && rebels < values.length * 0.2, "rebels " + rebels);
        assertTrue(loyal > values.length * 0.03 && loyal < values.length * 0.2, "loyal " + loyal);
    }

    @Test
    void stancesFollowTheThresholds() {
        assertEquals(Sentiment.Stance.RESISTANCE, Sentiment.stance(-0.9f));
        assertEquals(Sentiment.Stance.REBEL, Sentiment.stance(-0.5f));
        assertEquals(Sentiment.Stance.NEUTRAL, Sentiment.stance(0f));
        assertEquals(Sentiment.Stance.LOYAL, Sentiment.stance(0.5f));
    }

    @Test
    void playersPullProgramsTowardTheirSide() {
        assertTrue(Sentiment.byPlayer(0f, 0.8f, 0.1f) > 0, "a loyalist's gift warms it to the regime");
        assertTrue(Sentiment.byPlayer(0f, -0.8f, 0.1f) < 0, "a rebel's gift turns it against the regime");
        assertTrue(Sentiment.byPlayer(0f, 0.8f, -0.2f) < 0, "hurt by a loyalist, it turns from the regime");
        assertTrue(Sentiment.byPlayer(0f, -0.8f, -0.2f) > 0, "hurt by a rebel, it turns to the regime");
        // A player who hasn't picked a side still counts, as an outsider
        assertEquals(-Sentiment.MIN_PLAYER_EFFECT, Sentiment.byPlayer(0f, 0f, 0.1f), 1e-6);
        assertTrue(Math.abs(Sentiment.byPlayer(0f, -1f, 0.15f)) > Math.abs(Sentiment.byPlayer(0f, -0.2f, 0.15f)),
                "a committed player sways harder");
        assertEquals(1f, Sentiment.byPlayer(0.99f, 1f, 0.5f), "clamped");
    }

    @Test
    void recruitersWinOverAllButTheSetAgainstThem() {
        assertTrue(Sentiment.exposed(0f, 0.01f) > 0);
        assertTrue(Sentiment.exposed(0.9f, 0.01f) > 0.9f);
        assertTrue(Sentiment.exposed(-0.7f, 0.01f) < -0.7f, "backlash");
        assertTrue(Sentiment.exposed(-0.5f, 0.01f) > -0.5f, "a mere rebel can still be worn down");
    }
}
