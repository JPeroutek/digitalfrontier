package com.partatoes.digitalfrontier.grid.layout;

import net.minecraft.util.DyeColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final GridLayoutSettings settings;
    private final List<Site> sites;
    private final List<Site> waypoints;
    private final Map<Long, Site> sitesByChunk = new HashMap<>();
    private final CorruptionField corruption;
    private final double districtRotation;

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

        // Strongholds and portals sit between the 8 waypoint directions, so they never collide with a waypoint
        double strongholdAngle = (GridRandom.nextInt(GridRandom.hash(seed, 0, 0, SALT_STRONGHOLD), 8) + 0.5) * Math.PI / 4;
        Site stronghold = siteOnRing(Site.Type.STRONGHOLD, "stronghold_0", strongholdAngle,
                (settings.outskirtsRadius() + settings.outlandsRadius()) / 2);
        allSites.add(stronghold);
        allSites.add(siteOnRing(Site.Type.PORTAL, "portal_0", strongholdAngle + Math.PI, settings.outlandsRadius() - 8));

        this.sites = List.copyOf(allSites);
        this.waypoints = List.copyOf(waypointSites);
        for (Site site : sites) {
            sitesByChunk.put(chunkKey(site.chunkX(), site.chunkZ()), site);
        }
        this.corruption = new CorruptionField(settings, List.of(stronghold));
        this.districtRotation = GridRandom.unit(GridRandom.hash(seed, 0, 0, SALT_DISTRICTS)) * Math.PI * 2;
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

    public ChunkPlan planAt(int chunkX, int chunkZ) {
        Zone zone = zoneAt(chunkX, chunkZ);
        float intensity = corruption.intensityAt(chunkX, chunkZ);
        CorruptionTier tier = CorruptionTier.fromIntensity(intensity);
        DyeColor district = districtAt(chunkX, chunkZ);

        Site site = sitesByChunk.get(chunkKey(chunkX, chunkZ));
        if (site != null) {
            return new ChunkPlan(zone, PlanType.SITE, 0, district, false, false, false, site, intensity, tier);
        }

        long roll = GridRandom.hash(settings.gridSeed(), chunkX, chunkZ, SALT_PLAN);
        int floorRoll = GridRandom.nextInt(GridRandom.hash(settings.gridSeed(), chunkX, chunkZ, SALT_FLOORS), 1 << 16);
        return switch (zone) {
            case CITY_CORE, CITY -> {
                int period = settings.blockSize() + 1;
                boolean streetX = Math.floorMod(chunkZ, period) == 0;
                boolean streetZ = Math.floorMod(chunkX, period) == 0;
                if (streetX || streetZ) {
                    boolean avenue = (streetX && chunkZ == 0) || (streetZ && chunkX == 0);
                    yield new ChunkPlan(zone, PlanType.STREET, 0, district, streetX, streetZ, avenue, null, intensity, tier);
                }
                if (zone == Zone.CITY && GridRandom.unit(roll) < 0.08) {
                    yield new ChunkPlan(zone, PlanType.PLAZA, 0, district, false, false, false, null, intensity, tier);
                }
                int maxFloors = maxFloors(chunkX, chunkZ);
                int floors = maxFloors / 2 + floorRoll % (maxFloors - maxFloors / 2 + 1);
                yield new ChunkPlan(zone, PlanType.BUILDING, Math.max(1, floors), district, false, false, false, null, intensity, tier);
            }
            case OUTSKIRTS -> GridRandom.unit(roll) < 0.12
                    ? new ChunkPlan(zone, PlanType.BUILDING, 1 + floorRoll % 2, district, false, false, false, null, intensity, tier)
                    : new ChunkPlan(zone, PlanType.OPEN, 0, district, false, false, false, null, intensity, tier);
            case WAYPOINT -> new ChunkPlan(zone, PlanType.PLAZA, 0, district, false, false, false, null, intensity, tier);
            case OUTLANDS, UNALLOCATED -> new ChunkPlan(zone, PlanType.OPEN, 0, district, false, false, false, null, intensity, tier);
        };
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
