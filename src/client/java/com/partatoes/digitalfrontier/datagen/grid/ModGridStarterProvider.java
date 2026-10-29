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
import com.partatoes.digitalfrontier.grid.parts.SpiralStair;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
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
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState BRICKS = ModBlocks.GRIDSTONE_BRICKS_BLOCK.defaultBlockState();
    private static final BlockState WALL = ModBlocks.GRIDSTONE_BRICKS_WALL.defaultBlockState();
    // White blocks are recolored to the district's color at generation time
    private static final BlockState GLASS = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
    private static final BlockState LIGHT = pattern("full");
    private static final BlockState STRIPE = pattern("center_stripe");
    private static final BlockState TARGET = pattern("thick_target");
    private static final BlockState PAD = ModBlocks.ARRIVAL_PAD.defaultBlockState();
    private static final BlockState PORTAL = ModBlocks.GRID_PORTAL.defaultBlockState();
    private static final BlockState SLAB = ModBlocks.GRIDSTONE_BRICKS_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    private static final BlockState DARK_GLASS = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
    private static final BlockState UPLINK = ModBlocks.STRONGHOLD_UPLINK.defaultBlockState();
    private static final BlockState RED_LIGHT = pattern("full", DyeColor.RED);
    private static final BlockState RED_STRIPE = pattern("center_stripe", DyeColor.RED);
    private static final BlockState RED_TARGET = pattern("thick_target", DyeColor.RED);

    private final PackOutput.PathProvider structures;
    private final PackOutput.PathProvider buildings;

    public ModGridStarterProvider(FabricPackOutput output) {
        this.structures = output.createPathProvider(PackOutput.Target.DATA_PACK, "structure");
        this.buildings = output.createPathProvider(PackOutput.Target.DATA_PACK, "grid/building");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput writer) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        parts().forEach((path, part) -> futures.add(writeNbt(writer, structures.file(id(path), "nbt"), part)));
        buildingDefs().forEach((name, def) -> {
            JsonElement json = GridBuildingDef.CODEC.encodeStart(JsonOps.INSTANCE, def).getOrThrow();
            futures.add(DataProvider.saveStable(writer, json, buildings.json(id(name))));
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
        levelParts(parts);
        return parts;
    }

    /**
     * The city's levels, authored along X: the same shapes the generator builds in code when there's no template.
     * Y = 0 is Y 65 for skyroads and guideways, Y 47 (the tunnel floor) for subways, and a skyway's floor.
     */
    private static void levelParts(Map<String, StarterPart> parts) {
        // A skyroad deck half: median at Z = 15, outer edge at Z = 8, driven on at Y 80 (part Y 15)
        parts.put("grid/starter/level/skyroad", new StarterPart(16, 16)
                .box(0, 14, 8, 15, 14, 15, BRICKS)
                .box(0, 14, 8, 15, 14, 8, LIGHT).box(0, 14, 15, 15, 14, 15, LIGHT)
                .box(0, 15, 8, 15, 15, 8, WALL)
                .box(7, 0, 15, 8, 13, 15, BRICKS));
        // A junction quarter: the deck covers all but the corner away from the medians
        parts.put("grid/starter/level/skyroad_junction", new StarterPart(16, 16)
                .box(0, 14, 8, 15, 14, 15, BRICKS).box(8, 14, 0, 15, 14, 7, BRICKS)
                .box(0, 14, 15, 15, 14, 15, LIGHT).box(15, 14, 0, 15, 14, 15, LIGHT)
                .box(0, 14, 8, 7, 14, 8, LIGHT).box(8, 14, 0, 8, 14, 7, LIGHT)
                .box(0, 15, 8, 7, 15, 8, WALL).box(8, 15, 0, 8, 15, 7, WALL)
                .box(7, 0, 15, 8, 13, 15, BRICKS).box(15, 0, 7, 15, 13, 8, BRICKS));
        parts.put("grid/starter/level/skyroad_ramp_lower", ramp(false));
        parts.put("grid/starter/level/skyroad_ramp_upper", ramp(true));

        // Elevated rail: tracks along X at Z = 6 and 9, at Y 88 (part Y 23); powered every 8 blocks
        parts.put("grid/starter/level/guideway", guideway(new StarterPart(16, 26), 4, 11)
                .box(8, 0, 0, 8, 20, 0, BRICKS).box(8, 0, 15, 8, 20, 15, BRICKS)
                .box(8, 21, 0, 8, 21, 15, BRICKS));
        parts.put("grid/starter/level/guideway_crossing", guideway(new StarterPart(16, 26), 4, 11));
        StarterPart railStation = guideway(new StarterPart(16, 26), 1, 14)
                .box(0, 22, 1, 15, 22, 3, BRICKS).box(0, 22, 12, 15, 22, 14, BRICKS);
        // Placed turned clockwise, so the staircases land in the chunk's corners
        stationStairs(railStation, 0, 22, Rotation.CLOCKWISE_90);
        parts.put("grid/starter/level/guideway_station", railStation);

        // Subway: tracks along X at Z = 6 and 9, on the tunnel floor (Y 47, part Y 0)
        parts.put("grid/starter/level/subway", subway(new StarterPart(16, 8), 3, 12));
        StarterPart subwayStation = subway(new StarterPart(16, 21), 0, 15);
        stationStairs(subwayStation, 1, 16, Rotation.NONE);
        parts.put("grid/starter/level/subway_station", subwayStation);
        parts.put("grid/starter/level/subway_corner", subwayCorner());
        parts.put("grid/starter/level/subway_passage", subwayPassage());

        // A skyway across a road along X, so the bridge runs along Z; part Y 0 is its floor
        parts.put("grid/starter/level/skyway", new StarterPart(16, 5)
                .box(5, 0, 0, 10, 0, 15, BRICKS).box(6, 0, 0, 6, 0, 15, LIGHT).box(9, 0, 0, 9, 0, 15, LIGHT)
                .box(5, 1, 0, 5, 3, 15, GLASS).box(10, 1, 0, 10, 3, 15, GLASS)
                .box(6, 1, 0, 9, 3, 15, AIR)
                .box(5, 4, 0, 10, 4, 15, BRICKS));
    }

    // One chunk of an on-ramp in Z = 2..7, climbing half a block per block toward +X; the upper chunk meets the deck
    private static StarterPart ramp(boolean upper) {
        StarterPart part = new StarterPart(16, 18);
        for (int x = 0; x < 16; x++) {
            int level = upper ? Math.min(30, 16 + x) : x;
            if (level == 0) {
                continue;
            }
            boolean slab = level % 2 == 1;
            int surfaceY = slab ? (level - 1) / 2 : level / 2 - 1;
            if (surfaceY > 0) {
                part.box(x, 0, 2, x, surfaceY - 1, 7, BRICKS);
            }
            part.box(x, surfaceY, 2, x, surfaceY, 7, slab ? SLAB : BRICKS)
                    .box(x, surfaceY + 1, 3, x, surfaceY + 3, 7, AIR);
            if (level >= 2) {
                part.box(x, surfaceY + 1, 2, x, surfaceY + 1, 2, WALL);
            }
            if (upper && level >= 28) {
                // Open the deck's railing where the ramp joins it
                part.box(x, 15, 8, x, 15, 8, AIR);
            }
        }
        return part;
    }

    private static StarterPart guideway(StarterPart part, int railingFirst, int railingLast) {
        part.box(0, 22, 4, 15, 22, 11, BRICKS)
                .box(0, 22, 4, 15, 22, 4, LIGHT).box(0, 22, 11, 15, 22, 11, LIGHT)
                .box(0, 23, railingFirst + 1, 15, 25, railingLast - 1, AIR)
                .box(0, 24, 4, 15, 25, 11, AIR)
                .box(0, 23, railingFirst, 15, 23, railingFirst, WALL).box(0, 23, railingLast, 15, 23, railingLast, WALL);
        tracks(part, 22, 23);
        return part;
    }

    private static StarterPart subway(StarterPart part, int wallFirst, int wallLast) {
        part.box(0, 0, wallFirst, 15, 0, wallLast, BRICKS)
                .box(0, 1, wallFirst, 15, 6, wallFirst, BRICKS).box(0, 1, wallLast, 15, 6, wallLast, BRICKS)
                .box(0, 1, wallFirst + 1, 15, 6, wallLast - 1, AIR)
                .box(0, 7, wallFirst, 15, 7, wallLast, BRICKS);
        for (int x = 0; x < 16; x += 4) {
            part.box(x, 4, wallFirst, x, 4, wallFirst, LIGHT).box(x, 4, wallLast, x, 4, wallLast, LIGHT)
                    .box(x + 2, 7, 7, x + 2, 7, 8, LIGHT);
        }
        tracks(part, 0, 1);
        return part;
    }

    // Two tracks along X at Z = 6 and 9; powered rails on redstone at X = 0 and 8 (every 8 blocks in the world)
    private static void tracks(StarterPart part, int bedY, int railY) {
        for (int z : new int[]{6, 9}) {
            part.box(0, railY, z, 15, railY, z, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
            for (int x : new int[]{0, 8}) {
                part.box(x, bedY, z, x, bedY, z, Blocks.REDSTONE_BLOCK.defaultBlockState())
                        .box(x, railY, z, x, railY, z, Blocks.POWERED_RAIL.defaultBlockState()
                                .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST).setValue(PoweredRailBlock.POWERED, true));
            }
        }
    }

    /**
     * A station's two spiral staircases, in the corners of the chunk it's placed in: (0..3, 0..3) and the opposite
     * one. {@code placed} is how the generator turns the station, so the stairs are written turned back.
     */
    private static void stationStairs(StarterPart part, int bottomY, int rise, Rotation placed) {
        boolean turned = placed == Rotation.CLOCKWISE_90;
        Rotation back = turned ? Rotation.COUNTERCLOCKWISE_90 : Rotation.NONE;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Where this part position ends up in the chunk
                int chunkX = turned ? 15 - z : x;
                int chunkZ = turned ? x : z;
                for (int y = 0; y <= rise + 3; y++) {
                    BlockState state = null;
                    if (chunkX <= 3 && chunkZ <= 3) {
                        state = SpiralStair.blockAt(chunkX, y, chunkZ, rise, LIGHT);
                    } else if (chunkX >= 12 && chunkZ >= 12) {
                        state = SpiralStair.blockAt(15 - chunkX, y, 15 - chunkZ, rise, LIGHT);
                        state = state == null ? null : state.rotate(Rotation.CLOCKWISE_180);
                    }
                    if (state != null) {
                        part.box(x, bottomY + y, z, x, bottomY + y, z, state.rotate(back));
                    }
                }
            }
        }
    }

    // In from the south (Z = 15), out to the east (X = 15); the outer track turns at (6, 6), the inner at (9, 9)
    private static StarterPart subwayCorner() {
        StarterPart part = new StarterPart(16, 8);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                boolean inside = cornerInside(x, z);
                boolean wall = false;
                for (int dx = -1; dx <= 1 && !inside; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        wall |= cornerInside(x + dx, z + dz);
                    }
                }
                if (!inside && !wall) {
                    continue;
                }
                part.box(x, 0, z, x, 0, z, BRICKS).box(x, 7, z, x, 7, z, BRICKS)
                        .box(x, 1, z, x, 6, z, inside ? AIR : BRICKS);
                if (wall && Math.floorMod(x + z, 4) == 0) {
                    part.box(x, 4, z, x, 4, z, LIGHT);
                }
            }
        }
        part.box(7, 7, 7, 8, 7, 8, LIGHT);
        for (int track : new int[]{6, 9}) {
            part.box(track, 1, track + 1, track, 1, 15, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH))
                    .box(track + 1, 1, track, 15, 1, track, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST))
                    .box(track, 1, track, track, 1, track, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.SOUTH_EAST));
        }
        return part;
    }

    private static boolean cornerInside(int x, int z) {
        return x >= 4 && x <= 15 && z >= 4 && z <= 15 && !(x >= 12 && z >= 12);
    }

    // A maintenance room in X = 4..11, Z = 1..2, behind a doorway in the tunnel wall (Z = 3)
    private static StarterPart subwayPassage() {
        return new StarterPart(16, 6)
                .box(3, 0, 0, 12, 5, 2, BRICKS)
                .box(4, 1, 1, 11, 4, 2, AIR)
                .box(6, 5, 1, 6, 5, 2, LIGHT).box(9, 5, 1, 9, 5, 2, LIGHT)
                .box(4, 1, 1, 5, 3, 1, BRICKS).box(10, 1, 1, 11, 3, 1, BRICKS)
                .box(4, 2, 1, 5, 2, 1, LIGHT).box(10, 2, 1, 11, 2, 1, LIGHT)
                .box(7, 1, 3, 8, 2, 3, AIR);
    }

    private static Map<String, GridBuildingDef> buildingDefs() {
        Map<String, GridBuildingDef> defs = new LinkedHashMap<>();
        defs.put("starter_tower", new GridBuildingDef(GridBuildingDef.Kind.BUILDING,
                List.of(Zone.CITY_CORE, Zone.CITY, Zone.OUTSKIRTS, Zone.WAYPOINT, Zone.DERELICT), 10, 1,
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
        level(defs, "skyroad", GridBuildingDef.Kind.SKYROAD);
        level(defs, "skyroad_junction", GridBuildingDef.Kind.SKYROAD_JUNCTION);
        level(defs, "skyroad_ramp_lower", GridBuildingDef.Kind.SKYROAD_RAMP_LOWER);
        level(defs, "skyroad_ramp_upper", GridBuildingDef.Kind.SKYROAD_RAMP_UPPER);
        level(defs, "guideway", GridBuildingDef.Kind.GUIDEWAY);
        level(defs, "guideway_crossing", GridBuildingDef.Kind.GUIDEWAY_CROSSING);
        level(defs, "guideway_station", GridBuildingDef.Kind.GUIDEWAY_STATION);
        // Express lines run out beyond the city, so their tunnels are wanted everywhere
        List<Zone> everywhere = List.of(Zone.CITY_CORE, Zone.CITY, Zone.OUTSKIRTS, Zone.WAYPOINT, Zone.OUTLANDS, Zone.UNALLOCATED, Zone.DERELICT);
        level(defs, "subway", GridBuildingDef.Kind.SUBWAY, everywhere);
        level(defs, "subway_station", GridBuildingDef.Kind.SUBWAY_STATION);
        level(defs, "subway_corner", GridBuildingDef.Kind.SUBWAY_CORNER, everywhere);
        level(defs, "subway_passage", GridBuildingDef.Kind.SUBWAY_PASSAGE, everywhere);
        level(defs, "skyway", GridBuildingDef.Kind.SKYWAY);
        return defs;
    }

    // Most levels only run inside the city
    private static void level(Map<String, GridBuildingDef> defs, String name, GridBuildingDef.Kind kind) {
        level(defs, name, kind, List.of(Zone.CITY_CORE, Zone.CITY));
    }

    private static void level(Map<String, GridBuildingDef> defs, String name, GridBuildingDef.Kind kind, List<Zone> zones) {
        defs.put("starter_" + name, new GridBuildingDef(kind, zones, 10, 1,
                Optional.of(id("grid/starter/level/" + name)), List.of(), Optional.empty(), 0, 0));
    }

    private static CompletableFuture<?> writeNbt(CachedOutput writer, Path path, StarterPart part) {
        return CompletableFuture.runAsync(() -> {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                HashingOutputStream hashing = new HashingOutputStream(Hashing.sha1(), bytes);
                NbtIo.writeCompressed(part.toNbt(), hashing);
                writer.writeIfNeeded(path, bytes.toByteArray(), hashing.hash());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static BlockState pattern(String name) {
        return pattern(name, DyeColor.WHITE);
    }

    private static BlockState pattern(String name, DyeColor color) {
        return ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get(name).get(color).defaultBlockState();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, path);
    }

    @Override
    public String getName() {
        return "Grid starter parts";
    }
}
