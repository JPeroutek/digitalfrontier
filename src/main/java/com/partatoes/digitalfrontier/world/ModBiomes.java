package com.partatoes.digitalfrontier.world;

import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BiomeMoodSound;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.BiomeParticleConfig;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;

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
        context.register(CORRUPTED_CITY, gridBiome(0x2A1C1E, new BiomeParticleConfig(ParticleTypes.WHITE_ASH, 0.02f)));
        context.register(CORRUPTED_OUTLANDS, gridBiome(0x2E1A14, new BiomeParticleConfig(ParticleTypes.WHITE_ASH, 0.03f)));
        context.register(CORRUPTED_UNALLOCATED, gridBiome(0x6A5060, new BiomeParticleConfig(ParticleTypes.ASH, 0.04f)));
    }

    private static Biome gridBiome(int fogColor, @Nullable BiomeParticleConfig particles) {
        BiomeEffects.Builder effects = new BiomeEffects.Builder()
                .fogColor(fogColor)
                .skyColor(0x000000)
                .waterColor(0x3F76E4)
                .waterFogColor(0x050533)
                .moodSound(BiomeMoodSound.CAVE);
        if (particles != null) {
            effects.particleConfig(particles);
        }
        return new Biome.Builder()
                .precipitation(false)
                .temperature(0.5f)
                .downfall(0.5f)
                .effects(effects.build())
                .spawnSettings(new SpawnSettings.Builder().build())
                .generationSettings(new GenerationSettings.Builder().build())
                .build();
    }

    public static RegistryKey<Biome> registerKey(String name) {
        return RegistryKey.of(RegistryKeys.BIOME, Identifier.of(DigitalFrontier.MOD_ID, name));
    }
}
