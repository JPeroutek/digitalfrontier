package com.partatoes.digitalfrontier.world;

import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;

import java.util.List;

public class ModBiomes {
    public static final RegistryKey<Biome> GRID_CITY_CORE = registerKey("grid_city_core");
    public static final RegistryKey<Biome> GRID_CITY = registerKey("grid_city");
    public static final RegistryKey<Biome> OUTSKIRTS = registerKey("outskirts");
    public static final RegistryKey<Biome> OUTLANDS = registerKey("outlands");
    public static final RegistryKey<Biome> UNALLOCATED_SPACE = registerKey("unallocated_space");
    // Where corruption is strong (the CORRUPTED tier and up): grayed, reddened fog and drifting static
    public static final RegistryKey<Biome> CORRUPTED_CITY = registerKey("corrupted_city");
    public static final RegistryKey<Biome> CORRUPTED_OUTLANDS = registerKey("corrupted_outlands");
    public static final RegistryKey<Biome> CORRUPTED_UNALLOCATED = registerKey("corrupted_unallocated");

    public static void bootstrap(Registerable<Biome> context) {
        context.register(GRID_CITY_CORE, gridBiome(0x0B2A3D, null));
        context.register(GRID_CITY, gridBiome(0x0A1F2E, null));
        context.register(OUTSKIRTS, gridBiome(0x111A22, null));
        context.register(OUTLANDS, gridBiome(0x1C1510, null));
        context.register(UNALLOCATED_SPACE, gridBiome(0xA080A0, null));
        context.register(CORRUPTED_CITY, gridBiome(0x2A1C1E, AmbientParticle.of(ParticleTypes.WHITE_ASH, 0.02f)));
        context.register(CORRUPTED_OUTLANDS, gridBiome(0x2E1A14, AmbientParticle.of(ParticleTypes.WHITE_ASH, 0.03f)));
        context.register(CORRUPTED_UNALLOCATED, gridBiome(0x6A5060, AmbientParticle.of(ParticleTypes.ASH, 0.04f)));
    }

    private static Biome gridBiome(int fogColor, @Nullable List<AmbientParticle> particles) {
        Biome.Builder biome = new Biome.Builder()
                .precipitation(false)
                .temperature(0.5f)
                .downfall(0.5f)
                .setEnvironmentAttribute(EnvironmentAttributes.FOG_COLOR_VISUAL, fogColor)
                .setEnvironmentAttribute(EnvironmentAttributes.SKY_COLOR_VISUAL, 0x000000)
                // The Sea of Simulation: dark, and darker under the surface
                .setEnvironmentAttribute(EnvironmentAttributes.WATER_FOG_COLOR_VISUAL, 0x02090C)
                .setEnvironmentAttribute(EnvironmentAttributes.AMBIENT_SOUNDS_AUDIO, AmbientSounds.CAVE)
                .effects(new BiomeEffects.Builder().waterColor(0x103842).build());
        if (particles != null) {
            biome.setEnvironmentAttribute(EnvironmentAttributes.AMBIENT_PARTICLES_VISUAL, particles);
        }
        return biome
                .spawnSettings(new SpawnSettings.Builder().build())
                .generationSettings(new GenerationSettings.Builder().build())
                .build();
    }

    public static RegistryKey<Biome> registerKey(String name) {
        return RegistryKey.of(RegistryKeys.BIOME, Identifier.of(DigitalFrontier.MOD_ID, name));
    }
}
