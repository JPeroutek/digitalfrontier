package com.partatoes.digitalfrontier.datagen.grid;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.RoadClass;
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
    private static final BlockState PAD = ModBlocks.ARRIVAL_PAD.getDefaultState();
    private static final BlockState PORTAL = ModBlocks.GRID_PORTAL.getDefaultState();
    private static final BlockState DARK_GLASS = Blocks.BLACK_STAINED_GLASS.getDefaultState();
    private static final BlockState UPLINK = ModBlocks.STRONGHOLD_UPLINK.getDefaultState();
    private static final BlockState RED_LIGHT = pattern("full", DyeColor.RED);
    private static final BlockState RED_STRIPE = pattern("center_stripe", DyeColor.RED);
    private static final BlockState RED_TARGET = pattern("thick_target", DyeColor.RED);

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
        // Arrival terminal: a lit platform under a glass canopy, with the Arrival Pad in the middle
        parts.put("grid/starter/terminal/platform", new StarterPart(16, 8)
                .plate(0, 3, BRICKS)
                .ring(0, 0, 3, LIGHT)
                .box(7, 0, 7, 7, 0, 7, PAD)
                .box(3, 1, 3, 3, 6, 3, BRICKS).box(12, 1, 3, 12, 6, 3, BRICKS)
                .box(3, 1, 12, 3, 6, 12, BRICKS).box(12, 1, 12, 12, 6, 12, BRICKS)
                .plate(7, 3, GLASS)
                .ring(7, 7, 3, LIGHT));
        // Portal: a tall column of light on a lit dais, visible from far across the outlands
        parts.put("grid/starter/portal/beam", new StarterPart(16, 48)
                .plate(0, 2, BRICKS)
                .ring(0, 0, 2, LIGHT)
                .ring(0, 0, 4, STRIPE)
                .box(6, 1, 6, 9, 47, 9, PORTAL)
                .box(5, 1, 5, 5, 2, 5, WALL).box(10, 1, 5, 10, 2, 5, WALL)
                .box(5, 1, 10, 5, 2, 10, WALL).box(10, 1, 10, 10, 2, 10, WALL));
        // Stronghold: a dark keep lit red; red isn't recolored by district, so it stays the regime's color
        parts.put("grid/starter/stronghold/base", new StarterPart(16, 8)
                .ring(0, 6, 0, BRICKS)
                .openings(0, 3, 0, 4, AIR)
                .ring(4, 4, 0, RED_LIGHT)
                .plate(7, 0, BRICKS)
                .box(6, 0, 6, 9, 0, 9, RED_TARGET)
                .box(1, 0, 8, 1, 0, 8, UPLINK));
        parts.put("grid/starter/stronghold/floor", new StarterPart(16, 6)
                .ring(0, 4, 1, BRICKS)
                .openings(1, 3, 1, 2, DARK_GLASS)
                .ring(2, 2, 1, RED_STRIPE)
                .plate(5, 1, BRICKS));
        parts.put("grid/starter/stronghold/roof", new StarterPart(16, 6)
                .ring(0, 0, 1, RED_LIGHT)
                .box(1, 1, 1, 1, 2, 1, BRICKS).box(14, 1, 1, 14, 2, 1, BRICKS)
                .box(1, 1, 14, 1, 2, 14, BRICKS).box(14, 1, 14, 14, 2, 14, BRICKS)
                .box(6, 0, 6, 9, 3, 9, BRICKS)
                .box(7, 4, 7, 8, 5, 8, RED_LIGHT));
        // One half of a boulevard: outer edge at Z = 0, the median at Z = 15 (the other half is the same, turned)
        parts.put("grid/starter/street/boulevard_half", new StarterPart(16, 4)
                .box(3, 0, 0, 3, 2, 0, WALL).box(3, 3, 0, 3, 3, 0, LIGHT)
                .box(11, 0, 0, 11, 2, 0, WALL).box(11, 3, 0, 11, 3, 0, LIGHT)
                .box(0, 0, 15, 15, 0, 15, WALL)
                .box(0, 1, 15, 0, 1, 15, LIGHT).box(5, 1, 15, 5, 1, 15, LIGHT).box(10, 1, 15, 10, 1, 15, LIGHT)
                .box(15, 1, 15, 15, 1, 15, LIGHT));
        // A lane: planters fill the strips either side of the 6-block lane (Z = 5..10), with gaps to get through
        parts.put("grid/starter/street/lane", new StarterPart(16, 4)
                .box(0, 0, 0, 2, 0, 3, BRICKS).box(5, 0, 0, 10, 0, 3, BRICKS).box(13, 0, 0, 15, 0, 3, BRICKS)
                .box(0, 0, 12, 2, 0, 15, BRICKS).box(5, 0, 12, 10, 0, 15, BRICKS).box(13, 0, 12, 15, 0, 15, BRICKS)
                .box(1, 0, 1, 1, 0, 2, LIGHT).box(7, 0, 1, 8, 0, 2, LIGHT).box(14, 0, 1, 14, 0, 2, LIGHT)
                .box(1, 0, 13, 1, 0, 14, LIGHT).box(7, 0, 13, 8, 0, 14, LIGHT).box(14, 0, 13, 14, 0, 14, LIGHT)
                .box(8, 1, 3, 8, 2, 3, WALL).box(8, 3, 3, 8, 3, 3, LIGHT)
                .box(8, 1, 12, 8, 2, 12, WALL).box(8, 3, 12, 8, 3, 12, LIGHT));
        parts.put("grid/starter/street/lights", new StarterPart(16, 4)
                .box(3, 0, 0, 3, 2, 0, WALL).box(3, 3, 0, 3, 3, 0, LIGHT)
                .box(11, 0, 0, 11, 2, 0, WALL).box(11, 3, 0, 11, 3, 0, LIGHT)
                .box(3, 0, 15, 3, 2, 15, WALL).box(3, 3, 15, 3, 3, 15, LIGHT)
                .box(11, 0, 15, 11, 2, 15, WALL).box(11, 3, 15, 11, 3, 15, LIGHT));
        return parts;
    }

    private static Map<String, GridBuildingDef> buildingDefs() {
        Map<String, GridBuildingDef> defs = new LinkedHashMap<>();
        defs.put("starter_tower", new GridBuildingDef(GridBuildingDef.Kind.BUILDING,
                List.of(Zone.CITY_CORE, Zone.CITY, Zone.OUTSKIRTS, Zone.WAYPOINT), 10, 1,
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
                List.of(Zone.CITY_CORE, Zone.CITY, Zone.WAYPOINT), 10, 1,
                Optional.of(id("grid/starter/street/lights")), List.of(), Optional.empty(), 0, 0));
        defs.put("starter_terminal", new GridBuildingDef(GridBuildingDef.Kind.TERMINAL,
                List.of(Zone.OUTSKIRTS, Zone.WAYPOINT), 10, 1,
                Optional.of(id("grid/starter/terminal/platform")), List.of(), Optional.empty(), 0, 0));
        defs.put("starter_boulevard", new GridBuildingDef(GridBuildingDef.Kind.STREET,
                List.of(Zone.CITY_CORE, Zone.CITY), 10, 1,
                Optional.of(id("grid/starter/street/boulevard_half")), List.of(), Optional.empty(), 0, 0,
                Optional.of(RoadClass.BOULEVARD)));
        defs.put("starter_lane", new GridBuildingDef(GridBuildingDef.Kind.STREET,
                List.of(Zone.CITY_CORE, Zone.CITY, Zone.WAYPOINT), 10, 1,
                Optional.of(id("grid/starter/street/lane")), List.of(), Optional.empty(), 0, 0,
                Optional.of(RoadClass.LANE)));
        defs.put("starter_stronghold",new GridBuildingDef(GridBuildingDef.Kind.STRONGHOLD,
                List.of(Zone.OUTSKIRTS, Zone.OUTLANDS, Zone.UNALLOCATED), 10, 1,
                Optional.of(id("grid/starter/stronghold/base")),
                List.of(id("grid/starter/stronghold/floor")),
                Optional.of(id("grid/starter/stronghold/roof")), 3, 5));
        defs.put("starter_portal",new GridBuildingDef(GridBuildingDef.Kind.PORTAL,
                List.of(Zone.OUTSKIRTS, Zone.OUTLANDS, Zone.UNALLOCATED), 10, 1,
                Optional.of(id("grid/starter/portal/beam")), List.of(), Optional.empty(), 0, 0));
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
        return pattern(name, DyeColor.WHITE);
    }

    private static BlockState pattern(String name, DyeColor color) {
        return ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get(name).get(color).getDefaultState();
    }

    private static Identifier id(String path) {
        return Identifier.of(DigitalFrontier.MOD_ID, path);
    }

    @Override
    public String getName() {
        return "Grid starter parts";
    }
}
