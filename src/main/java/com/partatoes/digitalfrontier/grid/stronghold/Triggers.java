package com.partatoes.digitalfrontier.grid.stronghold;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.grid.stronghold.Trigger.EventTrigger;
import com.partatoes.digitalfrontier.grid.stronghold.Trigger.StateTrigger;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/** Every trigger type, by its JSON {@code "type"}. */
public final class Triggers {
    /** Default radius, in blocks around the stronghold, for triggers about things happening at it. */
    private static final int STRONGHOLD_RADIUS = 32;

    // In a holder class, not a field of Triggers: each trigger record's codec calls Triggers.unit while initializing,
    // which would otherwise build this map before that codec exists
    private static final class Codecs {
        static final Map<String, MapCodec<? extends Trigger>> BY_TYPE = Map.ofEntries(
                // States
                Map.entry(Time.TYPE, Time.CODEC),
                Map.entry(MoonPhase.TYPE, MoonPhase.CODEC),
                Map.entry(Thunder.TYPE, Thunder.CODEC),
                Map.entry(Score.TYPE, Score.CODEC),
                Map.entry(AllDefeated.TYPE, AllDefeated.CODEC),
                // Events
                Map.entry(GridEntered.TYPE, GridEntered.CODEC),
                Map.entry(StrongholdEntered.TYPE, StrongholdEntered.CODEC),
                Map.entry(ItemCrafted.TYPE, ItemCrafted.CODEC),
                Map.entry(ItemUsed.TYPE, ItemUsed.CODEC),
                Map.entry(BlockPlaced.TYPE, BlockPlaced.CODEC),
                Map.entry(BlockBroken.TYPE, BlockBroken.CODEC),
                Map.entry(Redstone.TYPE, Redstone.CODEC),
                Map.entry(LieutenantDefeated.TYPE, LieutenantDefeated.CODEC),
                Map.entry(PlayerDerezzed.TYPE, PlayerDerezzed.CODEC),
                Map.entry(PortalUsed.TYPE, PortalUsed.CODEC),
                Map.entry(ProgramDerezzed.TYPE, ProgramDerezzed.CODEC),
                Map.entry(MemoryPaged.TYPE, MemoryPaged.CODEC),
                Map.entry(Function.TYPE, Function.CODEC),
                // Combinators
                Map.entry(AnyOf.TYPE, AnyOf.CODEC),
                Map.entry(AllOf.TYPE, AllOf.CODEC),
                Map.entry(Not.TYPE, Not.CODEC),
                Map.entry(Chance.TYPE, Chance.CODEC),
                Map.entry(Count.TYPE, Count.CODEC));
    }

    private Triggers() {
    }

    static DataResult<MapCodec<? extends Trigger>> codec(String type) {
        MapCodec<? extends Trigger> codec = Codecs.BY_TYPE.get(type);
        return codec != null ? DataResult.success(codec) : DataResult.error(() -> "Unknown lieutenant trigger type " + type);
    }

    public static Set<String> types() {
        return Codecs.BY_TYPE.keySet();
    }

    // Horizontal distance from the stronghold; a negative radius means anywhere in the grid
    private static boolean within(@Nullable BlockPos pos, TriggerContext context, int radius) {
        if (radius < 0 || pos == null) {
            return true;
        }
        double dx = pos.getX() + 0.5 - context.center().getX();
        double dz = pos.getZ() + 0.5 - context.center().getZ();
        return dx * dx + dz * dz <= (double) radius * radius;
    }

    // A fresh instance per occurrence: leaves are told apart by identity, so two identical ones keep separate counts
    private static <T> MapCodec<T> unit(Supplier<T> constructor) {
        return MapCodec.unit(constructor);
    }

    // ---- States ----

    /** Enough game time has passed since the defeat. */
    public record Time(long ticks) implements StateTrigger {
        static final String TYPE = "time";
        static final MapCodec<Time> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.LONG.fieldOf("ticks").forGetter(Time::ticks)).apply(i, Time::new));

        public String type() {
            return TYPE;
        }

        public boolean holds(TriggerContext context) {
            return context.grid().getTime() - context.defeatedAt() >= ticks;
        }
    }

    /** The real world's moon is in one of these phases (0 is full, 4 is new). */
    public record MoonPhase(List<Integer> phases) implements StateTrigger {
        static final String TYPE = "overworld_moon_phase";
        static final MapCodec<MoonPhase> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.intRange(0, 7).listOf().fieldOf("phases").forGetter(MoonPhase::phases)).apply(i, MoonPhase::new));

        public String type() {
            return TYPE;
        }

        public boolean holds(TriggerContext context) {
            ServerWorld overworld = context.grid().getServer().getOverworld();
            return phases.contains(overworld.getDimension().getMoonPhase(overworld.getLunarTime()));
        }
    }

    /** A thunderstorm is raging in the real world: a power surge. */
    public record Thunder() implements StateTrigger {
        static final String TYPE = "overworld_thunder";
        static final MapCodec<Thunder> CODEC = unit(Thunder::new);

        public String type() {
            return TYPE;
        }

        public boolean holds(TriggerContext context) {
            return context.grid().getServer().getOverworld().isThundering();
        }
    }

    /** A scoreboard score has reached a value; {@code holder} is a player name or a fake one like {@code #regime}. */
    public record Score(String objective, String holder, int min) implements StateTrigger {
        static final String TYPE = "scoreboard";
        static final MapCodec<Score> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("objective").forGetter(Score::objective),
                Codec.STRING.fieldOf("holder").forGetter(Score::holder),
                Codec.INT.fieldOf("min").forGetter(Score::min)).apply(i, Score::new));

        public String type() {
            return TYPE;
        }

        public boolean holds(TriggerContext context) {
            Scoreboard scoreboard = context.grid().getServer().getScoreboard();
            ScoreboardObjective scoreObjective = scoreboard.getNullableObjective(objective);
            if (scoreObjective == null) {
                return false;
            }
            ReadableScoreboardScore score = scoreboard.getScore(ScoreHolder.fromName(holder), scoreObjective);
            return score != null && score.getScore() >= min;
        }
    }

    /** Every stronghold in the grid has fallen. */
    public record AllDefeated() implements StateTrigger {
        static final String TYPE = "all_lieutenants_defeated";
        static final MapCodec<AllDefeated> CODEC = unit(AllDefeated::new);

        public String type() {
            return TYPE;
        }

        public boolean holds(TriggerContext context) {
            return context.allDefeated();
        }
    }

    // ---- Events ----

    /** A player arrived in the grid; optionally only players who had never been to it before. */
    public record GridEntered(boolean newPlayersOnly) implements EventTrigger {
        static final String TYPE = "grid_entered";
        static final MapCodec<GridEntered> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.BOOL.optionalFieldOf("new_players_only", false).forGetter(GridEntered::newPlayersOnly)).apply(i, GridEntered::new));

        public String type() {
            return TYPE;
        }

        public boolean matches(GridEvent event, TriggerContext context) {
            return event instanceof GridEvent.GridEntered entered && (!newPlayersOnly || entered.newPlayer());
        }
    }

    /** A player came near the stronghold after it had been deserted for at least {@code after_absence} ticks. */
    public record StrongholdEntered(long afterAbsence) implements EventTrigger {
        static final String TYPE = "stronghold_entered";
        static final MapCodec<StrongholdEntered> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.LONG.optionalFieldOf("after_absence", 0L).forGetter(StrongholdEntered::afterAbsence)).apply(i, StrongholdEntered::new));

        public String type() {
            return TYPE;
        }

        public boolean matches(GridEvent event, TriggerContext context) {
            return event instanceof GridEvent.StrongholdEntered entered && entered.absence() >= afterAbsence;
        }
    }

    /** Someone crafted a matching item, anywhere (crafting in the real world counts for every grid). */
    public record ItemCrafted(Optional<RegistryMatcher> item) implements EventTrigger {
        static final String TYPE = "item_crafted";
        static final MapCodec<ItemCrafted> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                RegistryMatcher.CODEC.optionalFieldOf("item").forGetter(ItemCrafted::item)).apply(i, ItemCrafted::new));

        public String type() {
            return TYPE;
        }

        public boolean matches(GridEvent event, TriggerContext context) {
            return event instanceof GridEvent.ItemCrafted crafted && item.map(matcher -> matcher.matches(crafted.stack())).orElse(true);
        }
    }

    /** A player used a matching item near the stronghold, like an offering; it can be used up. */
    public record ItemUsed(Optional<RegistryMatcher> item, int radius, boolean consume) implements EventTrigger {
        static final String TYPE = "item_used_at_stronghold";
        static final MapCodec<ItemUsed> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                RegistryMatcher.CODEC.optionalFieldOf("item").forGetter(ItemUsed::item),
                Codec.INT.optionalFieldOf("radius", 16).forGetter(ItemUsed::radius),
                Codec.BOOL.optionalFieldOf("consume", true).forGetter(ItemUsed::consume)).apply(i, ItemUsed::new));

        public String type() {
            return TYPE;
        }

        public boolean matches(GridEvent event, TriggerContext context) {
            return event instanceof GridEvent.ItemUsed used && within(used.pos(), context, radius)
                    && item.map(matcher -> matcher.matches(used.stack())).orElse(true);
        }
    }

    /** A matching block was placed near the stronghold ({@code radius} -1: anywhere in the grid). */
    public record BlockPlaced(Optional<RegistryMatcher> block, int radius) implements EventTrigger {
        static final String TYPE = "block_placed";
        static final MapCodec<BlockPlaced> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                RegistryMatcher.CODEC.optionalFieldOf("block").forGetter(BlockPlaced::block),
                Codec.INT.optionalFieldOf("radius", STRONGHOLD_RADIUS).forGetter(BlockPlaced::radius)).apply(i, BlockPlaced::new));

        public String type() {
            return TYPE;
        }

        public boolean matches(GridEvent event, TriggerContext context) {
            return event instanceof GridEvent.BlockPlaced placed && within(placed.pos(), context, radius)
                    && block.map(matcher -> matcher.matches(placed.state())).orElse(true);
        }
    }

    /** A matching block was broken near the stronghold ({@code radius} -1: anywhere in the grid). */
    public record BlockBroken(Optional<RegistryMatcher> block, int radius) implements EventTrigger {
        static final String TYPE = "block_broken";
        static final MapCodec<BlockBroken> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                RegistryMatcher.CODEC.optionalFieldOf("block").forGetter(BlockBroken::block),
                Codec.INT.optionalFieldOf("radius", STRONGHOLD_RADIUS).forGetter(BlockBroken::radius)).apply(i, BlockBroken::new));

        public String type() {
            return TYPE;
        }

        public boolean matches(GridEvent event, TriggerContext context) {
            return event instanceof GridEvent.BlockBroken broken && within(broken.pos(), context, radius)
                    && block.map(matcher -> matcher.matches(broken.state())).orElse(true);
        }
    }

    /** A Stronghold Uplink near the stronghold received a redstone signal. */
    public record Redstone(int radius) implements EventTrigger {
        static final String TYPE = "redstone";
        static final MapCodec<Redstone> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("radius", STRONGHOLD_RADIUS).forGetter(Redstone::radius)).apply(i, Redstone::new));

        public String type() {
            return TYPE;
        }

        public boolean matches(GridEvent event, TriggerContext context) {
            return event instanceof GridEvent.Redstone redstone && within(redstone.pos(), context, radius);
        }
    }

    /** A lieutenant of this grid fell: by default only a different one (when one falls, another rises). */
    public record LieutenantDefeated(Optional<Identifier> lieutenant, boolean othersOnly) implements EventTrigger {
        static final String TYPE = "lieutenant_defeated";
        static final MapCodec<LieutenantDefeated> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.optionalFieldOf("lieutenant").forGetter(LieutenantDefeated::lieutenant),
                Codec.BOOL.optionalFieldOf("others_only", true).forGetter(LieutenantDefeated::othersOnly)).apply(i, LieutenantDefeated::new));

        public String type() {
            return TYPE;
        }

        public boolean matches(GridEvent event, TriggerContext context) {
            return event instanceof GridEvent.LieutenantDefeated defeated
                    && (!othersOnly || !defeated.lieutenant().equals(context.lieutenant()))
                    && lieutenant.map(id -> id.equals(defeated.lieutenant())).orElse(true);
        }
    }

    /** A player died in this grid: the regime feeds on it. */
    public record PlayerDerezzed(int radius) implements EventTrigger {
        static final String TYPE = "player_derezzed";
        static final MapCodec<PlayerDerezzed> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("radius", -1).forGetter(PlayerDerezzed::radius)).apply(i, PlayerDerezzed::new));

        public String type() {
            return TYPE;
        }

        public boolean matches(GridEvent event, TriggerContext context) {
            return event instanceof GridEvent.PlayerDerezzed derezzed && within(derezzed.pos(), context, radius);
        }
    }

    /** Someone fled this grid through its Portal. */
    public record PortalUsed() implements EventTrigger {
        static final String TYPE = "portal_used";
        static final MapCodec<PortalUsed> CODEC = unit(PortalUsed::new);

        public String type() {
            return TYPE;
        }

        public boolean matches(GridEvent event, TriggerContext context) {
            return event instanceof GridEvent.PortalUsed;
        }
    }

    /** A player derezzed a clean program in this grid. */
    public record ProgramDerezzed(int radius) implements EventTrigger {
        static final String TYPE = "program_derezzed";
        static final MapCodec<ProgramDerezzed> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("radius", -1).forGetter(ProgramDerezzed::radius)).apply(i, ProgramDerezzed::new));

        public String type() {
            return TYPE;
        }

        public boolean matches(GridEvent event, TriggerContext context) {
            return event instanceof GridEvent.ProgramDerezzed derezzed && within(derezzed.pos(), context, radius);
        }
    }

    /** A player cheated death in this grid with Memory Paging or a Core Dump. */
    public record MemoryPaged() implements EventTrigger {
        static final String TYPE = "memory_paged";
        static final MapCodec<MemoryPaged> CODEC = unit(MemoryPaged::new);

        public String type() {
            return TYPE;
        }

        public boolean matches(GridEvent event, TriggerContext context) {
            return event instanceof GridEvent.MemoryPaged;
        }
    }

    /** {@code /grid lieutenant trigger <grid> [signal]} was run; with a signal set, only that signal counts. */
    public record Function(Optional<String> signal) implements EventTrigger {
        static final String TYPE = "function";
        static final MapCodec<Function> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("signal").forGetter(Function::signal)).apply(i, Function::new));

        public String type() {
            return TYPE;
        }

        public boolean matches(GridEvent event, TriggerContext context) {
            return event instanceof GridEvent.Function function
                    && signal.map(expected -> expected.equals(function.signal())).orElse(true);
        }
    }

    // ---- Combinators ----

    public record AnyOf(List<Trigger> triggers) implements Trigger {
        static final String TYPE = "any_of";
        static final MapCodec<AnyOf> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Trigger.CODEC.listOf(1, 64).fieldOf("triggers").forGetter(AnyOf::triggers)).apply(i, AnyOf::new));

        public String type() {
            return TYPE;
        }

        public boolean test(TriggerContext context) {
            return triggers.stream().anyMatch(trigger -> trigger.test(context));
        }

        public void leaves(List<Trigger> out) {
            triggers.forEach(trigger -> trigger.leaves(out));
        }
    }

    public record AllOf(List<Trigger> triggers) implements Trigger {
        static final String TYPE = "all_of";
        static final MapCodec<AllOf> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Trigger.CODEC.listOf(1, 64).fieldOf("triggers").forGetter(AllOf::triggers)).apply(i, AllOf::new));

        public String type() {
            return TYPE;
        }

        public boolean test(TriggerContext context) {
            return triggers.stream().allMatch(trigger -> trigger.test(context));
        }

        public void leaves(List<Trigger> out) {
            triggers.forEach(trigger -> trigger.leaves(out));
        }
    }

    public record Not(Trigger trigger) implements Trigger {
        static final String TYPE = "not";
        static final MapCodec<Not> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Trigger.CODEC.fieldOf("trigger").forGetter(Not::trigger)).apply(i, Not::new));

        public String type() {
            return TYPE;
        }

        public boolean test(TriggerContext context) {
            return !trigger.test(context);
        }

        public void leaves(List<Trigger> out) {
            trigger.leaves(out);
        }
    }

    /**
     * Each time the inner events fire, they only count with this probability. Inner states are rolled once a
     * second while they hold, and stay satisfied once a roll succeeds.
     */
    public record Chance(float chance, Trigger trigger) implements Trigger {
        static final String TYPE = "chance";
        static final MapCodec<Chance> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.floatRange(0f, 1f).fieldOf("chance").forGetter(Chance::chance),
                Trigger.CODEC.fieldOf("trigger").forGetter(Chance::trigger)).apply(i, Chance::new));

        public String type() {
            return TYPE;
        }

        public boolean test(TriggerContext context) {
            return trigger.test(context);
        }

        public void leaves(List<Trigger> out) {
            trigger.leaves(out);
        }
    }

    /** An event has happened at least {@code count} times since the defeat. */
    public record Count(int count, Trigger trigger) implements Trigger {
        static final String TYPE = "count";
        static final MapCodec<Count> CODEC = RecordCodecBuilder.<Count>mapCodec(i -> i.group(
                Codec.intRange(1, 10_000).fieldOf("count").forGetter(Count::count),
                Trigger.CODEC.fieldOf("trigger").forGetter(Count::trigger)).apply(i, Count::new))
                .validate(c -> c.trigger instanceof EventTrigger
                        ? DataResult.success(c)
                        : DataResult.error(() -> "count only works around a single event trigger, not " + c.trigger.type()));

        public String type() {
            return TYPE;
        }

        public boolean test(TriggerContext context) {
            return context.count(trigger) >= count;
        }

        public void leaves(List<Trigger> out) {
            trigger.leaves(out);
        }
    }
}
