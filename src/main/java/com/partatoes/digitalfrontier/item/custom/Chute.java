package com.partatoes.digitalfrontier.item.custom;

import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

/**
 * The Chute: worn on the back, it glides like an elytra (its glider component), and opens by itself once its wearer
 * has fallen a few blocks, so stepping off a skyroad (or out of a Recognizer) is a ride down rather than a fall.
 */
public final class Chute {
    /** How far a wearer falls before the Chute opens. */
    public static final double OPENS_AFTER = 4;

    private Chute() {
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(world -> {
            for (Player player : world.players()) {
                if (shouldOpen(player)) {
                    player.startFallFlying();
                }
            }
        });
    }

    /** Whether a falling player's Chute should open now. */
    public static boolean shouldOpen(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CHUTE)
                && !player.onGround() && !player.isFallFlying() && !player.isInWater() && !player.isPassenger()
                && !player.getAbilities().flying && player.fallDistance >= OPENS_AFTER;
    }
}
