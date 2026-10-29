package com.partatoes.digitalfrontier.compat;

import com.partatoes.digitalfrontier.grid.exit.MemoryPaging;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.event.TrinketDropCallback;
import net.minecraft.world.entity.player.Player;

/**
 * Trinkets Updated: a Core Dump keeps equipped trinkets too. Trinkets Updated copies whatever stays equipped to
 * the respawned player itself. Only loaded when Trinkets Updated is installed.
 */
public final class TrinketsCompat {
    private TrinketsCompat() {
    }

    public static void register() {
        // Only the default rule is overridden, as with keepInventory: items and slots that always drop or destroy
        // still do. The event runs before the slot's own rule is applied, so that's checked here too
        TrinketDropCallback.EVENT.register((rule, stack, slot, entity) ->
                rule == TrinketDropRule.DEFAULT && slot.slotType().dropRule() == TrinketDropRule.DEFAULT
                        && entity instanceof Player player && MemoryPaging.keepsInventory(player)
                        ? TrinketDropRule.KEEP
                        : rule);
    }
}
