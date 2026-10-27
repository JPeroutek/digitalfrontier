package com.partatoes.digitalfrontier.datagen.grid;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.grid.layout.Zone;
import com.partatoes.digitalfrontier.grid.parts.GridBuildingDef;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Generates a small placeholder set of grid parts and building definitions, so the grid has real buildings
 * before any are authored in-game. Everything lives under {@code grid/starter/}; hand-built parts go in
 * {@code src/main/resources} under other names (see docs/grid-structures.md).
 */
public class ModGridStarterProvider implements DataProvider {
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState BRICKS = ModBlocks.GRIDSTONE_BRICKS_BLOCK.getDefaultState();
    private static final BlockState WALL = ModBlocks.GRIDSTONE_BRICKS_WALL.getDefaultState();
    // White blocks are recolored to the district's color at generation time
    private static final BlockState GLASS = Blocks.WHITE_STAINED_GLASS.getDefaultState();
    private static final BlockState LIGHT = pattern("full");
    private static final BlockState STRIPE = pattern("center_stripe");
    private static final BlockState TARGET = pattern("thick_target");
    private final DataOutput.PathResolver structures;
    private final DataOutput.PathResolver buildings;

    public ModGridStarterProvider(FabricDataOutput output) {
        this.structures = output.getResolver(DataOutput.OutputType.DATA_PACK, "structure");
        this.buildings = output.getResolver(DataOutput.OutputType.DATA_PACK, "grid/building");
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        parts().forEach((path, part) -> futures.add(writeNbt(writer, structures.resolve(id(path), "nbt"), part)));
        buildingDefs().forEach((name, def) -> {
            JsonElement json = GridBuildingDef.CODEC.encodeStart(JsonOps.INSTANCE, def).getOrThrow();
            futures.add(DataProvider.writeToPath(writer, json, buildings.resolveJson(id(name))));
        });
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static Map<String, StarterPart> parts() {
        Map<String, StarterPart> parts = new LinkedHashMap<>();

        // 16×16 tower, walls inset one block so neighbouring towers stand apart
        parts.put("grid/starter/tower/base", new StarterPart(16, 6)
                .ring(0, 4, 1, BRICKS)
                .openings(0, 2, 1, 2, AIR)
                .plate(5, 1, BRICKS)
                .ring(5, 5, 1, LIGHT));
        parts.put("grid/starter/tower/floor_glass", new StarterPart(16, 6)
                .plate(0, 1, BRICKS)
                .ring(1, 4, 1, BRICKS)
                .openings(1, 3, 1, 10, GLASS)
                .ring(5, 5, 1, LIGHT));
        parts.put("grid/starter/tower/floor_striped", new StarterPart(16, 6)
                .plate(0, 1, BRICKS)
                .ring(1, 4, 1, BRICKS)
                .ring(2, 2, 1, STRIPE)
                .openings(1, 3, 1, 4, GLASS)
                .ring(5, 5, 1, LIGHT));
        parts.put("grid/starter/tower/roof", new StarterPart(16, 5)
                .plate(0, 1, BRICKS)
                .ring(1, 1, 1, LIGHT)
                .box(7, 1, 7, 8, 3, 8, BRICKS)
                .box(7, 4, 7, 8, 4, 8, LIGHT));

        // 32×32 block tower, also used for the landmark at the city center
        parts.put("grid/starter/block_tower/base", new StarterPart(32, 6)
                .ring(0, 4, 2, BRICKS)
                .openings(0, 3, 2, 4, AIR)
                .plate(5, 2, BRICKS)
                .ring(5, 5, 2, LIGHT));
        parts.put("grid/starter/block_tower/floor", new StarterPart(32, 6)
                .plate(0, 2, BRICKS)
                .ring(1, 4, 2, BRICKS)
                .openings(1, 3, 2, 22, GLASS)
                .openings(1, 4, 2, 2, LIGHT)
                .ring(5, 5, 2, LIGHT));
        parts.put("grid/starter/block_tower/roof", new StarterPart(32, 8)
                .plate(0, 2, BRICKS)
                .ring(1, 1, 2, LIGHT)
                .box(10, 1, 10, 21, 4, 21, BRICKS)
                .ring(4, 4, 10, LIGHT)
                .box(15, 5, 15, 16, 7, 16, LIGHT));

        // Open spaces and street furniture (streets are authored running along X)
        parts.put("grid/starter/plaza/square", new StarterPart(16, 3)
                .box(3, 0, 3, 3, 1, 3, WALL).box(3, 2, 3, 3, 2, 3, LIGHT)
                .box(12, 0, 3, 12, 1, 3, WALL).box(12, 2, 3, 12, 2, 3, LIGHT)
                .box(3, 0, 12, 3, 1, 12, WALL).box(3, 2, 12, 3, 2, 12, LIGHT)
                .box(12, 0, 12, 12, 1, 12, WALL).box(12, 2, 12, 12, 2, 12, LIGHT)
                .box(7, 0, 7, 8, 0, 8, TARGET));
        parts.put("grid/starter/street/lights",new StarterPart(16, 4)
                .box(3, 0, 0, 3, 2, 0, WALL).box(3, 3, 0, 3, 3, 0, LIGHT)
                .box(11, 0, 0, 11, 2, 0, WALL).box(11, 3, 0, 11, 3, 0, LIGHT)
                .box(3, 0, 15, 3, 2, 15, WALL).box(3, 3, 15, 3, 3, 15, LIGHT)
                .box(11, 0, 15, 11, 2, 15, WALL).box(11, 3, 15, 11, 3, 15, LIGHT));
        return parts;
    }

    private static Map<String, GridBuildingDef> buildingDefs() {
        Map<String, GridBuildingDef> defs = new LinkedHashMap<>();
        defs.put("starter_tower", new GridBuildingDef(GridBuildingDef.Kind.BUILDING,
                List.of(Zone.CITY_CORE, Zone.CITY, Zone.OUTSKIRTS), 10, 1,
                Optional.of(id("grid/starter/tower/base")),
                List.of(id("grid/starter/tower/floor_glass"), id("grid/starter/tower/floor_striped")),
                Optional.of(id("grid/starter/tower/roof")), 1, 24));
        defs.put("starter_block_tower", new GridBuildingDef(GridBuildingDef.Kind.BUILDING,
                List.of(Zone.CITY_CORE, Zone.CITY), 10, 2,
                Optional.of(id("grid/starter/block_tower/base")),
                List.of(id("grid/starter/block_tower/floor")),
                Optional.of(id("grid/starter/block_tower/roof")), 4, 24));
        defs.put("starter_core_tower", new GridBuildingDef(GridBuildingDef.Kind.LANDMARK,
                List.of(Zone.CITY_CORE), 10, 2,
                Optional.of(id("grid/starter/block_tower/base")),
                List.of(id("grid/starter/block_tower/floor")),
                Optional.of(id("grid/starter/block_tower/roof")), 32, 40));
        defs.put("starter_plaza", new GridBuildingDef(GridBuildingDef.Kind.PLAZA,
                List.of(Zone.CITY, Zone.WAYPOINT), 10, 1,
                Optional.of(id("grid/starter/plaza/square")), List.of(), Optional.empty(), 0, 0));
        defs.put("starter_street_lights", new GridBuildingDef(GridBuildingDef.Kind.STREET,
                List.of(Zone.CITY_CORE, Zone.CITY), 10, 1,
                Optional.of(id("grid/starter/street/lights")), List.of(), Optional.empty(), 0, 0));
        return defs;
    }

    private static CompletableFuture<?> writeNbt(DataWriter writer, Path path, StarterPart part) {
        return CompletableFuture.runAsync(() -> {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                HashingOutputStream hashing = new HashingOutputStream(Hashing.sha1(), bytes);
                NbtIo.writeCompressed(part.toNbt(), hashing);
                writer.write(path, bytes.toByteArray(), hashing.hash());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static BlockState pattern(String name) {
        return ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get(name).get(DyeColor.WHITE).getDefaultState();
    }

    private static Identifier id(String path) {
        return Identifier.of(DigitalFrontier.MOD_ID, path);
    }

    @Override
    public String getName() {
        return "Grid starter parts";
    }
}
