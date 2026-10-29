package com.partatoes.digitalfrontier.grid.gen;

import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.grid.layout.ChunkPlan;
import com.partatoes.digitalfrontier.grid.layout.GridLevel;
import com.partatoes.digitalfrontier.grid.layout.PlanType;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.RoadClass;
import com.partatoes.digitalfrontier.grid.layout.TransitLayout;
import com.partatoes.digitalfrontier.grid.layout.TransitLayout.ExpressPiece;
import com.partatoes.digitalfrontier.grid.layout.TransitLayout.Skyway;
import com.partatoes.digitalfrontier.grid.layout.TransitLayout.StationInfo;
import com.partatoes.digitalfrontier.grid.layout.GridRandom;
import com.partatoes.digitalfrontier.grid.parts.DistrictPalette;
import com.partatoes.digitalfrontier.grid.parts.GridAssets;
import com.partatoes.digitalfrontier.grid.parts.GridBuildingDef.Kind;
import com.partatoes.digitalfrontier.grid.parts.GridPart;
import com.partatoes.digitalfrontier.grid.parts.SpiralStair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.partatoes.digitalfrontier.grid.layout.GridLayout.FLOOR_Y;

/**
 * Placeholder geometry for the city's levels ({@link TransitLayout}): skyroad decks and ramps, elevated rail
 * guideways, subway tunnels, stations and side passages, and skyway bridges with the doorways they lead into. Built
 * in code, like {@link GridTerrain}'s placeholder towers, wherever there's no template for a piece.
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
    // Station signs hang at eye height on the platforms
    private static final int SUBWAY_SIGN_Y = SUBWAY_FLOOR_Y + 3;
    private static final int CITY_STAIR_RISE = FLOOR_Y - SUBWAY_FLOOR_Y - 1;
    private static final int RAIL_STAIR_RISE = GUIDEWAY_Y - FLOOR_Y - 1;

    private static final BlockState BRICKS = ModBlocks.GRIDSTONE_BRICKS_BLOCK.defaultBlockState();
    private static final BlockState WALL = ModBlocks.GRIDSTONE_BRICKS_WALL.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState SLAB = ModBlocks.GRIDSTONE_BRICKS_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    private static final BlockState WALL_SIGN = Blocks.DARK_OAK_WALL_SIGN.defaultBlockState();
    private static final BlockState STANDING_SIGN = Blocks.DARK_OAK_SIGN.defaultBlockState();

    private GridTransit() {
    }

    /** A doorway in a building chunk's outer wall where a skyway arrives. Sides: 0 north, 1 south, 2 west, 3 east. */
    record Door(int side, int standY) {
    }

    /** A level template placed in this chunk: its part, where its Y = 0 goes, and how it's turned and flipped. */
    record Placement(GridPart part, int bottomY, Rotation rotation, Mirror mirror) {
        int topY() {
            return bottomY + part.sizeY() - 1;
        }
    }

    /**
     * A subway tunnel in this chunk. Its geometry is written once, for a tunnel along X (the canonical frame), and
     * turned by {@code rotation}: straight tunnels along Z turn clockwise, and corners are written for a tunnel
     * coming in from the south and leaving east.
     *
     * @param closedEnd for a terminus, the canonical end that's walled off (0 at X = 0, 1 at X = 15), else -1
     * @param passage   the canonical side with a maintenance passage (0 toward Z = 0, 1 toward Z = 15), else -1
     * @param alongBase the world coordinate along the line at canonical X = 0, for spacing the powered rails
     * @param stairRise for a station, how far its staircases climb from the platform
     */
    record Tunnel(Rotation rotation, boolean corner, boolean station, int closedEnd, int passage, int alongBase,
                  int stairRise, @Nullable ExpressPiece express) {
    }

    /** A sign with its text, at a position in the chunk; the block itself comes from {@link #blockAt}. */
    record Sign(BlockPos localPos, SignText text) {
    }

    /**
     * What the levels put in one chunk, worked out once per chunk. Pieces with a template are in
     * {@code placements}; the {@code ...Templated} flags say which pieces those replace in the code-built geometry.
     *
     * @param skyways     every skyway over this road chunk; {@code codeSkyways} are those without a template
     */
    record ChunkTransit(TransitLayout layout, ChunkPlan plan, int chunkX, int chunkZ, boolean elevatedRail,
                        @Nullable Tunnel tunnel, boolean railStation, List<Skyway> skyways, List<Skyway> codeSkyways,
                        List<Door> doors, boolean deckChunk, List<Placement> placements, boolean deckTemplated,
                        boolean rampTemplated, boolean railTemplated, boolean subwayTemplated, boolean passageTemplated) {
        boolean any() {
            return elevatedRail || tunnel != null || !skyways.isEmpty() || !doors.isEmpty() || deckChunk;
        }

        boolean subway() {
            return tunnel != null;
        }

        boolean subwayStation() {
            return tunnel != null && tunnel.station();
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
            if (tunnel != null && tunnel.station()) {
                max = Math.max(max, SUBWAY_FLOOR_Y + 1 + tunnel.stairRise() + 3);
            }
            for (Skyway skyway : skyways) {
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

    /**
     * @param groundTop the top of the ground in a column of this chunk, for staircases out of tunnels beyond the
     *                  city, where the ground isn't flat
     */
    static ChunkTransit forChunk(TransitLayout layout, GridAssets assets, long seed, ChunkPlan plan, int chunkX, int chunkZ,
                                 SkywayCheck check, IntBinaryOperator groundTop) {
        boolean road = plan.type() == PlanType.STREET;
        List<Skyway> skyways = new ArrayList<>();
        List<Door> doors = new ArrayList<>();
        if (road) {
            for (Skyway candidate : layout.skywaysAt(chunkX, chunkZ)) {
                if (check.fits(candidate, chunkX, chunkZ)) {
                    skyways.add(candidate);
                }
            }
        } else if (plan.type() == PlanType.BUILDING) {
            // Skyways in a neighbouring road chunk that lead to this building
            addDoors(doors, layout, check, chunkX, chunkZ - 1, true, 0);
            addDoors(doors, layout, check, chunkX, chunkZ + 1, true, 1);
            addDoors(doors, layout, check, chunkX - 1, chunkZ, false, 2);
            addDoors(doors, layout, check, chunkX + 1, chunkZ, false, 3);
        }
        boolean elevatedRail = road && layout.has(GridLevel.ELEVATED_RAIL)
                && layout.elevatedRail(chunkX, chunkZ);
        boolean deckChunk = road && layout.has(GridLevel.SKYROADS)
                && (isBoulevard(plan.roadX()) || isBoulevard(plan.roadZ()));
        boolean railStation = elevatedRail && layout.railStation(chunkX, chunkZ);
        Tunnel tunnel = layout.has(GridLevel.SUBWAY) ? tunnel(layout, plan, chunkX, chunkZ, groundTop) : null;

        // Templates, where the loaded assets have one for a piece; earlier placements win where they overlap
        Templates templates = new Templates(assets, seed, plan, chunkX, chunkZ);
        boolean rampTemplated = false;
        TransitLayout.RampPiece ramp = deckChunk ? layout.rampPiece(chunkX, chunkZ) : null;
        if (ramp != null) {
            rampTemplated = templates.add(ramp.upper() ? Kind.SKYROAD_RAMP_UPPER : Kind.SKYROAD_RAMP_LOWER,
                    Kind.SKYROAD_RAMP_LOWER.baseY(), rampRotation(ramp), rampMirror(ramp));
        }
        List<Skyway> codeSkyways = new ArrayList<>();
        for (Skyway skyway : skyways) {
            if (!templates.add(Kind.SKYWAY, skyway.standY() - 1,
                    skyway.roadAlongX() ? Rotation.NONE : Rotation.CLOCKWISE_90, Mirror.NONE)) {
                codeSkyways.add(skyway);
            }
        }
        // Elevated rail only runs along Z
        boolean intersection = plan.roadX() != null && plan.roadZ() != null;
        boolean railTemplated = elevatedRail && templates.add(
                railStation ? Kind.GUIDEWAY_STATION : intersection ? Kind.GUIDEWAY_CROSSING : Kind.GUIDEWAY,
                Kind.GUIDEWAY.baseY(), Rotation.CLOCKWISE_90, Mirror.NONE);
        boolean passageTemplated = false;
        boolean subwayTemplated = false;
        if (tunnel != null) {
            // The passage goes first, so its doorway wins over the tunnel wall
            passageTemplated = tunnel.passage() >= 0 && templates.add(Kind.SUBWAY_PASSAGE, Kind.SUBWAY_PASSAGE.baseY(),
                    tunnel.rotation(), tunnel.passage() == 1 ? Mirror.LEFT_RIGHT : Mirror.NONE);
            // Express termini have staircases as tall as the ground above them, so they're always built in code
            boolean terminus = tunnel.station() && tunnel.express() != null;
            Kind kind = tunnel.corner() ? Kind.SUBWAY_CORNER : tunnel.station() ? Kind.SUBWAY_STATION : Kind.SUBWAY;
            subwayTemplated = !terminus && templates.add(kind, kind.baseY(), tunnel.rotation(), Mirror.NONE);
        }
        TransitLayout.DeckPiece deck = deckChunk ? layout.deckPiece(chunkX, chunkZ) : null;
        boolean deckTemplated = deck != null && (deck.junction()
                ? templates.add(Kind.SKYROAD_JUNCTION, Kind.SKYROAD_JUNCTION.baseY(), junctionRotation(deck), Mirror.NONE)
                : templates.add(Kind.SKYROAD, Kind.SKYROAD.baseY(), deckRotation(deck), Mirror.NONE));

        return new ChunkTransit(layout, plan, chunkX, chunkZ, elevatedRail, tunnel, railStation, List.copyOf(skyways),
                List.copyOf(codeSkyways), List.copyOf(doors), deckChunk, List.copyOf(templates.placements), deckTemplated,
                rampTemplated, railTemplated, subwayTemplated, passageTemplated);
    }

    @Nullable
    private static Tunnel tunnel(TransitLayout layout, ChunkPlan plan, int chunkX, int chunkZ, IntBinaryOperator groundTop) {
        ExpressPiece express = layout.expressAt(chunkX, chunkZ);
        if (express == null) {
            // City lines run along X, under streets
            if (plan.type() != PlanType.STREET || !layout.subway(chunkX, chunkZ)) {
                return null;
            }
            boolean station = layout.subwayStation(chunkX, chunkZ);
            return new Tunnel(Rotation.NONE, false, station, -1, station ? -1 : layout.passageSide(chunkX, chunkZ),
                    chunkX * 16, CITY_STAIR_RISE, null);
        }
        if (express.corner()) {
            return new Tunnel(cornerRotation(express), true, false, -1, -1, 0, 0, express);
        }
        boolean alongX = express.from().getAxis() == Direction.Axis.X;
        Rotation rotation = alongX ? Rotation.NONE : Rotation.CLOCKWISE_90;
        // Canonical X runs along local X for a tunnel along X, along local Z for one turned onto Z
        int alongBase = alongX ? chunkX * 16 : chunkZ * 16;
        if (!express.station()) {
            return new Tunnel(rotation, false, false, -1, layout.passageSide(chunkX, chunkZ), alongBase, 0, express);
        }
        // A terminus: the line carries on toward whichever side has the next piece; the other end is walled off
        Direction open = layout.expressAt(chunkX + express.to().getStepX(), chunkZ + express.to().getStepZ()) != null
                ? express.to() : express.from();
        Direction closed = open.getOpposite();
        int closedEnd = closed == Direction.WEST || closed == Direction.NORTH ? 0 : 1;
        // The staircases come up wherever the ground is, which outside the city isn't always the street level
        int top = SUBWAY_CEILING_Y + 2;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                top = Math.max(top, Math.max(groundTop.applyAsInt(i, j), groundTop.applyAsInt(15 - i, 15 - j)));
            }
        }
        int rise = SpiralStair.fit(top - SUBWAY_FLOOR_Y - 1);
        return new Tunnel(rotation, false, true, closedEnd, -1, alongBase, rise, express);
    }

    // Corners are written coming in from the south and leaving east
    private static Rotation cornerRotation(ExpressPiece piece) {
        boolean north = piece.from() == Direction.NORTH || piece.to() == Direction.NORTH;
        boolean west = piece.from() == Direction.WEST || piece.to() == Direction.WEST;
        if (north) {
            return west ? Rotation.CLOCKWISE_180 : Rotation.COUNTERCLOCKWISE_90;
        }
        return west ? Rotation.CLOCKWISE_90 : Rotation.NONE;
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

        boolean add(Kind kind, int bottomY, Rotation rotation, Mirror mirror) {
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
    private static Rotation deckRotation(TransitLayout.DeckPiece deck) {
        if (deck.halfX() >= 0) {
            return deck.halfX() == 0 ? Rotation.NONE : Rotation.CLOCKWISE_180;
        }
        return deck.halfZ() == 0 ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
    }

    // Junction quarters are authored with the medians meeting at X = 15, Z = 15 (half 0 of both boulevards)
    private static Rotation junctionRotation(TransitLayout.DeckPiece deck) {
        if (deck.halfX() == 0) {
            return deck.halfZ() == 0 ? Rotation.NONE : Rotation.CLOCKWISE_90;
        }
        return deck.halfZ() == 0 ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_180;
    }

    // Ramps are authored in half 0's lane of a boulevard along X, climbing toward +X; flips cover the other ways
    private static Rotation rampRotation(TransitLayout.RampPiece ramp) {
        if (ramp.alongX()) {
            return ramp.half() == 1 && !ramp.risingPositive() ? Rotation.CLOCKWISE_180 : Rotation.NONE;
        }
        return ramp.half() == 0 ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
    }

    private static Mirror rampMirror(TransitLayout.RampPiece ramp) {
        if (ramp.alongX()) {
            if (ramp.half() == 0) {
                return ramp.risingPositive() ? Mirror.NONE : Mirror.FRONT_BACK;
            }
            return ramp.risingPositive() ? Mirror.LEFT_RIGHT : Mirror.NONE;
        }
        // Turned onto Z, the climb runs toward -Z for half 0 and +Z for half 1 unless flipped
        boolean flip = ramp.half() == 0 ? ramp.risingPositive() : !ramp.risingPositive();
        return flip ? Mirror.FRONT_BACK : Mirror.NONE;
    }

    @Nullable
    private static BlockState placed(ChunkTransit transit, int localX, int y, int localZ) {
        for (Placement placement : transit.placements()) {
            if (y < placement.bottomY() || y > placement.topY()) {
                continue;
            }
            // Undo the rotation, then the mirror, to find the template position
            int mx = canonicalX(placement.rotation(), localX, localZ);
            int mz = canonicalZ(placement.rotation(), localX, localZ);
            int px = placement.mirror() == Mirror.FRONT_BACK ? 15 - mx : mx;
            int pz = placement.mirror() == Mirror.LEFT_RIGHT ? 15 - mz : mz;
            BlockState state = placement.part().get(px, y - placement.bottomY(), pz);
            if (state != null) {
                return DistrictPalette.remap(state, transit.district()).mirror(placement.mirror()).rotate(placement.rotation());
            }
        }
        return null;
    }

    // Where a chunk position is in a piece authored along X and turned by a rotation, and back again
    private static int canonicalX(Rotation rotation, int localX, int localZ) {
        return switch (rotation) {
            case CLOCKWISE_90 -> localZ;
            case CLOCKWISE_180 -> 15 - localX;
            case COUNTERCLOCKWISE_90 -> 15 - localZ;
            default -> localX;
        };
    }

    private static int canonicalZ(Rotation rotation, int localX, int localZ) {
        return switch (rotation) {
            case CLOCKWISE_90 -> 15 - localX;
            case CLOCKWISE_180 -> 15 - localZ;
            case COUNTERCLOCKWISE_90 -> localX;
            default -> localZ;
        };
    }

    private static BlockPos toLocal(Rotation rotation, int x, int y, int z) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(15 - z, y, x);
            case CLOCKWISE_180 -> new BlockPos(15 - x, y, 15 - z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, y, 15 - x);
            default -> new BlockPos(x, y, z);
        };
    }

    private static boolean isBoulevard(@Nullable StreetAxis.Road road) {
        return road != null && road.roadClass() == RoadClass.BOULEVARD;
    }

    private static void addDoors(List<Door> doors, TransitLayout layout, SkywayCheck check, int roadX, int roadZ, boolean alongX, int side) {
        for (Skyway skyway : layout.skywaysAt(roadX, roadZ)) {
            if (skyway.roadAlongX() == alongX && check.fits(skyway, roadX, roadZ)) {
                doors.add(new Door(side, skyway.standY()));
            }
        }
    }

    @Nullable
    static BlockState blockAt(ChunkTransit transit, int localX, int y, int localZ) {
        // Signs and side passages come first: templates have to leave room for them
        BlockState state = sign(transit, localX, y, localZ);
        Tunnel tunnel = transit.tunnel();
        if (state == null && tunnel != null && tunnel.passage() >= 0 && !transit.passageTemplated()) {
            state = passage(tunnel, transit.district(), canonicalX(tunnel.rotation(), localX, localZ), y,
                    canonicalZ(tunnel.rotation(), localX, localZ));
        }
        if (state == null && !transit.placements().isEmpty()) {
            state = placed(transit, localX, y, localZ);
        }
        for (int i = 0; state == null && i < transit.codeSkyways().size(); i++) {
            state = skyway(transit, transit.codeSkyways().get(i), localX, y, localZ);
        }
        if (state == null && transit.elevatedRail() && !transit.railTemplated()) {
            state = elevatedRail(transit, localX, y, localZ);
        }
        if (state == null && transit.deckChunk() && !(transit.deckTemplated() && transit.rampTemplated())) {
            state = skyroad(transit, localX, y, localZ);
        }
        if (state == null && tunnel != null && !transit.subwayTemplated()) {
            state = subway(transit, tunnel, localX, y, localZ);
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
        if (station) {
            // Staircases up from the street, in two corners of the station
            BlockState stair = stationStair(transit, across, y, along, FLOOR_Y + 1, RAIL_STAIR_RISE);
            if (stair != null) {
                return stair;
            }
        }
        // Portal-frame pillars on the sidewalks, every 16 blocks; none in intersections, where they'd block traffic
        boolean intersection = transit.plan().roadX() != null;
        if (along == 8 && !intersection && y > FLOOR_Y && y < GUIDEWAY_Y) {
            if (across == 0 || across == 15 || y == GUIDEWAY_Y - 1) {
                return BRICKS;
            }
        }
        if (y == GUIDEWAY_Y) {
            if (across >= 4 && across <= 11) {
                if (track && powered) {
                    return Blocks.REDSTONE_BLOCK.defaultBlockState();
                }
                return across == 4 || across == 11 ? pattern(transit.district()) : BRICKS;
            }
            return station && (across >= 1 && across <= 3 || across >= 12 && across <= 14) ? BRICKS : null;
        }
        if (y == GUIDEWAY_Y + 1) {
            if (track) {
                return rail(powered, true);
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

    /**
     * The two spiral staircases of a station, in opposite corners of the chunk (0..3, 0..3 and 12..15, 12..15),
     * so each comes out toward the middle of the chunk.
     */
    @Nullable
    private static BlockState stationStair(ChunkTransit transit, int localX, int y, int localZ, int bottomY, int rise) {
        BlockState light = pattern(transit.district());
        if (localX <= 3 && localZ <= 3) {
            return SpiralStair.blockAt(localX, y - bottomY, localZ, rise, light);
        }
        if (localX >= 12 && localZ >= 12) {
            BlockState state = SpiralStair.blockAt(15 - localX, y - bottomY, 15 - localZ, rise, light);
            return state == null ? null : state.rotate(Rotation.CLOCKWISE_180);
        }
        return null;
    }

    // ---- Subway ----

    @Nullable
    private static BlockState subway(ChunkTransit transit, Tunnel tunnel, int localX, int y, int localZ) {
        if (tunnel.station()) {
            BlockState stair = stationStair(transit, localX, y, localZ, SUBWAY_FLOOR_Y + 1, tunnel.stairRise());
            if (stair != null) {
                return stair;
            }
        }
        if (y < SUBWAY_FLOOR_Y || y > SUBWAY_CEILING_Y) {
            return null;
        }
        int x = canonicalX(tunnel.rotation(), localX, localZ);
        int z = canonicalZ(tunnel.rotation(), localX, localZ);
        BlockState state = tunnel.corner()
                ? subwayCorner(transit.district(), x, y, z)
                : subwayStraight(transit.district(), tunnel, x, y, z);
        return state == null ? null : state.rotate(tunnel.rotation());
    }

    // A tunnel along X (across is Z), in the canonical frame
    @Nullable
    private static BlockState subwayStraight(DyeColor district, Tunnel tunnel, int along, int y, int across) {
        int blockAlong = tunnel.alongBase() + along;
        boolean station = tunnel.station();
        int first = station ? 1 : 4;
        int last = station ? 14 : 11;
        boolean powered = Math.floorMod(blockAlong, 8) == 0;
        boolean track = across == 6 || across == 9;
        boolean inside = across >= first && across <= last;
        // A terminus is walled off at its far end
        boolean endWall = tunnel.closedEnd() == 0 && along == 0 || tunnel.closedEnd() == 1 && along == 15;
        if (y == SUBWAY_FLOOR_Y) {
            return inside && track && powered && !endWall ? Blocks.REDSTONE_BLOCK.defaultBlockState()
                    : across >= first - 1 && across <= last + 1 ? BRICKS : null;
        }
        if (y == SUBWAY_CEILING_Y) {
            if (!inside) {
                return across == first - 1 || across == last + 1 ? BRICKS : null;
            }
            return (across == 7 || across == 8) && Math.floorMod(blockAlong, 4) == 2 ? pattern(district) : BRICKS;
        }
        if (across == first - 1 || across == last + 1) {
            return y == SUBWAY_FLOOR_Y + 4 && Math.floorMod(blockAlong, 4) == 0 ? pattern(district) : BRICKS;
        }
        if (!inside) {
            return null;
        }
        if (endWall) {
            return y == SUBWAY_FLOOR_Y + 4 && (across == 7 || across == 8) ? pattern(district) : BRICKS;
        }
        if (y == SUBWAY_FLOOR_Y + 1 && track) {
            return rail(powered, false);
        }
        return AIR;
    }

    /**
     * A corner, in the canonical frame: the tunnel comes in from the south (Z = 15) with tracks at X = 6 and 9, and
     * leaves east (X = 15) with tracks at Z = 6 and 9; the outer track turns at (6, 6), the inner one at (9, 9).
     */
    @Nullable
    private static BlockState subwayCorner(DyeColor district, int x, int y, int z) {
        boolean inside = isCornerInside(x, z);
        boolean wall = !inside && (isCornerInside(x + 1, z) || isCornerInside(x, z + 1) || isCornerInside(x + 1, z + 1)
                || isCornerInside(x - 1, z) || isCornerInside(x, z - 1) || isCornerInside(x - 1, z - 1)
                || isCornerInside(x + 1, z - 1) || isCornerInside(x - 1, z + 1));
        if (!inside && !wall) {
            return null;
        }
        if (y == SUBWAY_FLOOR_Y) {
            return BRICKS;
        }
        if (y == SUBWAY_CEILING_Y) {
            return inside && (x == 7 || x == 8) && (z == 7 || z == 8) ? pattern(district) : BRICKS;
        }
        if (wall) {
            return y == SUBWAY_FLOOR_Y + 4 && Math.floorMod(x + z, 4) == 0 ? pattern(district) : BRICKS;
        }
        if (y == SUBWAY_FLOOR_Y + 1) {
            RailShape shape = cornerTrack(x, z);
            if (shape != null) {
                return Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape);
            }
        }
        return AIR;
    }

    private static boolean isCornerInside(int x, int z) {
        return x >= 4 && x <= 15 && z >= 4 && z <= 15 && !(x >= 12 && z >= 12);
    }

    @Nullable
    private static RailShape cornerTrack(int x, int z) {
        for (int track : new int[]{6, 9}) {
            if (x == track && z == track) {
                return RailShape.SOUTH_EAST;
            }
            if (x == track && z > track) {
                return RailShape.NORTH_SOUTH;
            }
            if (z == track && x > track) {
                return RailShape.EAST_WEST;
            }
        }
        return null;
    }

    /**
     * A maintenance passage off a tunnel, in the canonical frame: a small room beside the tunnel, toward Z = 0 for
     * side 0 (side 1 is the mirror image), reached through a doorway in the tunnel wall.
     */
    @Nullable
    private static BlockState passage(Tunnel tunnel, DyeColor district, int x, int y, int z) {
        int side = tunnel.passage() == 1 ? 15 - z : z;
        if (y < SUBWAY_FLOOR_Y || y > SUBWAY_FLOOR_Y + 5 || x < 3 || x > 12 || side > 3) {
            return null;
        }
        if (side == 3) {
            // The doorway through the tunnel wall
            return (x == 7 || x == 8) && y > SUBWAY_FLOOR_Y && y <= SUBWAY_FLOOR_Y + 2 ? AIR : null;
        }
        boolean inside = x >= 4 && x <= 11 && side >= 1;
        if (!inside || y == SUBWAY_FLOOR_Y) {
            return BRICKS;
        }
        if (y == SUBWAY_FLOOR_Y + 5) {
            return x == 6 || x == 9 ? pattern(district) : BRICKS;
        }
        // Racks of lit panels along the back wall
        if (side == 1 && (x == 4 || x == 5 || x == 10 || x == 11) && y <= SUBWAY_FLOOR_Y + 3) {
            return y == SUBWAY_FLOOR_Y + 2 ? pattern(district) : BRICKS;
        }
        return AIR;
    }

    /** Where a corrupted program can stand in this chunk's maintenance passage, if it has one. */
    @Nullable
    static BlockPos passageSpot(ChunkTransit transit) {
        Tunnel tunnel = transit.tunnel();
        if (tunnel == null || tunnel.passage() < 0) {
            return null;
        }
        return toLocal(tunnel.rotation(), 7, SUBWAY_FLOOR_Y + 1, tunnel.passage() == 1 ? 13 : 2);
    }

    // ---- Station signs ----

    /** The signs naming this chunk's stations, with their text. */
    static List<Sign> signs(ChunkTransit transit) {
        List<Sign> signs = new ArrayList<>();
        TransitLayout layout = transit.layout();
        Tunnel tunnel = transit.tunnel();
        if (tunnel != null && tunnel.station()) {
            StationInfo info = layout.stationInfo(transit.chunkX(), transit.chunkZ(), false);
            // On both walls: the name on the left and the line on the right, seen from the platform
            Rotation rotation = tunnel.rotation();
            signs.add(new Sign(toLocal(rotation, 7, SUBWAY_SIGN_Y, 1), nameText(info)));
            signs.add(new Sign(toLocal(rotation, 8, SUBWAY_SIGN_Y, 1), lineText(info)));
            signs.add(new Sign(toLocal(rotation, 8, SUBWAY_SIGN_Y, 14), nameText(info)));
            signs.add(new Sign(toLocal(rotation, 7, SUBWAY_SIGN_Y, 14), lineText(info)));
        }
        if (transit.railStation()) {
            StationInfo info = layout.stationInfo(transit.chunkX(), transit.chunkZ(), true);
            // Standing on the platforms either side of the tracks, facing them
            signs.add(new Sign(new BlockPos(2, GUIDEWAY_Y + 1, 8), nameText(info)));
            signs.add(new Sign(new BlockPos(2, GUIDEWAY_Y + 1, 7), lineText(info)));
            signs.add(new Sign(new BlockPos(13, GUIDEWAY_Y + 1, 7), nameText(info)));
            signs.add(new Sign(new BlockPos(13, GUIDEWAY_Y + 1, 8), lineText(info)));
        }
        return signs;
    }

    @Nullable
    private static BlockState sign(ChunkTransit transit, int localX, int y, int localZ) {
        Tunnel tunnel = transit.tunnel();
        if (y == SUBWAY_SIGN_Y && tunnel != null && tunnel.station()) {
            int x = canonicalX(tunnel.rotation(), localX, localZ);
            int z = canonicalZ(tunnel.rotation(), localX, localZ);
            if ((x == 7 || x == 8) && (z == 1 || z == 14)) {
                return WALL_SIGN.setValue(WallSignBlock.FACING, z == 1 ? Direction.SOUTH : Direction.NORTH).rotate(tunnel.rotation());
            }
        }
        if (y == GUIDEWAY_Y + 1 && transit.railStation() && (localZ == 7 || localZ == 8)) {
            // Sign rotation 12 faces east, 4 west
            if (localX == 2) {
                return STANDING_SIGN.setValue(StandingSignBlock.ROTATION, 12);
            }
            if (localX == 13) {
                return STANDING_SIGN.setValue(StandingSignBlock.ROTATION, 4);
            }
        }
        return null;
    }

    private static SignText nameText(StationInfo info) {
        String[] words = info.name().split(" ", 2);
        return new SignText()
                .setMessage(1, Component.literal(words[0]))
                .setMessage(2, Component.literal(words.length > 1 ? words[1] : ""))
                .setColor(DyeColor.WHITE).setHasGlowingText(true);
    }

    private static SignText lineText(StationInfo info) {
        SignText text = new SignText().setColor(info.color()).setHasGlowingText(true);
        if (info.waypoint() > 0) {
            return text.setMessage(1, Component.translatable("sign.digitalfrontier.express"))
                    .setMessage(2, Component.translatable("sign.digitalfrontier.express_to", info.waypoint()));
        }
        return text.setMessage(1, Component.translatable("color.minecraft." + info.color().getSerializedName()))
                .setMessage(2, Component.translatable("sign.digitalfrontier.line"))
                .setMessage(3, Component.translatable(info.elevated() ? "sign.digitalfrontier.elevated" : "sign.digitalfrontier.subway"));
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
            return DistrictPalette.remap(Blocks.WHITE_STAINED_GLASS.defaultBlockState(), transit.district());
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
            return Blocks.POWERED_RAIL.defaultBlockState()
                    .setValue(PoweredRailBlock.SHAPE, alongZ ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST)
                    .setValue(PoweredRailBlock.POWERED, true);
        }
        return Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, alongZ ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST);
    }

    private static BlockState pattern(DyeColor color) {
        return ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.get("full").get(color).defaultBlockState();
    }
}
