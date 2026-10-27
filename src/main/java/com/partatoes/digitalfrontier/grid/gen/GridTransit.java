package com.partatoes.digitalfrontier.grid.gen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.grid.layout.ChunkPlan;
import com.partatoes.digitalfrontier.grid.layout.GridLevel;
import com.partatoes.digitalfrontier.grid.layout.PlanType;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.RoadClass;
import com.partatoes.digitalfrontier.grid.layout.TransitLayout;
import com.partatoes.digitalfrontier.grid.layout.TransitLayout.Skyway;
import com.partatoes.digitalfrontier.grid.layout.GridRandom;
import com.partatoes.digitalfrontier.grid.parts.DistrictPalette;
import com.partatoes.digitalfrontier.grid.parts.GridAssets;
import com.partatoes.digitalfrontier.grid.parts.GridBuildingDef.Kind;
import com.partatoes.digitalfrontier.grid.parts.GridPart;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.RailBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.partatoes.digitalfrontier.grid.layout.GridLayout.FLOOR_Y;

/**
 * Placeholder geometry for the city's levels ({@link TransitLayout}): skyroad decks and ramps, elevated rail
 * guideways, subway tunnels and stations, and skyway bridges with the doorways they lead into. Built in code, like
 * {@link GridTerrain}'s placeholder towers, so the layout can be walked before any templates exist.
 *
 * <p>{@link #blockAt} returns the block a level puts at a position, or null where the levels leave the chunk as it
 * is; the levels win wherever they return something.
 */
final class GridTransit {
    /** The skyroad deck's blocks; you drive on top of them, at Y 80. */
    static final int DECK_Y = FLOOR_Y + 15;
    static final int GUIDEWAY_Y = 87;
    static final int SUBWAY_FLOOR_Y = 47;
    private static final int SUBWAY_CEILING_Y = 54;

    private static final BlockState BRICKS = ModBlocks.GRIDSTONE_BRICKS_BLOCK.getDefaultState();
    private static final BlockState WALL = ModBlocks.GRIDSTONE_BRICKS_WALL.getDefaultState();
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState SLAB = ModBlocks.GRIDSTONE_BRICKS_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM);

    private GridTransit() {
    }

    /** A doorway in a building chunk's outer wall where a skyway arrives. Sides: 0 north, 1 south, 2 west, 3 east. */
    record Door(int side, int standY) {
    }

    /** A level template placed in this chunk: its part, where its Y = 0 goes, and how it's turned and flipped. */
    record Placement(GridPart part, int bottomY, BlockRotation rotation, BlockMirror mirror) {
        int topY() {
            return bottomY + part.sizeY() - 1;
        }
    }

    /**
     * What the levels put in one chunk, worked out once per chunk. Pieces with a template are in
     * {@code placements}; the {@code ...Templated} flags say which pieces those replace in the code-built geometry.
     */
    record ChunkTransit(TransitLayout layout, ChunkPlan plan, int chunkX, int chunkZ, boolean elevatedRail, boolean subway,
                       boolean railStation, boolean subwayStation, @Nullable Skyway skyway, List<Door> doors, boolean deckChunk,
                       List<Placement> placements, boolean deckTemplated, boolean rampTemplated, boolean railTemplated,
                       boolean subwayTemplated, boolean skywayTemplated) {
        boolean any() {
            return elevatedRail || subway || skyway != null || !doors.isEmpty() || deckChunk;
        }

        /** The highest Y any level reaches in this chunk. */
        int maxY() {
            int max = FLOOR_Y;
            if (deckChunk) {
                max = DECK_Y + 3;
            }
            if (elevatedRail) {
                max = Math.max(max, GUIDEWAY_Y + 3);
            }
            if (skyway != null) {
                max = Math.max(max, skyway.standY() + 3);
            }
            for (Door door : doors) {
                max = Math.max(max, door.standY() + 2);
            }
            for (Placement placement : placements) {
                max = Math.max(max, placement.topY());
            }
            return max;
        }

        DyeColor district() {
            return plan.district();
        }
    }

    /** Checks whether a skyway really has towers to connect: the chunk-building code supplies this. */
    interface SkywayCheck {
        boolean fits(Skyway skyway, int roadChunkX, int roadChunkZ);
    }

    static ChunkTransit forChunk(TransitLayout layout, GridAssets assets, long seed, ChunkPlan plan, int chunkX, int chunkZ,
                                 SkywayCheck check) {
        boolean road = plan.type() == PlanType.STREET;
        Skyway skyway = null;
        List<Door> doors = new ArrayList<>();
        if (road) {
            Skyway candidate = layout.skywayAt(chunkX, chunkZ);
            if (candidate != null && check.fits(candidate, chunkX, chunkZ)) {
                skyway = candidate;
            }
        } else if (plan.type() == PlanType.BUILDING) {
            // A skyway in a neighbouring road chunk that leads to this building
            addDoor(doors, layout, check, chunkX, chunkZ - 1, true, 0);
            addDoor(doors, layout, check, chunkX, chunkZ + 1, true, 1);
            addDoor(doors, layout, check, chunkX - 1, chunkZ, false, 2);
            addDoor(doors, layout, check, chunkX + 1, chunkZ, false, 3);
        }
        boolean elevatedRail = road && layout.has(GridLevel.ELEVATED_RAIL)
                && layout.elevatedRail(chunkX, chunkZ);
        boolean subway = road && layout.has(GridLevel.SUBWAY) && layout.subway(chunkX, chunkZ);
        boolean deckChunk = road && layout.has(GridLevel.SKYROADS)
                && (isBoulevard(plan.roadX()) || isBoulevard(plan.roadZ()));
        boolean railStation = elevatedRail && layout.railStation(chunkX, chunkZ);
        boolean subwayStation = subway && layout.subwayStation(chunkX, chunkZ);

        // Templates, where the loaded assets have one for a piece; earlier placements win where they overlap
        Templates templates = new Templates(assets, seed, plan, chunkX, chunkZ);
        boolean rampTemplated = false;
        TransitLayout.RampPiece ramp = deckChunk ? layout.rampPiece(chunkX, chunkZ) : null;
        if (ramp != null) {
            rampTemplated = templates.add(ramp.upper() ? Kind.SKYROAD_RAMP_UPPER : Kind.SKYROAD_RAMP_LOWER,
                    Kind.SKYROAD_RAMP_LOWER.baseY(), rampRotation(ramp), rampMirror(ramp));
        }
        boolean skywayTemplated = skyway != null && templates.add(Kind.SKYWAY, skyway.standY() - 1,
                skyway.roadAlongX() ? BlockRotation.NONE : BlockRotation.CLOCKWISE_90, BlockMirror.NONE);
        // Elevated rail only runs along Z
        boolean intersection = plan.roadX() != null && plan.roadZ() != null;
        boolean railTemplated = elevatedRail && templates.add(
                railStation ? Kind.GUIDEWAY_STATION : intersection ? Kind.GUIDEWAY_CROSSING : Kind.GUIDEWAY,
                Kind.GUIDEWAY.baseY(), BlockRotation.CLOCKWISE_90, BlockMirror.NONE);
        // Subways only run along X
        boolean subwayTemplated = subway && templates.add(subwayStation ? Kind.SUBWAY_STATION : Kind.SUBWAY,
                Kind.SUBWAY.baseY(), BlockRotation.NONE, BlockMirror.NONE);
        TransitLayout.DeckPiece deck = deckChunk ? layout.deckPiece(chunkX, chunkZ) : null;
        boolean deckTemplated = deck != null && (deck.junction()
                ? templates.add(Kind.SKYROAD_JUNCTION, Kind.SKYROAD_JUNCTION.baseY(), junctionRotation(deck), BlockMirror.NONE)
                : templates.add(Kind.SKYROAD, Kind.SKYROAD.baseY(), deckRotation(deck), BlockMirror.NONE));

        return new ChunkTransit(layout, plan, chunkX, chunkZ, elevatedRail, subway, railStation, subwayStation,
                skyway, List.copyOf(doors), deckChunk, List.copyOf(templates.placements), deckTemplated, rampTemplated,
                railTemplated, subwayTemplated, skywayTemplated);
    }

    // Picks a template for each piece, the same way buildings are picked
    private static final class Templates {
        private static final int SALT = 7000;

        private final GridAssets assets;
        private final long seed;
        private final ChunkPlan plan;
        private final int chunkX;
        private final int chunkZ;
        private final List<Placement> placements = new ArrayList<>();

        Templates(GridAssets assets, long seed, ChunkPlan plan, int chunkX, int chunkZ) {
            this.assets = assets;
            this.seed = seed;
            this.plan = plan;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        boolean add(Kind kind, int bottomY, BlockRotation rotation, BlockMirror mirror) {
            double roll = GridRandom.unit(GridRandom.hash(seed, chunkX, chunkZ, SALT + kind.ordinal()));
            GridAssets.Candidate candidate = assets.pick(kind, 1, plan.zone(), roll);
            GridPart part = candidate == null ? null : candidate.def().base().map(assets::part).orElse(null);
            if (part == null) {
                return false;
            }
            placements.add(new Placement(part, bottomY, rotation, mirror));
            return true;
        }
    }

    // Deck templates are authored for a boulevard half along X: median at Z = 15, like boulevard street furniture
    private static BlockRotation deckRotation(TransitLayout.DeckPiece deck) {
        if (deck.halfX() >= 0) {
            return deck.halfX() == 0 ? BlockRotation.NONE : BlockRotation.CLOCKWISE_180;
        }
        return deck.halfZ() == 0 ? BlockRotation.COUNTERCLOCKWISE_90 : BlockRotation.CLOCKWISE_90;
    }

    // Junction quarters are authored with the medians meeting at X = 15, Z = 15 (half 0 of both boulevards)
    private static BlockRotation junctionRotation(TransitLayout.DeckPiece deck) {
        if (deck.halfX() == 0) {
            return deck.halfZ() == 0 ? BlockRotation.NONE : BlockRotation.CLOCKWISE_90;
        }
        return deck.halfZ() == 0 ? BlockRotation.COUNTERCLOCKWISE_90 : BlockRotation.CLOCKWISE_180;
    }

    // Ramps are authored in half 0's lane of a boulevard along X, climbing toward +X; flips cover the other ways
    private static BlockRotation rampRotation(TransitLayout.RampPiece ramp) {
        if (ramp.alongX()) {
            return ramp.half() == 1 && !ramp.risingPositive() ? BlockRotation.CLOCKWISE_180 : BlockRotation.NONE;
        }
        return ramp.half() == 0 ? BlockRotation.COUNTERCLOCKWISE_90 : BlockRotation.CLOCKWISE_90;
    }

    private static BlockMirror rampMirror(TransitLayout.RampPiece ramp) {
        if (ramp.alongX()) {
            if (ramp.half() == 0) {
                return ramp.risingPositive() ? BlockMirror.NONE : BlockMirror.FRONT_BACK;
            }
            return ramp.risingPositive() ? BlockMirror.LEFT_RIGHT : BlockMirror.NONE;
        }
        // Turned onto Z, the climb runs toward -Z for half 0 and +Z for half 1 unless flipped
        boolean flip = ramp.half() == 0 ? ramp.risingPositive() : !ramp.risingPositive();
        return flip ? BlockMirror.FRONT_BACK : BlockMirror.NONE;
    }

    @Nullable
    private static BlockState placed(ChunkTransit transit, int localX, int y, int localZ) {
        for (Placement placement : transit.placements()) {
            if (y < placement.bottomY() || y > placement.topY()) {
                continue;
            }
            // Undo the rotation, then the mirror, to find the template position
            int last = 15;
            int mx;
            int mz;
            switch (placement.rotation()) {
                case CLOCKWISE_90 -> { mx = localZ; mz = last - localX; }
                case CLOCKWISE_180 -> { mx = last - localX; mz = last - localZ; }
                case COUNTERCLOCKWISE_90 -> { mx = last - localZ; mz = localX; }
                default -> { mx = localX; mz = localZ; }
            }
            int px = placement.mirror() == BlockMirror.FRONT_BACK ? last - mx : mx;
            int pz = placement.mirror() == BlockMirror.LEFT_RIGHT ? last - mz : mz;
            BlockState state = placement.part().get(px, y - placement.bottomY(), pz);
            if (state != null) {
                return DistrictPalette.remap(state, transit.district()).mirror(placement.mirror()).rotate(placement.rotation());
            }
        }
        return null;
    }

    private static boolean isBoulevard(@Nullable StreetAxis.Road road) {
        return road != null && road.roadClass() == RoadClass.BOULEVARD;
    }

    private static void addDoor(List<Door> doors, TransitLayout layout, SkywayCheck check, int roadX, int roadZ, boolean alongX, int side) {
        Skyway skyway = layout.skywayAt(roadX, roadZ);
        if (skyway != null && skyway.roadAlongX() == alongX && check.fits(skyway, roadX, roadZ)) {
            doors.add(new Door(side, skyway.standY()));
        }
    }

    @Nullable
    static BlockState blockAt(ChunkTransit transit, int localX, int y, int localZ) {
        BlockState state = transit.placements().isEmpty() ? null : placed(transit, localX, y, localZ);
        if (state == null && transit.skyway() != null && !transit.skywayTemplated()) {
            state = skyway(transit, transit.skyway(), localX, y, localZ);
        }
        if (state == null && transit.elevatedRail() && !transit.railTemplated()) {
            state = elevatedRail(transit, localX, y, localZ);
        }
        if (state == null && transit.deckChunk() && !(transit.deckTemplated() && transit.rampTemplated())) {
            state = skyroad(transit, localX, y, localZ);
        }
        if (state == null && transit.subway() && !transit.subwayTemplated()) {
            state = subway(transit, localX, y, localZ);
        }
        if (state == null && !transit.doors().isEmpty()) {
            state = door(transit, localX, y, localZ);
        }
        return state;
    }

    // ---- Skyroads ----

    @Nullable
    private static BlockState skyroad(ChunkTransit transit, int localX, int y, int localZ) {
        TransitLayout layout = transit.layout();
        int blockX = transit.chunkX() * 16 + localX;
        int blockZ = transit.chunkZ() * 16 + localZ;
        // Only the parts without a template are built here
        int deck = transit.deckTemplated() ? -1 : layout.deckLevel(blockX, blockZ);
        int ramp = transit.rampTemplated() ? -1 : layout.rampLevel(blockX, blockZ);
        int level = Math.max(deck, ramp);
        if (level < 0) {
            return null;
        }
        // Even levels are full blocks, odd ones bottom slabs half a block higher: half a block per step
        boolean slab = level % 2 == 1;
        int blockY = slab ? FLOOR_Y + 1 + level / 2 : FLOOR_Y + level / 2;
        boolean centerRow = layout.isDeckCenterRow(blockZ);
        boolean centerColumn = layout.isDeckCenterColumn(blockX);

        if (y == blockY) {
            if (slab) {
                return SLAB;
            }
            boolean lit = deck == TransitLayout.DECK_LEVEL && (centerRow || centerColumn || isDeckEdge(layout, blockX, blockZ));
            return lit ? pattern(transit.district()) : BRICKS;
        }
        if (y < blockY && y > FLOOR_Y) {
            // On-ramps sit on a solid embankment; the deck stands on pillars along its median, every 16 blocks
            if (ramp >= 0 && ramp == level) {
                return BRICKS;
            }
            boolean pillar = deck == TransitLayout.DECK_LEVEL
                    && (centerRow && isPillarSpot(blockX) || centerColumn && isPillarSpot(blockZ));
            return pillar ? BRICKS : null;
        }
        if (y > blockY && y <= blockY + 3) {
            boolean railing = y == blockY + 1 && (deck == TransitLayout.DECK_LEVEL && isDeckEdge(layout, blockX, blockZ) && !nearRampTop(layout, blockX, blockZ)
                    || ramp >= 2 && ramp == level && layout.isRampOuterEdge(blockX, blockZ));
            return railing ? WALL : AIR;
        }
        return null;
    }

    private static boolean isPillarSpot(int along) {
        int local = along & 15;
        return local == 7 || local == 8;
    }

    private static boolean isDeckEdge(TransitLayout layout, int blockX, int blockZ) {
        return layout.deckLevel(blockX + 1, blockZ) < 0 || layout.deckLevel(blockX - 1, blockZ) < 0
                || layout.deckLevel(blockX, blockZ + 1) < 0 || layout.deckLevel(blockX, blockZ - 1) < 0;
    }

    // Leave the railing open where an on-ramp reaches the deck
    private static boolean nearRampTop(TransitLayout layout, int blockX, int blockZ) {
        return layout.rampLevel(blockX + 1, blockZ) >= TransitLayout.DECK_LEVEL - 2 || layout.rampLevel(blockX - 1, blockZ) >= TransitLayout.DECK_LEVEL - 2
                || layout.rampLevel(blockX, blockZ + 1) >= TransitLayout.DECK_LEVEL - 2 || layout.rampLevel(blockX, blockZ - 1) >= TransitLayout.DECK_LEVEL - 2;
    }

    // ---- Elevated rail: along Z; across is local X ----

    @Nullable
    private static BlockState elevatedRail(ChunkTransit transit, int across, int y, int along) {
        int blockAlong = transit.chunkZ() * 16 + along;
        boolean powered = Math.floorMod(blockAlong, 8) == 0;
        boolean track = across == 6 || across == 9;
        boolean station = transit.railStation();
        // Portal-frame pillars on the sidewalks, every 16 blocks; none in intersections, where they'd block traffic
        boolean intersection = transit.plan().roadX() != null;
        if (along == 8 && !intersection && y > FLOOR_Y && y < GUIDEWAY_Y) {
            if (across == 0 || across == 15 || y == GUIDEWAY_Y - 1) {
                return BRICKS;
            }
        }
        if (station) {
            BlockState ladder = stationLadder(across, y, along, FLOOR_Y + 1, GUIDEWAY_Y, Direction.EAST, Direction.WEST, true);
            if (ladder != null) {
                return ladder;
            }
        }
        if (y == GUIDEWAY_Y) {
            if (across >= 4 && across <= 11) {
                if (track && powered) {
                    return Blocks.REDSTONE_BLOCK.getDefaultState();
                }
                return across == 4 || across == 11 ? pattern(transit.district()) : BRICKS;
            }
            return station && (across >= 1 && across <= 3 || across >= 12 && across <= 14) ? BRICKS : null;
        }
        if (y == GUIDEWAY_Y + 1) {
            if (track) {
                return rail(powered, true);
            }
            if (station && (across == 1 && along == 1 || across == 14 && along == 14)) {
                // Where the ladders come up
                return AIR;
            }
            int railingFirst = station ? 1 : 4;
            int railingLast = station ? 14 : 11;
            if (across == railingFirst || across == railingLast) {
                return WALL;
            }
            return across > railingFirst && across < railingLast ? AIR : null;
        }
        if (y > GUIDEWAY_Y + 1 && y <= GUIDEWAY_Y + 3 && across >= 4 && across <= 11) {
            return AIR;
        }
        return null;
    }

    // ---- Subway: along X; across is local Z ----

    @Nullable
    private static BlockState subway(ChunkTransit transit, int along, int y, int across) {
        int blockAlong = transit.chunkX() * 16 + along;
        boolean station = transit.subwayStation();
        int first = station ? 1 : 4;
        int last = station ? 14 : 11;
        if (y < SUBWAY_FLOOR_Y || y > FLOOR_Y) {
            return null;
        }
        if (station) {
            BlockState ladder = stationLadder(along, y, across, SUBWAY_FLOOR_Y + 1, FLOOR_Y, Direction.SOUTH, Direction.NORTH, false);
            if (ladder != null) {
                return ladder;
            }
            // Mark the shafts' openings in the street
            if (y == FLOOR_Y && (along == 2 && across == 1 || along == 13 && across == 14)) {
                return pattern(transit.district());
            }
        }
        if (y > SUBWAY_CEILING_Y) {
            return null;
        }
        boolean powered = Math.floorMod(blockAlong, 8) == 0;
        boolean track = across == 6 || across == 9;
        boolean inside = across >= first && across <= last;
        if (y == SUBWAY_FLOOR_Y) {
            return inside && track && powered ? Blocks.REDSTONE_BLOCK.getDefaultState() : across >= first - 1 && across <= last + 1 ? BRICKS : null;
        }
        if (y == SUBWAY_CEILING_Y) {
            if (!inside) {
                return across == first - 1 || across == last + 1 ? BRICKS : null;
            }
            return (across == 7 || across == 8) && Math.floorMod(blockAlong, 4) == 2 ? pattern(transit.district()) : BRICKS;
        }
        if (across == first - 1 || across == last + 1) {
            return y == SUBWAY_FLOOR_Y + 4 && Math.floorMod(blockAlong, 4) == 0 ? pattern(transit.district()) : BRICKS;
        }
        if (!inside) {
            return null;
        }
        if (y == SUBWAY_FLOOR_Y + 1 && track) {
            return rail(powered, false);
        }
        return AIR;
    }

    /**
     * The two ladder shafts of a station, in opposite corners of the chunk. Elevated stations put each ladder
     * against a column built up from the street; subway stations against the tunnel wall and the ground above.
     */
    @Nullable
    private static BlockState stationLadder(int across, int y, int along, int bottom, int top, Direction firstFacing,
                                            Direction secondFacing, boolean column) {
        if (y < bottom || y > top) {
            return null;
        }
        if (column) {
            if (across == 0 && along == 1 || across == 15 && along == 14) {
                return BRICKS;
            }
            if (across == 1 && along == 1) {
                return Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, firstFacing);
            }
            if (across == 14 && along == 14) {
                return Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, secondFacing);
            }
            return null;
        }
        // Subway: here "across" is the along-X position and "along" the across-Z one
        if (across == 1 && along == 1) {
            return Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, firstFacing);
        }
        if (across == 14 && along == 14) {
            return Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, secondFacing);
        }
        return null;
    }

    // ---- Skyways ----

    @Nullable
    private static BlockState skyway(ChunkTransit transit, Skyway skyway, int localX, int y, int localZ) {
        // The bridge crosses the road: along Z for a road running along X, and the other way round
        int lateral = skyway.roadAlongX() ? localX : localZ;
        int floor = skyway.standY() - 1;
        if (y < floor || y > skyway.standY() + 3 || lateral < 5 || lateral > 10) {
            return null;
        }
        if (y == floor) {
            return lateral == 6 || lateral == 9 ? pattern(transit.district()) : BRICKS;
        }
        if (y == skyway.standY() + 3) {
            return BRICKS;
        }
        if (lateral == 5 || lateral == 10) {
            return DistrictPalette.remap(Blocks.WHITE_STAINED_GLASS.getDefaultState(), transit.district());
        }
        return AIR;
    }

    // The building end of a skyway: a doorway through the wall and a landing out to the chunk edge
    @Nullable
    private static BlockState door(ChunkTransit transit, int localX, int y, int localZ) {
        for (Door door : transit.doors()) {
            int depth = switch (door.side()) {
                case 0 -> localZ;
                case 1 -> 15 - localZ;
                case 2 -> localX;
                default -> 15 - localX;
            };
            int lateral = door.side() < 2 ? localX : localZ;
            if (depth > 2 || lateral < 6 || lateral > 9) {
                continue;
            }
            if (y == door.standY() - 1) {
                return BRICKS;
            }
            if (y >= door.standY() && y <= door.standY() + 2) {
                return AIR;
            }
        }
        return null;
    }

    private static BlockState rail(boolean powered, boolean alongZ) {
        if (powered) {
            return Blocks.POWERED_RAIL.getDefaultState()
                    .with(PoweredRailBlock.SHAPE, alongZ ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST)
                    .with(PoweredRailBlock.POWERED, true);
        }
        return Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, alongZ ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST);
    }

    private static BlockState pattern(DyeColor color) {
        return ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get("full").get(color).getDefaultState();
    }
}
