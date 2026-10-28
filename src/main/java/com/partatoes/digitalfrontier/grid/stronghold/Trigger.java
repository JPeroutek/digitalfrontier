package com.partatoes.digitalfrontier.grid.stronghold;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.List;

/**
 * A condition for a defeated lieutenant to return, written in JSON as an object with a {@code "type"}
 * (see {@link Triggers} for every type). There are three kinds:
 * <ul>
 *   <li>{@link StateTrigger}s look at the world right now, e.g. "7 days have passed" or "it's thundering".</li>
 *   <li>{@link EventTrigger}s count {@link GridEvent}s since the defeat, e.g. "a player died in this grid".</li>
 *   <li>Combinators ({@code any_of}, {@code all_of}, {@code not}, {@code chance}, {@code count}) combine others.</li>
 * </ul>
 */
public interface Trigger {
    Codec<Trigger> CODEC = Codec.lazyInitialized(() -> Codec.STRING.partialDispatch("type",
            trigger -> DataResult.success(trigger.type()), Triggers::codec));

    String type();

    /** Whether the trigger is satisfied now. */
    boolean test(TriggerContext context);

    /** The state and event triggers in this tree, depth first; combinators only pass through their children's. */
    default void leaves(List<Trigger> out) {
        out.add(this);
    }

    /** Looks at the world each time the stronghold is checked (once a second). */
    interface StateTrigger extends Trigger {
        boolean holds(TriggerContext context);

        @Override
        default boolean test(TriggerContext context) {
            // Under a chance, a state only counts once a check has rolled successfully while it held
            return context.chance(this) < 1 ? context.count(this) > 0 : holds(context);
        }
    }

    /** Counts matching events since the lieutenant was defeated; satisfied once one has happened. */
    interface EventTrigger extends Trigger {
        boolean matches(GridEvent event, TriggerContext context);

        @Override
        default boolean test(TriggerContext context) {
            return context.count(this) > 0;
        }
    }
}
