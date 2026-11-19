package com.partatoes.digitalfrontier.effect;

import com.mojang.serialization.Codec;
import com.partatoes.digitalfrontier.DigitalFrontier;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Overclocking: a big boost that eats at the user's code. Drinking an Overclock gives the {@link #OVERCLOCKED} effect
 * (strength and speed) and adds to the drinker's <b>decay</b>, which fades slowly. While overclocked, or while decay
 * runs high, the player reads as corrupt to the grid: clean programs and the regime turn on them, and corrupted
 * programs leave them be.
 */
public final class Overclock {
    /** Decay added by each Overclock drunk. */
    public static final float DECAY_PER_DOSE = 0.25f;
    /** From this much decay, the player reads as corrupt even after the effect wears off. */
    public static final float MARKED_FROM = 0.5f;
    // Decay fades by this much each minute
    private static final float FADE_PER_MINUTE = 0.01f;

    /** A player's decay, 0 to 1, synced to every client so they can see it in the player's flicker. */
    public static final AttachmentType<Float> DECAY = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "player_decay"),
            builder -> builder.persistent(Codec.FLOAT).initializer(() -> 0f).syncWith(ByteBufCodecs.FLOAT, AttachmentSyncPredicate.all()));

    public static final Holder<MobEffect> OVERCLOCKED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "overclocked"),
            new OverclockedEffect()
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "overclocked_damage"),
                            4, AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "overclocked_speed"),
                            0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private Overclock() {
    }

    public static void register() {
        // Decay fades a little every minute, and an overclocked player throws off sparks of it
        ServerTickEvents.END_LEVEL_TICK.register(world -> {
            boolean minute = world.getGameTime() % 1200 == 0;
            for (Player player : world.players()) {
                if (minute && decay(player) > 0) {
                    player.setAttached(DECAY, Math.max(0, decay(player) - FADE_PER_MINUTE));
                }
                if (world.getGameTime() % 10 == 0 && marked(player)) {
                    world.sendParticles(new DustParticleOptions(0xFF8A1E, 1.0f), player.getX(), player.getY() + 1, player.getZ(),
                            3, 0.3, 0.5, 0.3, 0);
                }
            }
        });
    }

    public static float decay(Entity entity) {
        Float decay = entity.getAttached(DECAY);
        return decay == null ? 0 : decay;
    }

    /** Whether a player reads as corrupt: overclocked now, or decayed past {@link #MARKED_FROM}. */
    public static boolean marked(Entity entity) {
        return entity instanceof Player player && (player.hasEffect(OVERCLOCKED) || decay(player) >= MARKED_FROM);
    }

    /** The Overclocked effect; taking it adds decay. */
    private static final class OverclockedEffect extends MobEffect {
        private OverclockedEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xFF8A1E);
        }

        @Override
        public void onEffectAdded(LivingEntity entity, int amplifier) {
            super.onEffectAdded(entity, amplifier);
            if (entity instanceof Player player && player.level() instanceof ServerLevel) {
                player.setAttached(DECAY, Math.min(1, decay(player) + DECAY_PER_DOSE));
            }
        }
    }
}
