package com.partatoes.digitalfrontier.grid.layout;

import com.partatoes.digitalfrontier.grid.layout.StreetAxis.Road;
import com.partatoes.digitalfrontier.grid.layout.StreetAxis.RoadClass;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The city's levels above and below the streets, decided from the street pattern ({@link StreetAxis}) and the grid
 * seed alone, so each chunk can still work out what it holds on its own:
 * <ul>
 *   <li><b>Skyroads</b>: a 16-wide light-cycle deck over every boulevard, centered on its median, at surface
 *       height Y 80. It ramps down to the street at the city edge, and on-ramps climb the outer lane of
 *       one boulevard half along some of the city blocks.</li>
 *   <li><b>Elevated rail</b>: guideways over some one-chunk streets running along Z (north–south).</li>
 *   <li><b>Subway</b>: tunnels under some one-chunk streets running along X (east–west).</li>
 *   <li><b>Skyways</b>: bridges across a one-chunk road between the towers on either side.</li>
 * </ul>
 * Rail and subway lines run along different axes, so their tracks never cross at the same level. Both have
 * stations where a cross road meets them.
 *
 * <p>Ramp heights are in half blocks above the street surface: 0 is the street (Y 65 to stand on), 30 the deck
 * (Y 80).
 */
public final class TransitLayout {
    public static final int DECK_LEVEL = 30;
    /** Blocks either side of a boulevard's median that are deck. */
    public static final int DECK_HALF_WIDTH = 8;
    public static final int MIN_LINE_SPACING = 6;
    public static final int MIN_STATION_SPACING = 6;
    public static final int MAX_STATION_SPACING = 10;
    /** On-ramps keep this many chunks clear of the end ramps. */
    private static final int RAMP_END_CLEARANCE = 3;

    private static final int SALT_SEED = 0x7A;
    private static final int SALT_RAMP = 1;
    private static final int SALT_RAIL_LINE = 2;
    private static final int SALT_STATION = 3;
    private static final int SALT_SKYWAY = 4;

    /** A skyway bridge crossing a road chunk, between the building chunks either side; Y is its walking height. */
    public record Skyway(boolean roadAlongX, int standY) {
    }

    private final GridLayoutSettings settings;
    private final StreetAxis xAxis;
    private final StreetAxis zAxis;
    private final long seed;
    private final Set<Integer> railColumns = new HashSet<>();
    private final Set<Integer> subwayRows = new HashSet<>();
    private final Set<Long> railStations = new HashSet<>();
    private final Set<Long> subwayStations = new HashSet<>();

    TransitLayout(GridLayoutSettings settings, StreetAxis xAxis, StreetAxis zAxis, List<Site> waypoints) {
        this.settings = settings;
        this.xAxis = xAxis;
        this.zAxis = zAxis;
        this.seed = GridRandom.hash(settings.gridSeed(), 0, 0, SALT_SEED);
        if (has(GridLevel.ELEVATED_RAIL)) {
            // Streets along Z sit at x-axis road positions
            railColumns.addAll(pickLines(xAxis, 0));
            railColumns.forEach(column -> addStations(railStations, column, zAxis, true));
        }
        if (has(GridLevel.SUBWAY)) {
            subwayRows.addAll(pickLines(zAxis, 1));
            subwayRows.forEach(row -> addStations(subwayStations, row, xAxis, false));
            for (int i = 0; i < waypoints.size(); i++) {
                addExpressLine(i, waypoints.get(i));
            }
        }
    }

    // ---- Express lines to the waypoints ----

    /**
     * One chunk of an express subway line: the sides of the chunk its tracks leave by (opposite sides for a
     * straight, adjacent ones for a corner; a terminus station is straight, its tracks ending at the far wall).
     *
     * @param line which waypoint's line, counting from 0
     */
    public record ExpressPiece(Direction from, Direction to, boolean station, int line) {
        public boolean corner() {
            return from.getAxis() != to.getAxis();
        }
    }

    private final Map<Long, ExpressPiece> express = new HashMap<>();

    @Nullable
    public ExpressPiece expressAt(int chunkX, int chunkZ) {
        return express.get(key(chunkX, chunkZ));
    }

    /**
     * Each waypoint gets its own line, from a station just outside the city to a station beside the waypoint. Lines
     * to waypoints on the axes run straight along the axis; lines to diagonal waypoints run along a column two chunks
     * off the Z axis, then turn along the waypoint's row. That way no two lines ever meet, and none enters the city,
     * so they never cross the city's own subway lines at the same level.
     */
    private void addExpressLine(int line, Site waypoint) {
        int wx = waypoint.chunkX();
        int wz = waypoint.chunkZ();
        int radius = settings.cityRadius();
        List<int[]> path = new ArrayList<>();
        if (wz == 0 || wx == 0) {
            boolean alongX = wz == 0;
            int sign = Integer.signum(alongX ? wx : wz);
            for (int c = sign * (radius + 1); c != (alongX ? wx : wz); c += sign) {
                path.add(alongX ? new int[]{c, 0} : new int[]{0, c});
            }
        } else {
            int column = Integer.signum(wx) * 2;
            int signZ = Integer.signum(wz);
            int start = (int) Math.floor(Math.sqrt((double) radius * radius - column * column)) + 1;
            for (int z = signZ * start; z != wz + signZ; z += signZ) {
                path.add(new int[]{column, z});
            }
            int signX = Integer.signum(wx);
            for (int x = column + signX; x != wx; x += signX) {
                path.add(new int[]{x, wz});
            }
        }
        if (path.size() < 2) {
            return;
        }
        for (int i = 0; i < path.size(); i++) {
            int[] here = path.get(i);
            Direction toPrevious = i > 0 ? direction(here, path.get(i - 1)) : null;
            Direction toNext = i < path.size() - 1 ? direction(here, path.get(i + 1)) : null;
            boolean end = toPrevious == null || toNext == null;
            Direction from = toPrevious != null ? toPrevious : toNext.getOpposite();
            Direction to = toNext != null ? toNext : toPrevious.getOpposite();
            express.put(key(here[0], here[1]), new ExpressPiece(from, to, end, line));
        }
    }

    private static Direction direction(int[] from, int[] to) {
        if (to[0] > from[0]) {
            return Direction.EAST;
        }
        if (to[0] < from[0]) {
            return Direction.WEST;
        }
        return to[1] > from[1] ? Direction.SOUTH : Direction.NORTH;
    }

    // ---- Stations: names and lines ----

    private static final String[] NAME_FIRST = {"Kernel", "Vector", "Cipher", "Sector", "Lattice", "Beacon", "Binary",
            "Cache", "Relay", "Daemon", "Pixel", "Quorum", "Halo", "Ion", "Signal", "Matrix", "Flux", "Syntax", "Proxy", "Echo"};
    private static final String[] NAME_SECOND = {"Junction", "Gate", "Hub", "Exchange", "Terminal", "Plaza", "Spire",
            "Loop", "Point", "Crossing", "Square", "Yard"};
    private static final DyeColor[] LINE_COLORS = {DyeColor.CYAN, DyeColor.ORANGE, DyeColor.LIME, DyeColor.MAGENTA,
            DyeColor.YELLOW, DyeColor.LIGHT_BLUE, DyeColor.PINK, DyeColor.PURPLE};
    private static final int SALT_NAME = 6;

    /**
     * What a station's signs say: its name (made up from the seed), and its line: a color for city lines, a
     * waypoint number for express lines.
     *
     * @param waypoint the waypoint an express line runs to (counting from 1), or 0 for a city line
     */
    public record StationInfo(String name, DyeColor color, int waypoint, boolean elevated) {
    }

    public StationInfo stationInfo(int chunkX, int chunkZ, boolean elevated) {
        long hash = GridRandom.hash(seed, chunkX, chunkZ, SALT_NAME);
        String name = NAME_FIRST[GridRandom.nextInt(hash, NAME_FIRST.length)] + " "
                + NAME_SECOND[GridRandom.nextInt(hash >>> 24, NAME_SECOND.length)];
        ExpressPiece piece = elevated ? null : expressAt(chunkX, chunkZ);
        if (piece != null) {
            return new StationInfo(name, LINE_COLORS[piece.line() % LINE_COLORS.length], piece.line() + 1, false);
        }
        // City lines are numbered along their axis, so neighbouring lines get different colors
        List<Integer> lines = (elevated ? railColumns : subwayRows).stream().sorted().toList();
        int index = Math.max(0, lines.indexOf(elevated ? chunkX : chunkZ));
        return new StationInfo(name, LINE_COLORS[(index + (elevated ? 4 : 0)) % LINE_COLORS.length], 0, elevated);
    }

    // ---- Subway side passages ----

    private static final int SALT_PASSAGE = 7;

    /**
     * Whether a straight subway chunk (city or express, not a station) has a maintenance passage off it, and on
     * which side: 0 toward lower coordinates, 1 toward higher, -1 for none.
     */
    public int passageSide(int chunkX, int chunkZ) {
        long hash = GridRandom.hash(seed, chunkX, chunkZ, SALT_PASSAGE);
        if (GridRandom.unit(hash) >= 0.15) {
            return -1;
        }
        return (int) (hash >>> 1 & 1);
    }

    public boolean has(GridLevel level) {
        return settings.levels().contains(level);
    }

    // ---- Skyroads ----

    /**
     * The skyroad deck's height at a block (in half blocks above the street, {@link #DECK_LEVEL} on the deck itself,
     * less on the end ramps), or -1 if there's no deck here.
     */
    public int deckLevel(int blockX, int blockZ) {
        if (!has(GridLevel.SKYROADS)) {
            return -1;
        }
        return Math.max(deckLevelAlong(zAxis, blockZ, blockX), deckLevelAlong(xAxis, blockX, blockZ));
    }

    /** Whether this block row is the center line of a deck running along X (the lit median line). */
    public boolean isDeckCenterRow(int blockZ) {
        return has(GridLevel.SKYROADS) && isCenter(zAxis, blockZ);
    }

    /** Whether this block column is the center line of a deck running along Z. */
    public boolean isDeckCenterColumn(int blockX) {
        return has(GridLevel.SKYROADS) && isCenter(xAxis, blockX);
    }

    private boolean isCenter(StreetAxis axis, int across) {
        Road road = axis.roadAt(across >> 4);
        if (road == null || road.roadClass() != RoadClass.BOULEVARD) {
            return false;
        }
        int local = across & 15;
        return road.half() == 0 ? local == 15 : local == 0;
    }

    // A deck along a boulevard: across the band next to the median, along the line between its end ramps
    private int deckLevelAlong(StreetAxis acrossAxis, int across, int along) {
        Road road = acrossAxis.roadAt(across >> 4);
        if (road == null || road.roadClass() != RoadClass.BOULEVARD) {
            return -1;
        }
        int local = across & 15;
        boolean inBand = road.half() == 0 ? local >= 16 - DECK_HALF_WIDTH : local < DECK_HALF_WIDTH;
        if (!inBand) {
            return -1;
        }
        int end = lineEnd(boulevardRows(across >> 4, road));
        // Blocks inside the city along the line run from -end*16 to (end+1)*16 - 1
        int fromEnd = Math.min((end + 1) * 16 - 1 - along, along + end * 16);
        return fromEnd < 0 ? -1 : Math.min(DECK_LEVEL, fromEnd);
    }

    /**
     * An on-ramp's height at a block, or -1 if there's none. On-ramps climb two chunks of one boulevard half's outer
     * lane, alongside the deck, on about half the city blocks the boulevard passes.
     */
    public int rampLevel(int blockX, int blockZ) {
        if (!has(GridLevel.SKYROADS)) {
            return -1;
        }
        return Math.max(rampLevelAlong(zAxis, xAxis, blockZ, blockX, 0), rampLevelAlong(xAxis, zAxis, blockX, blockZ, 1));
    }

    /** Whether this ramp block is on the ramp's outer edge, away from the deck (where its railing goes). */
    public boolean isRampOuterEdge(int blockX, int blockZ) {
        return isRampOuter(zAxis, blockZ) || isRampOuter(xAxis, blockX);
    }

    private boolean isRampOuter(StreetAxis acrossAxis, int across) {
        Road road = acrossAxis.roadAt(across >> 4);
        if (road == null || road.roadClass() != RoadClass.BOULEVARD) {
            return false;
        }
        return (across & 15) == (road.half() == 0 ? rampFirst(0) : rampLast(1));
    }

    // Ramp lanes: the 6 blocks of a half just outside the deck band
    private static int rampFirst(int half) {
        return half == 0 ? 16 - DECK_HALF_WIDTH - 6 : DECK_HALF_WIDTH;
    }

    private static int rampLast(int half) {
        return rampFirst(half) + 5;
    }

    private int rampLevelAlong(StreetAxis acrossAxis, StreetAxis alongAxis, int across, int along, int axisSalt) {
        Road road = acrossAxis.roadAt(across >> 4);
        if (road == null || road.roadClass() != RoadClass.BOULEVARD) {
            return -1;
        }
        int local = across & 15;
        if (local < rampFirst(road.half()) || local > rampLast(road.half())) {
            return -1;
        }
        Ramp ramp = rampAlong(acrossAxis, alongAxis, across >> 4, along >> 4, axisSalt);
        if (ramp == null) {
            return -1;
        }
        int step = along - ramp.startChunk() * 16;
        if (!ramp.risingPositive()) {
            step = 31 - step;
        }
        return Math.min(DECK_LEVEL, step);
    }

    /**
     * An on-ramp along one boulevard: which half's lane it climbs, which way, and the first of its two chunks. The
     * same for every chunk of the ramp and every block across the boulevard.
     */
    private record Ramp(int half, boolean risingPositive, int startChunk) {
    }

    @Nullable
    private Ramp rampAlong(StreetAxis acrossAxis, StreetAxis alongAxis, int acrossChunk, int alongChunk, int axisSalt) {
        Road road = acrossAxis.roadAt(acrossChunk);
        if (road == null || road.roadClass() != RoadClass.BOULEVARD || alongAxis.roadAt(alongChunk) != null) {
            return null;
        }
        StreetAxis.Block block = alongAxis.blockAt(alongChunk);
        if (block.size() < 2 || alongChunk > block.start() + 1) {
            return null;
        }
        int[] rows = boulevardRows(acrossChunk, road);
        int end = lineEnd(rows);
        if (Math.abs(block.start()) > end - RAMP_END_CLEARANCE || Math.abs(block.start() + 1) > end - RAMP_END_CLEARANCE) {
            return null;
        }
        long hash = GridRandom.hash(seed, block.index(), rows[0], SALT_RAMP * 4 + axisSalt);
        if (GridRandom.unit(hash) >= 0.5 || (int) (hash >>> 1 & 1) != road.half()) {
            return null;
        }
        return new Ramp(road.half(), (hash >>> 2 & 1) == 0, block.start());
    }

    // ---- Pieces, for templates ----

    /**
     * The skyroad deck piece a chunk holds, if it's a whole one: a straight half along X or Z, or a quarter of a
     * junction. Null where there's no deck, or only part of one (the end ramps), which are built in code.
     *
     * @param halfX the half of the boulevard running along X through this chunk (-1 if none)
     * @param halfZ the half of the boulevard running along Z through this chunk (-1 if none)
     */
    public record DeckPiece(int halfX, int halfZ) {
        public boolean junction() {
            return halfX >= 0 && halfZ >= 0;
        }
    }

    @Nullable
    public DeckPiece deckPiece(int chunkX, int chunkZ) {
        if (!has(GridLevel.SKYROADS)) {
            return null;
        }
        int halfX = fullDeckHalf(zAxis, chunkZ, chunkX);
        int halfZ = fullDeckHalf(xAxis, chunkX, chunkZ);
        Road roadX = zAxis.roadAt(chunkZ);
        Road roadZ = xAxis.roadAt(chunkX);
        boolean boulevardX = roadX != null && roadX.roadClass() == RoadClass.BOULEVARD;
        boolean boulevardZ = roadZ != null && roadZ.roadClass() == RoadClass.BOULEVARD;
        // Every boulevard through the chunk must have its whole deck here
        if (!boulevardX && !boulevardZ || boulevardX && halfX < 0 || boulevardZ && halfZ < 0) {
            return null;
        }
        return new DeckPiece(halfX, halfZ);
    }

    // The boulevard half, if this chunk is a full-height stretch of its deck; -1 otherwise
    private int fullDeckHalf(StreetAxis acrossAxis, int acrossChunk, int alongChunk) {
        Road road = acrossAxis.roadAt(acrossChunk);
        if (road == null || road.roadClass() != RoadClass.BOULEVARD) {
            return -1;
        }
        int end = lineEnd(boulevardRows(acrossChunk, road));
        int first = alongChunk * 16;
        int fromEnd = Math.min((end + 1) * 16 - 1 - (first + 15), first + end * 16);
        return fromEnd >= DECK_LEVEL ? road.half() : -1;
    }

    /**
     * The on-ramp piece a chunk holds: which line and half, which way it climbs, and whether this is the upper of
     * its two chunks (the one that reaches the deck).
     */
    public record RampPiece(boolean alongX, int half, boolean risingPositive, boolean upper) {
    }

    @Nullable
    public RampPiece rampPiece(int chunkX, int chunkZ) {
        if (!has(GridLevel.SKYROADS)) {
            return null;
        }
        Ramp ramp = rampAlong(zAxis, xAxis, chunkZ, chunkX, 0);
        boolean alongX = true;
        int alongChunk = chunkX;
        if (ramp == null) {
            ramp = rampAlong(xAxis, zAxis, chunkX, chunkZ, 1);
            alongX = false;
            alongChunk = chunkZ;
        }
        if (ramp == null) {
            return null;
        }
        boolean second = alongChunk == ramp.startChunk() + 1;
        return new RampPiece(alongX, ramp.half(), ramp.risingPositive(), second == ramp.risingPositive());
    }

    // The two chunk rows (or columns) of a boulevard, given one of them
    private static int[] boulevardRows(int c, Road road) {
        int first = road.half() == 0 ? c : c - 1;
        return new int[]{first, first + 1};
    }

    /** The last chunk along a line (both directions, symmetric) that is still inside the city on all of its rows. */
    private int lineEnd(int... rows) {
        int farthest = 0;
        for (int row : rows) {
            farthest = Math.max(farthest, Math.abs(row));
        }
        int radius = settings.cityRadius();
        return farthest > radius ? -1 : (int) Math.floor(Math.sqrt((double) radius * radius - (double) farthest * farthest));
    }

    // ---- Rail ----

    /** Whether an elevated rail guideway runs along Z through this chunk. */
    public boolean elevatedRail(int chunkX, int chunkZ) {
        return railColumns.contains(chunkX) && Math.abs(chunkZ) <= lineEnd(chunkX);
    }

    /** Whether a subway tunnel runs along X under this chunk. */
    public boolean subway(int chunkX, int chunkZ) {
        return subwayRows.contains(chunkZ) && Math.abs(chunkX) <= lineEnd(chunkZ);
    }

    /** Whether this chunk holds an elevated rail station (stations are always on intersections). */
    public boolean railStation(int chunkX, int chunkZ) {
        return railStations.contains(key(chunkX, chunkZ));
    }

    /** Whether this chunk holds a subway station (stations are always on intersections). */
    public boolean subwayStation(int chunkX, int chunkZ) {
        return subwayStations.contains(key(chunkX, chunkZ));
    }

    // Lines from the center outward: ordinary one-chunk streets, picked by seed, never closer than MIN_LINE_SPACING
    private List<Integer> pickLines(StreetAxis axis, int salt) {
        List<Integer> candidates = new ArrayList<>();
        for (int c = -axis.extent(); c <= axis.extent(); c++) {
            Road road = axis.roadAt(c);
            if (road != null && road.roadClass() == RoadClass.STREET && lineEnd(c) > 0) {
                candidates.add(c);
            }
        }
        candidates.sort(Comparator.<Integer>comparingInt(Math::abs).thenComparingInt(c -> c));
        List<Integer> picked = new ArrayList<>();
        for (int c : candidates) {
            boolean spaced = picked.stream().allMatch(other -> Math.abs(other - c) >= MIN_LINE_SPACING);
            if (spaced && GridRandom.unit(GridRandom.hash(seed, c, salt, SALT_RAIL_LINE)) < 0.5) {
                picked.add(c);
            }
        }
        return picked;
    }

    // Stations at intersections along a line: at least MIN_STATION_SPACING apart, never more than MAX_STATION_SPACING
    private void addStations(Set<Long> stations, int line, StreetAxis crossAxis, boolean lineAlongZ) {
        int end = lineEnd(line);
        int last = Integer.MIN_VALUE / 2;
        for (int c = -end; c <= end; c++) {
            if (crossAxis.roadAt(c) == null || c - last < MIN_STATION_SPACING) {
                continue;
            }
            boolean due = c - last >= MAX_STATION_SPACING - 1;
            if (due || GridRandom.unit(GridRandom.hash(seed, line, c, SALT_STATION)) < 0.4) {
                stations.add(lineAlongZ ? key(line, c) : key(c, line));
                last = c;
            }
        }
    }

    // ---- Skyways ----

    /**
     * The skyway candidates on this chunk, lowest first: a one-chunk street or lane (not an intersection, no rail
     * over it) with city blocks on both sides gets one about a third of the time, and some of those a second, two
     * floors higher. Whether the towers either side are tall enough is up to the caller, which knows their buildings.
     */
    public List<Skyway> skywaysAt(int chunkX, int chunkZ) {
        if (!has(GridLevel.SKYWAYS) || Math.hypot(chunkX, chunkZ) > settings.cityRadius() - 1) {
            return List.of();
        }
        Road roadX = zAxis.roadAt(chunkZ);
        Road roadZ = xAxis.roadAt(chunkX);
        if ((roadX == null) == (roadZ == null)) {
            return List.of();
        }
        Road road = roadX != null ? roadX : roadZ;
        boolean alongX = roadX != null;
        if (road.roadClass() == RoadClass.BOULEVARD || (!alongX && railColumns.contains(chunkX))) {
            return List.of();
        }
        // The chunks either side, across the road, must be blocks
        StreetAxis acrossAxis = alongX ? zAxis : xAxis;
        int across = alongX ? chunkZ : chunkX;
        if (acrossAxis.roadAt(across - 1) != null || acrossAxis.roadAt(across + 1) != null) {
            return List.of();
        }
        long hash = GridRandom.hash(seed, chunkX, chunkZ, SALT_SKYWAY);
        if (GridRandom.unit(hash) >= 0.3) {
            return List.of();
        }
        int floor = 6 + GridRandom.nextInt(hash >>> 16, 3);
        Skyway first = new Skyway(alongX, GridLayout.FLOOR_Y + 1 + floor * GridLayout.FLOOR_HEIGHT);
        if (GridRandom.unit(GridRandom.hash(seed, chunkX, chunkZ, SALT_SKYWAY + 100)) >= 0.4) {
            return List.of(first);
        }
        return List.of(first, new Skyway(alongX, first.standY() + 2 * GridLayout.FLOOR_HEIGHT));
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
