package com.partatoes.digitalfrontier.grid.stronghold;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

import java.util.Optional;

/**
 * A stronghold lieutenant, loaded from {@code data/<namespace>/grid/lieutenant/<name>.json}. Everything that makes
 * it a character is data: its name (any text component, so a datapack can use lore names), its skin, and when it
 * comes back after being defeated. Its fighting style is one of the built-in {@link Archetype}s.
 *
 * @param minGridCorruption only strongholds of grids tuned at least this corrupt (0 to 1) can have this lieutenant
 */
public record LieutenantDef(
        Text name,
        Archetype archetype,
        Optional<Identifier> texture,
        Optional<Double> health,
        int weight,
        float minGridCorruption,
        float maxGridCorruption,
        Respawn respawn) {

    public static final Codec<LieutenantDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TextCodecs.CODEC.fieldOf("name").forGetter(LieutenantDef::name),
            Archetype.CODEC.fieldOf("archetype").forGetter(LieutenantDef::archetype),
            Identifier.CODEC.optionalFieldOf("texture").forGetter(LieutenantDef::texture),
            Codec.doubleRange(1, 4096).optionalFieldOf("health").forGetter(LieutenantDef::health),
            Codec.intRange(1, 1000).optionalFieldOf("weight", 10).forGetter(LieutenantDef::weight),
            Codec.floatRange(0, 1).optionalFieldOf("min_grid_corruption", 0f).forGetter(LieutenantDef::minGridCorruption),
            Codec.floatRange(0, 1).optionalFieldOf("max_grid_corruption", 1f).forGetter(LieutenantDef::maxGridCorruption),
            Respawn.CODEC.fieldOf("respawn").forGetter(LieutenantDef::respawn)
    ).apply(instance, LieutenantDef::new));

    public Identifier textureOrDefault() {
        return texture.orElse(archetype.defaultTexture());
    }

    public double healthOrDefault() {
        return health.orElse(archetype.defaultHealth());
    }

    /**
     * @param minDelay    ticks after the defeat before any trigger can bring the lieutenant back
     * @param maxRespawns how many times it can come back; -1 for no limit
     */
    public record Respawn(long minDelay, int maxRespawns, Trigger trigger) {
        public static final Codec<Respawn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.optionalFieldOf("min_delay", 0L).forGetter(Respawn::minDelay),
                Codec.intRange(-1, Integer.MAX_VALUE).optionalFieldOf("max_respawns", -1).forGetter(Respawn::maxRespawns),
                Trigger.CODEC.fieldOf("trigger").forGetter(Respawn::trigger)
        ).apply(instance, Respawn::new));
    }

    /** How a lieutenant fights. Behavior lives in code; each definition picks one. */
    public enum Archetype implements StringIdentifiable {
        /** Commands the regime: keeps its distance, calls in enforcers, hits hard when cornered. */
        RULER("ruler", 300, 0.26, "clu64"),
        /** The silent enforcer: fast, relentless, closes distance in sudden dashes. */
        DUELIST("duelist", 220, 0.36, "rinzler64");

        public static final Codec<Archetype> CODEC = StringIdentifiable.createCodec(Archetype::values);

        private final String name;
        private final double defaultHealth;
        private final double speed;
        private final String skin;

        Archetype(String name, double defaultHealth, double speed, String skin) {
            this.name = name;
            this.defaultHealth = defaultHealth;
            this.speed = speed;
            this.skin = skin;
        }

        public double defaultHealth() {
            return defaultHealth;
        }

        public double speed() {
            return speed;
        }

        public Identifier defaultTexture() {
            return Identifier.of(DigitalFrontier.MOD_ID, "textures/entity/" + skin + ".png");
        }

        @Override
        public String asString() {
            return name;
        }
    }
}
