package com.partatoes.digitalfrontier.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.grid.gen.GridBiomeSource;
import com.partatoes.digitalfrontier.grid.gen.GridBiomes;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.layout.GridLayoutSettings;
import com.partatoes.digitalfrontier.world.ModBiomes;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import java.util.concurrent.CompletableFuture;

// The dimension registry isn't one of the dynamic registries FabricDynamicRegistryProvider can write,
// so encode the DimensionOptions ourselves against the datagen registries.
public class ModDimensionProvider implements DataProvider {
    private final PackOutput.PathProvider pathResolver;
    private final CompletableFuture<HolderLookup.Provider> registriesFuture;

    public ModDimensionProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        this.pathResolver = output.createRegistryElementsPathProvider(Registries.LEVEL_STEM);
        this.registriesFuture = registriesFuture;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput writer) {
        return registriesFuture.thenCompose(registries -> {
            // Entries must be looked up through the same ops that encode them, or the codec rejects them as foreign
            RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
            HolderGetter<Biome> biomes = ops.getter(Registries.BIOME).orElseThrow();
            GridBiomes gridBiomes = new GridBiomes(
                    biomes.getOrThrow(ModBiomes.GRID_CITY_CORE),
                    biomes.getOrThrow(ModBiomes.GRID_CITY),
                    biomes.getOrThrow(ModBiomes.OUTSKIRTS),
                    biomes.getOrThrow(ModBiomes.OUTLANDS),
                    biomes.getOrThrow(ModBiomes.UNALLOCATED_SPACE),
                    biomes.getOrThrow(ModBiomes.CORRUPTED_CITY),
                    biomes.getOrThrow(ModBiomes.CORRUPTED_OUTLANDS),
                    biomes.getOrThrow(ModBiomes.CORRUPTED_UNALLOCATED));

            LevelStem dimension = new LevelStem(
                    ops.getter(Registries.DIMENSION_TYPE).orElseThrow().getOrThrow(ModDimensions.DIGITAL_FRONTIER_TYPE_KEY),
                    new GridChunkGenerator(new GridBiomeSource(GridLayoutSettings.DEFAULT, gridBiomes)));

            JsonElement json = LevelStem.CODEC.encodeStart(ops, dimension).getOrThrow();
            return DataProvider.saveStable(writer, json, pathResolver.json(ModDimensions.DEFAULT_GRID_ID));
        });
    }

    @Override
    public String getName() {
        return "Dimensions";
    }
}
