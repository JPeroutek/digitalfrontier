package com.partatoes.digitalfrontier.compat;

import com.partatoes.digitalfrontier.grid.exit.MemoryPaging;
import io.wispforest.accessories.api.events.DropRule;
import io.wispforest.accessories.api.events.OnDropCallback;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Accessories (and Trinkets mods running on its Trinkets compatibility layer): a Core Dump keeps equipped
 * accessories too. Accessories copies whatever stays equipped to the respawned player itself.
 * Only loaded when Accessories is installed.
 */
public final class AccessoriesCompat {
    private AccessoriesCompat() {
    }

    public static void register() {
        // Only the default rule is overridden, as with keepInventory: slots that always drop or destroy still do
        OnDropCallback.EVENT.register((dropRule, stack, reference, damageSource) ->
                dropRule == DropRule.DEFAULT && reference.entity() instanceof PlayerEntity player && MemoryPaging.keepsInventory(player)
                        ? DropRule.KEEP
                        : null);
    }
}
