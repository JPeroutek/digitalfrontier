package com.partatoes.digitalfrontier.grid.layout;

import net.minecraft.util.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * The macro map of one grid. A pure function of {@link GridLayoutSettings}: no world access, so every
 * chunk can work out what it contains on its own, in any order (the core idea borrowed from Lost Cities).
 */
public final class GridLayout {
    public static final int FLOOR_Y = 64;
    public static final int FLOOR_HEIGHT = 6;
    /** Radius in chunks of the area a waypoint occupies. */
    public static final int SITE_RADIUS = 4;
    public static final String MAIN_ARRIVAL_ID = "city_main";

    private static final int SALT_WAYPOINTS = 1;
    private static final int SALT_STRONGHOLD = 2;
    private static final int SALT_DISTRICTS = 3;
    private static final int SALT_PLAN = 4;
    private static final int SALT_FLOORS = 5;
    private static final int SALT_MULTI = 6;
    private static final int SALT_MULTI_OFFSET_X = 7;
    private static final int SALT_MULTI_OFFSET_Z = 8;
    private static final int SALT_EXTRA_STRONGHOLDS = 9;
    private static final int SALT_STREETS_X = 10;
    private static final int SALT_STREETS_Z = 11;

    private final GridLayoutSettings settings;
    private final List<Site> sites;
    private final List<Site> waypoints;
    private final List<Site> terminals;
    private final Map<Long, Site> sitesByChunk = new HashMap<>();
    private final CorruptionField corruption;
    private final double districtRotation;
    // Streets along X are found on the Z axis (a row of chunks), streets along Z on the X axis
    private final StreetAxis xAxis;
    private final StreetAxis zAxis;
    private final TransitLayout transit;

    public GridLayout(GridLayoutSettings settings) {
        this.settings = settings;
        long seed = settings.gridSeed();

        List<Site> allSites = new ArrayList<>();
        allSites.add(new Site(Site.Type.ARRIVAL_TERMINAL, MAIN_ARRIVAL_ID, 0, settings.cityRadius() + 2));

        List<Site> waypointSites = new ArrayList<>();
        if (settings.waypointCount() > 0) {
            int step = 8 / settings.waypointCount();
            int offset = GridRandom.nextInt(GridRandom.hash(seed, 0, 0, SALT_WAYPOINTS), step);
            for (int i = 0; i < settings.waypointCount(); i++) {
                waypointSites.add(siteOnRing(Site.Type.WAYPOINT, "waypoint_" + i,
                        (offset + i * step) * Math.PI / 4, settings.waypointRingRadius()));
            }
        }
        allSites.addAll(waypointSites);

        // Strongholds and the portal sit in the 8 slots between the waypoint directions, so they never collide with
        // a waypoint. The portal is opposite the first stronghold; more corrupted grids get more strongholds, in
        // other slots picked by the seed.
        int firstSlot = GridRandom.nextInt(GridRandom.hash(seed, 0, 0, SALT_STRONGHOLD), 8);
        int portalSlot = (firstSlot + 4) % 8;
        List<Integer> slots = new ArrayList<>(List.of(firstSlot));
        IntStream.range(0, 8)
                .filter(slot -> slot != firstSlot && slot != portalSlot)
                .boxed()
                .sorted(Comparator.comparingLong(slot -> GridRandom.hash(seed, slot, 0, SALT_EXTRA_STRONGHOLDS)))
                .limit(strongholdCount(settings.corruption()) - 1)
                .forEach(slots::add);
        List<Site> strongholds = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            strongholds.add(siteOnRing(Site.Type.STRONGHOLD, "stronghold_" + i, slotAngle(slots.get(i)),
                    (settings.outskirtsRadius() + settings.outlandsRadius()) / 2));
        }
        allSites.addAll(strongholds);
        allSites.add(siteOnRing(Site.Type.PORTAL, "portal_0", slotAngle(portalSlot), settings.outlandsRadius() - 8));

        this.sites = List.copyOf(allSites);
        this.waypoints = List.copyOf(waypointSites);
        this.terminals = sites.stream().filter(Site::hasTerminal).toList();
        for (Site site : sites) {
            sitesByChunk.put(chunkKey(site.chunkX(), site.chunkZ()), site);
        }
        this.corruption = new CorruptionField(settings, List.copyOf(strongholds));
        this.districtRotation = GridRandom.unit(GridRandom.hash(seed, 0, 0, SALT_DISTRICTS)) * Math.PI * 2;
        int extent = settings.cityRadius() + MultiChunk.SIZE + 2;
        this.xAxis = new StreetAxis(settings.streetStyle(), seed, SALT_STREETS_X, settings.blockSize(), extent);
        this.zAxis = new StreetAxis(settings.streetStyle(), seed, SALT_STREETS_Z, settings.blockSize(), extent);
        this.transit = new TransitLayout(settings, xAxis, zAxis);
    }

    /** One stronghold up to 64% corruption (the default grid has 50%), two up to 84%, three above that. */
    public static int strongholdCount(float corruption) {
        if (corruption >= 0.85f) {
            return 3;
        }
        return corruption >= 0.65f ? 2 : 1;
    }

    // The middle of one of the 8 gaps between waypoint directions
    private static double slotAngle(int slot) {
        return (slot + 0.5) * Math.PI / 4;
    }

    /** The city's levels above and below the streets. */
    public TransitLayout transit() {
        return transit;
    }

    public GridLayoutSettings settings() {
        return settings;
    }

    public List<Site> sites() {
        return sites;
    }

    public Site mainArrival() {
        return sites.getFirst();
    }

    /** Every site players can arrive at: the main terminal first, then the waypoints. */
    public List<Site> terminals() {
        return terminals;
    }

    public Optional<Site> terminal(String id) {
        return terminals.stream().filter(site -> site.id().equals(id)).findFirst();
    }

    public Site nearestTerminal(int chunkX, int chunkZ) {
        Site nearest = mainArrival();
        for (Site terminal : terminals) {
            if (terminal.distanceTo(chunkX, chunkZ) < nearest.distanceTo(chunkX, chunkZ)) {
                nearest = terminal;
            }
        }
        return nearest;
    }

    public Zone zoneAt(int chunkX, int chunkZ) {
        double distance = Math.hypot(chunkX, chunkZ);
        if (distance <= settings.coreRadius()) {
            return Zone.CITY_CORE;
        }
        if (distance <= settings.cityRadius()) {
            return Zone.CITY;
        }
        for (Site waypoint : waypoints) {
            if (waypoint.distanceTo(chunkX, chunkZ) <= SITE_RADIUS) {
                return Zone.WAYPOINT;
            }
        }
        if (distance <= settings.outskirtsRadius()) {
            return Zone.OUTSKIRTS;
        }
        if (distance <= settings.outlandsRadius()) {
            return Zone.OUTLANDS;
        }
        return Zone.UNALLOCATED;
    }

    /** Corruption intensity of a chunk, in [0, 1]; the same value its plan carries. */
    public float corruptionAt(int chunkX, int chunkZ) {
        return corruption.intensityAt(chunkX, chunkZ);
    }

    public ChunkPlan planAt(int chunkX, int chunkZ) {
        Zone zone = zoneAt(chunkX, chunkZ);
        float intensity = corruption.intensityAt(chunkX, chunkZ);
        Plans plans = new Plans(zone, districtAt(chunkX, chunkZ), intensity, CorruptionTier.fromIntensity(intensity));

        Site site = sitesByChunk.get(chunkKey(chunkX, chunkZ));
        if (site != null) {
            return plans.site(site);
        }
        if (isArrivalConnector(chunkX, chunkZ, zone)) {
            return plans.street(false, true, true);
        }
        if (zone != Zone.CITY && zone != Zone.CITY_CORE) {
            ChunkPlan highway = highwayAt(chunkX, chunkZ, plans);
            if (highway != null) {
                return highway;
            }
        }

        long roll = GridRandom.hash(settings.gridSeed(), chunkX, chunkZ, SALT_PLAN);
        return switch (zone) {
            case CITY_CORE, CITY -> {
                StreetAxis.Road roadX = zAxis.roadAt(chunkZ);
                StreetAxis.Road roadZ = xAxis.roadAt(chunkX);
                if (roadX != null || roadZ != null) {
                    yield plans.street(roadX, roadZ);
                }
                MultiChunk multi = multiChunkAt(chunkX, chunkZ);
                if (multi != null) {
                    // Every quarter takes its height and color from the origin so the building is consistent
                    int floors = multi.landmark() ? maxFloors(multi.originX(), multi.originZ()) : cityFloors(multi.originX(), multi.originZ());
                    yield plans.building(floors, districtAt(multi.originX(), multi.originZ()), multi);
                }
                if (zone == Zone.CITY && GridRandom.unit(roll) < 0.08) {
                    yield plans.plaza();
                }
                yield plans.building(cityFloors(chunkX, chunkZ), plans.district(), null);
            }
            case OUTSKIRTS -> GridRandom.unit(roll) < 0.12
                    ? plans.building(1 + floorRoll(chunkX, chunkZ) % 2, plans.district(), null)
                    : plans.open(null);
            case WAYPOINT -> settlementAt(chunkX, chunkZ, roll, plans);
            case OUTLANDS, UNALLOCATED -> plans.open(null);
        };
    }

    /** The avenue at x = 0 carries on past the city edge to the main arrival terminal. */
    private boolean isArrivalConnector(int chunkX, int chunkZ, Zone zone) {
        Site terminal = mainArrival();
        return zone != Zone.CITY && zone != Zone.CITY_CORE
                && chunkX == terminal.chunkX() && chunkZ > 0 && chunkZ < terminal.chunkZ();
    }

    /**
     * Highways run from the city edge to each waypoint. Waypoints sit on the axes or the diagonals: axis highways are
     * ordinary avenue chunks, diagonal ones a {@link DiagonalRoad} across open chunks.
     */
    @Nullable
    private ChunkPlan highwayAt(int chunkX, int chunkZ, Plans plans) {
        double distance = Math.hypot(chunkX, chunkZ);
        for (Site waypoint : waypoints) {
            if (distance >= Math.hypot(waypoint.chunkX(), waypoint.chunkZ())) {
                continue;
            }
            int signX = Integer.signum(waypoint.chunkX());
            int signZ = Integer.signum(waypoint.chunkZ());
            if (signZ == 0 && chunkZ == 0 && Integer.signum(chunkX) == signX) {
                return plans.street(true, false, true);
            }
            if (signX == 0 && chunkX == 0 && Integer.signum(chunkZ) == signZ) {
                return plans.street(false, true, true);
            }
            if (signX != 0 && signZ != 0) {
                // The road band touches the chunks on the diagonal and, at their corners, the ones beside them
                boolean rising = signX == signZ;
                int across = rising ? chunkX - chunkZ : chunkX + chunkZ;
                int along = rising ? chunkX + chunkZ : chunkX - chunkZ;
                boolean onSettlementCross = chunkX == waypoint.chunkX() || chunkZ == waypoint.chunkZ();
                if (Math.abs(across) <= 1 && Integer.signum(along) == signX && !onSettlementCross) {
                    return plans.open(new DiagonalRoad(rising));
                }
            }
        }
        return null;
    }

    /**
     * Roughly how far, in blocks, a block is from the edge of the nearest highway (0 on it), or
     * {@link Double#MAX_VALUE} if no highway runs nearby. Lets the outlands terrain level off beside the roads.
     */
    public double highwayDistance(int blockX, int blockZ) {
        double nearest = Double.MAX_VALUE;
        double distance = Math.hypot(blockX + 0.5, blockZ + 0.5);
        for (Site waypoint : waypoints) {
            double waypointDistance = Math.hypot(waypoint.chunkX() * 16 + 8, waypoint.chunkZ() * 16 + 8);
            int signX = Integer.signum(waypoint.chunkX());
            int signZ = Integer.signum(waypoint.chunkZ());
            if (distance > waypointDistance) {
                continue;
            }
            if (signZ == 0 && Integer.signum(blockX) == signX) {
                // Axis highways fill the chunk row (or column) through the city center: blocks 0 to 15
                nearest = Math.min(nearest, Math.max(0, Math.max(-blockZ, blockZ - 15)));
            } else if (signX == 0 && Integer.signum(blockZ) == signZ) {
                nearest = Math.min(nearest, Math.max(0, Math.max(-blockX, blockX - 15)));
            } else if (signX != 0 && signZ != 0) {
                DiagonalRoad road = new DiagonalRoad(signX == signZ);
                int along = road.rising() ? blockX + blockZ : blockX - blockZ;
                if (Integer.signum(along) == signX) {
                    nearest = Math.min(nearest, Math.max(0, Math.abs(road.offset(blockX, blockZ)) - DiagonalRoad.HALF_WIDTH));
                }
            }
        }
        return nearest;
    }

    /**
     * A waypoint is a small settlement: a street cross through its terminal, and low buildings and plazas around it.
     */
    private ChunkPlan settlementAt(int chunkX, int chunkZ, long roll, Plans plans) {
        Site waypoint = waypoints.stream().min(Comparator.comparingDouble(site -> site.distanceTo(chunkX, chunkZ))).orElseThrow();
        int dx = chunkX - waypoint.chunkX();
        int dz = chunkZ - waypoint.chunkZ();
        if (dx == 0 || dz == 0) {
            return plans.street(dz == 0, dx == 0, false);
        }
        return GridRandom.unit(roll) < 0.45
                ? plans.building(1 + floorRoll(chunkX, chunkZ) % 3, plans.district(), null)
                : plans.plaza();
    }

    // What every plan of one chunk shares, so each kind of plan reads as one short call
    private record Plans(Zone zone, DyeColor district, float corruption, CorruptionTier tier) {
        ChunkPlan site(Site site) {
            return new ChunkPlan(zone, PlanType.SITE, 0, district, null, null, site, null, null, corruption, tier);
        }

        ChunkPlan street(@Nullable StreetAxis.Road roadX, @Nullable StreetAxis.Road roadZ) {
            return new ChunkPlan(zone, PlanType.STREET, 0, district, roadX, roadZ, null, null, null, corruption, tier);
        }

        // An ordinary one-chunk street outside the city's street grid (highways, connectors, settlements)
        ChunkPlan street(boolean alongX, boolean alongZ, boolean central) {
            StreetAxis.Road road = new StreetAxis.Road(StreetAxis.RoadClass.STREET, 0, central);
            return street(alongX ? road : null, alongZ ? road : null);
        }

        ChunkPlan building(int floors, DyeColor buildingDistrict, @Nullable MultiChunk multi) {
            return new ChunkPlan(zone, PlanType.BUILDING, floors, buildingDistrict, null, null, null, multi, null, corruption, tier);
        }

        ChunkPlan plaza() {
            return new ChunkPlan(zone, PlanType.PLAZA, 0, district, null, null, null, null, null, corruption, tier);
        }

        ChunkPlan open(@Nullable DiagonalRoad diagonal) {
            return new ChunkPlan(zone, PlanType.OPEN, 0, district, null, null, null, null, diagonal, corruption, tier);
        }
    }

    /**
     * City blocks (the chunks between streets) may hold one 2×2-chunk building. The block nearest the
     * city center always does: that's the landmark tower. Groups that would spill out of the city are dropped.
     */
    @Nullable
    private MultiChunk multiChunkAt(int chunkX, int chunkZ) {
        StreetAxis.Block blockX = xAxis.blockAt(chunkX);
        StreetAxis.Block blockZ = zAxis.blockAt(chunkZ);
        if (blockX.size() < MultiChunk.SIZE || blockZ.size() < MultiChunk.SIZE) {
            return null;
        }
        long seed = settings.gridSeed();

        // Block 0 on both axes starts at chunk (1, 1), just past the roads through the center
        boolean landmark = blockX.index() == 0 && blockZ.index() == 0;
        int offsetX = 0;
        int offsetZ = 0;
        if (!landmark) {
            double chance = zoneAt(blockX.start(), blockZ.start()) == Zone.CITY_CORE ? 0.5 : 0.25;
            if (GridRandom.unit(GridRandom.hash(seed, blockX.index(), blockZ.index(), SALT_MULTI)) >= chance) {
                return null;
            }
            offsetX = GridRandom.nextInt(GridRandom.hash(seed, blockX.index(), blockZ.index(), SALT_MULTI_OFFSET_X),
                    blockX.size() - MultiChunk.SIZE + 1);
            offsetZ = GridRandom.nextInt(GridRandom.hash(seed, blockX.index(), blockZ.index(), SALT_MULTI_OFFSET_Z),
                    blockZ.size() - MultiChunk.SIZE + 1);
        }

        int originX = blockX.start() + offsetX;
        int originZ = blockZ.start() + offsetZ;
        if (chunkX < originX || chunkX >= originX + MultiChunk.SIZE || chunkZ < originZ || chunkZ >= originZ + MultiChunk.SIZE) {
            return null;
        }
        for (int dx = 0; dx < MultiChunk.SIZE; dx++) {
            for (int dz = 0; dz < MultiChunk.SIZE; dz++) {
                Zone zone = zoneAt(originX + dx, originZ + dz);
                if (zone != Zone.CITY && zone != Zone.CITY_CORE) {
                    return null;
                }
            }
        }
        return new MultiChunk(originX, originZ, landmark);
    }

    private int cityFloors(int chunkX, int chunkZ) {
        int maxFloors = maxFloors(chunkX, chunkZ);
        return Math.max(1, maxFloors / 2 + floorRoll(chunkX, chunkZ) % (maxFloors - maxFloors / 2 + 1));
    }

    private int floorRoll(int chunkX, int chunkZ) {
        return GridRandom.nextInt(GridRandom.hash(settings.gridSeed(), chunkX, chunkZ, SALT_FLOORS), 1 << 16);
    }

    /** Tallest building allowed at this chunk: up to 24 floors in the core, tapering to 3 at the city edge. */
    private int maxFloors(int chunkX, int chunkZ) {
        double distance = Math.hypot(chunkX, chunkZ);
        if (distance <= settings.coreRadius()) {
            return 24;
        }
        double t = (distance - settings.coreRadius()) / (settings.cityRadius() - settings.coreRadius());
        return (int) Math.round(12 + (3 - 12) * Math.clamp(t, 0.0, 1.0));
    }

    private DyeColor districtAt(int chunkX, int chunkZ) {
        List<DyeColor> colors = settings.districtColors();
        double angle = Math.atan2(chunkZ, chunkX) + districtRotation;
        double turns = angle / (Math.PI * 2);
        int index = (int) Math.floor((turns - Math.floor(turns)) * colors.size());
        return colors.get(Math.min(index, colors.size() - 1));
    }

    private static Site siteOnRing(Site.Type type, String id, double angle, int radius) {
        return new Site(type, id, (int) Math.round(radius * Math.cos(angle)), (int) Math.round(radius * Math.sin(angle)));
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
