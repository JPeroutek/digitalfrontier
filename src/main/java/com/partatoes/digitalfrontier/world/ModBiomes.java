package com.partatoes.digitalfrontier.world;

import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ModBiomes {
    public static final ResourceKey<Biome> GRID_CITY_CORE = registerKey("grid_city_core");
    public static final ResourceKey<Biome> GRID_CITY = registerKey("grid_city");
    public static final ResourceKey<Biome> OUTSKIRTS = registerKey("outskirts");
    public static final ResourceKey<Biome> OUTLANDS = registerKey("outlands");
    public static final ResourceKey<Biome> UNALLOCATED_SPACE = registerKey("unallocated_space");
    // Where corruption is strong (the CORRUPTED tier and up): grayed, reddened fog and drifting static
    public static final ResourceKey<Biome> CORRUPTED_CITY = registerKey("corrupted_city");
    public static final ResourceKey<Biome> CORRUPTED_OUTLANDS = registerKey("corrupted_outlands");
    public static final ResourceKey<Biome> CORRUPTED_UNALLOCATED = registerKey("corrupted_unallocated");

    public static void bootstrap(BootstrapContext<Biome> context) {
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
        Biome.BiomeBuilder biome = new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5f)
                .downfall(0.5f)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(fogColor))
                .setAttribute(EnvironmentAttributes.SKY_COLOR, ARGB.vector3fFromRGB24(0x000000))
                // The Sea of Simulation: dark, and darker under the surface
                .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, ARGB.vector3fFromRGB24(0x02090C))
                .setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS)
                .specialEffects(new BiomeSpecialEffects.Builder().waterColor(0x103842).build());
        if (particles != null) {
            biome.setAttribute(EnvironmentAttributes.AMBIENT_PARTICLES, particles);
        }
        return biome
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(new BiomeGenerationSettings.PlainBuilder().build())
                .build();
    }

    public static ResourceKey<Biome> registerKey(String name) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, name));
    }
}
