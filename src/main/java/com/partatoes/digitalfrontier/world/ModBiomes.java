package com.partatoes.digitalfrontier.world;

import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BiomeMoodSound;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;

public class ModBiomes {
    public static final RegistryKey<Biome> UNALLOCATED_SPACE = registerKey("unallocated_space");

    public static void bootstrap(Registerable<Biome> context) {
        context.register(UNALLOCATED_SPACE, new Biome.Builder()
                .precipitation(false)
                .temperature(0.5f)
                .downfall(0.5f)
                .effects(new BiomeEffects.Builder()
                        .fogColor(0xA080A0)
                        .skyColor(0x000000)
                        .waterColor(0x3F76E4)
                        .waterFogColor(0x050533)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(new SpawnSettings.Builder().build())
                .generationSettings(new GenerationSettings.Builder().build())
                .build());
    }

    public static RegistryKey<Biome> registerKey(String name) {
        return RegistryKey.of(RegistryKeys.BIOME, Identifier.of(DigitalFrontier.MOD_ID, name));
    }
}
