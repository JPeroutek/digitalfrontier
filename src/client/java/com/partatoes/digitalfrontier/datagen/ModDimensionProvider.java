package com.partatoes.digitalfrontier.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.world.ModBiomes;
import com.partatoes.digitalfrontier.world.ModDimensions;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorLayer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

// The dimension registry isn't one of the dynamic registries FabricDynamicRegistryProvider can write,
// so encode the DimensionOptions ourselves against the datagen registries.
public class ModDimensionProvider implements DataProvider {
    private final DataOutput.PathResolver pathResolver;
    private final CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture;

    public ModDimensionProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        this.pathResolver = output.getResolver(RegistryKeys.DIMENSION);
        this.registriesFuture = registriesFuture;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        return registriesFuture.thenCompose(registries -> {
            // Entries must be looked up through the same ops that encode them, or the codec rejects them as foreign
            RegistryOps<JsonElement> ops = registries.getOps(JsonOps.INSTANCE);
            RegistryEntry<Biome> biome = ops.getEntryLookup(RegistryKeys.BIOME).orElseThrow().getOrThrow(ModBiomes.UNALLOCATED_SPACE);
            FlatChunkGeneratorConfig flatConfig = new FlatChunkGeneratorConfig(Optional.empty(), biome, List.of())
                    .with(List.of(new FlatChunkGeneratorLayer(129, ModBlocks.GRIDSTONE_BLOCK)), Optional.empty(), biome);

            DimensionOptions dimension = new DimensionOptions(
                    ops.getEntryLookup(RegistryKeys.DIMENSION_TYPE).orElseThrow().getOrThrow(ModDimensions.DIGITAL_FRONTIER_TYPE_KEY),
                    new FlatChunkGenerator(flatConfig));

            JsonElement json = DimensionOptions.CODEC.encodeStart(ops, dimension).getOrThrow();
            return DataProvider.writeToPath(writer, json, pathResolver.resolveJson(ModDimensions.DIGITAL_FRONTIER_ID));
        });
    }

    @Override
    public String getName() {
        return "Dimensions";
    }
}
