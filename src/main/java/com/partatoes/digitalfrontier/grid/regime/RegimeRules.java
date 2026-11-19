package com.partatoes.digitalfrontier.grid.regime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The regime's numbers, from {@code data/digitalfrontier/grid/regime.json} when a datapack provides it. Every field is
 * optional; what's left out keeps its default. Sentiment and alignment run from -1 to 1.
 */
public record RegimeRules(Recruiting recruiting, Sentiments sentiment, Alignment alignment) {
    public static final RegimeRules DEFAULT = new RegimeRules(Recruiting.DEFAULT, Sentiments.DEFAULT, Alignment.DEFAULT);

    public static final Codec<RegimeRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Recruiting.CODEC.optionalFieldOf("recruiting", Recruiting.DEFAULT).forGetter(RegimeRules::recruiting),
            Sentiments.CODEC.optionalFieldOf("sentiment", Sentiments.DEFAULT).forGetter(RegimeRules::sentiment),
            Alignment.CODEC.optionalFieldOf("alignment", Alignment.DEFAULT).forGetter(RegimeRules::alignment)
    ).apply(instance, RegimeRules::new));

    /**
     * @param exposureChance    base chance a program joins after {@code exposureTicks} near a recruiter
     * @param bribeChance       base chance a program takes a bribe
     * @param exposureTicks     time near a recruiter between attempts
     * @param bribeInterval     ticks between a recruiter's bribes
     * @param exposureRadius    how close a recruiter has to be, in blocks
     * @param bribeRadius       how far a recruiter reaches with a bribe
     * @param idealPerStronghold the regime's ideal size per standing stronghold, at a 50% regime
     */
    public record Recruiting(float exposureChance, float bribeChance, int exposureTicks, int bribeInterval, int exposureRadius,
                             int bribeRadius, int idealPerStronghold) {
        public static final Recruiting DEFAULT = new Recruiting(0.15f, 0.35f, 100, 600, 4, 8, 8);
        public static final Codec<Recruiting> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.floatRange(0, 1).optionalFieldOf("exposure_chance", DEFAULT.exposureChance).forGetter(Recruiting::exposureChance),
                Codec.floatRange(0, 1).optionalFieldOf("bribe_chance", DEFAULT.bribeChance).forGetter(Recruiting::bribeChance),
                Codec.intRange(1, 72000).optionalFieldOf("exposure_ticks", DEFAULT.exposureTicks).forGetter(Recruiting::exposureTicks),
                Codec.intRange(20, 72000).optionalFieldOf("bribe_interval", DEFAULT.bribeInterval).forGetter(Recruiting::bribeInterval),
                Codec.intRange(1, 32).optionalFieldOf("exposure_radius", DEFAULT.exposureRadius).forGetter(Recruiting::exposureRadius),
                Codec.intRange(1, 32).optionalFieldOf("bribe_radius", DEFAULT.bribeRadius).forGetter(Recruiting::bribeRadius),
                Codec.intRange(0, 1000).optionalFieldOf("ideal_per_stronghold", DEFAULT.idealPerStronghold).forGetter(Recruiting::idealPerStronghold)
        ).apply(instance, Recruiting::new));
    }

    /**
     * How much things move a program's sentiment.
     *
     * @param exposurePerSecond each second near a recruiter (toward the regime, or away for a program set against it)
     * @param brutality         seeing an enforcer kill a program, within 16 blocks
     * @param protection        an enforcer killing a corrupted program that was attacking it
     * @param bribeRefused      turning a bribe down
     * @param gift              a gift from a player, scaled by the player's alignment
     * @param defended          a player killing what attacked it, scaled by the player's alignment
     * @param hurt              a player hurting it, scaled by the player's alignment
     */
    public record Sentiments(float exposurePerSecond, float brutality, float protection, float bribeRefused, float gift,
                             float defended, float hurt) {
        public static final Sentiments DEFAULT = new Sentiments(0.01f, -0.15f, 0.1f, -0.05f, 0.1f, 0.15f, 0.2f);
        private static final Codec<Float> SHIFT = Codec.floatRange(-2, 2);
        public static final Codec<Sentiments> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SHIFT.optionalFieldOf("exposure_per_second", DEFAULT.exposurePerSecond).forGetter(Sentiments::exposurePerSecond),
                SHIFT.optionalFieldOf("brutality", DEFAULT.brutality).forGetter(Sentiments::brutality),
                SHIFT.optionalFieldOf("protection", DEFAULT.protection).forGetter(Sentiments::protection),
                SHIFT.optionalFieldOf("bribe_refused", DEFAULT.bribeRefused).forGetter(Sentiments::bribeRefused),
                SHIFT.optionalFieldOf("gift", DEFAULT.gift).forGetter(Sentiments::gift),
                SHIFT.optionalFieldOf("defended", DEFAULT.defended).forGetter(Sentiments::defended),
                SHIFT.optionalFieldOf("hurt", DEFAULT.hurt).forGetter(Sentiments::hurt)
        ).apply(instance, Sentiments::new));
    }

    /**
     * How players' acts move their alignment, and what alignment does.
     *
     * @param toleratedFrom enforcers leave players at or above this alone, unless attacked
     * @param reportedBelow loyal programs report players below this to enforcers
     * @param wantedBelow   enforcers hunt players below this from twice as far
     */
    public record Alignment(float tribute, float killRebel, float killEnforcer, float killRecruiter, float defeatLieutenant,
                            float killLoyal, float redeemLieutenant, float defeatCore, float toleratedFrom, float reportedBelow, float wantedBelow) {
        public static final Alignment DEFAULT = new Alignment(0.05f, 0.05f, -0.03f, -0.08f, -0.25f, -0.02f, -0.3f, -0.3f, 0.4f, -0.3f, -0.5f);
        private static final Codec<Float> VALUE = Codec.floatRange(-2, 2);
        public static final Codec<Alignment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                VALUE.optionalFieldOf("tribute", DEFAULT.tribute).forGetter(Alignment::tribute),
                VALUE.optionalFieldOf("kill_rebel", DEFAULT.killRebel).forGetter(Alignment::killRebel),
                VALUE.optionalFieldOf("kill_enforcer", DEFAULT.killEnforcer).forGetter(Alignment::killEnforcer),
                VALUE.optionalFieldOf("kill_recruiter", DEFAULT.killRecruiter).forGetter(Alignment::killRecruiter),
                VALUE.optionalFieldOf("defeat_lieutenant", DEFAULT.defeatLieutenant).forGetter(Alignment::defeatLieutenant),
                VALUE.optionalFieldOf("kill_loyal", DEFAULT.killLoyal).forGetter(Alignment::killLoyal),
                VALUE.optionalFieldOf("redeem_lieutenant", DEFAULT.redeemLieutenant).forGetter(Alignment::redeemLieutenant),
                VALUE.optionalFieldOf("defeat_core", DEFAULT.defeatCore).forGetter(Alignment::defeatCore),
                VALUE.optionalFieldOf("tolerated_from", DEFAULT.toleratedFrom).forGetter(Alignment::toleratedFrom),
                VALUE.optionalFieldOf("reported_below", DEFAULT.reportedBelow).forGetter(Alignment::reportedBelow),
                VALUE.optionalFieldOf("wanted_below", DEFAULT.wantedBelow).forGetter(Alignment::wantedBelow)
        ).apply(instance, Alignment::new));
    }
}
