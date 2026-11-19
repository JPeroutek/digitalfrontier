package com.partatoes.digitalfrontier.grid.layout;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.partatoes.digitalfrontier.grid.layout.GridLayout.FLOOR_Y;

/**
 * The grid's arena, at its {@link Site.Type#ARENA} site in the outskirts: a sunken square floor, stepped stands all
 * round that double as the way down, an outer wall with a gate on the north, and a goal ring at each end. The middle
 * of the floor is a field of tiles over a pit, for the disc duel. It's code-built like {@link CoreChamber} and wins
 * over whatever else would be there, so every grid's arena is the same. Pure: it says what kind of block goes where,
 * and the chunk builder picks the blocks.
 */
public final class ArenaGeometry {
    /** Half the play floor's width: it spans this many blocks each way from the center. */
    public static final int PLAY = 15;
    /** Half the tile field's width, in the floor's middle. */
    public static final int TILES = 6;
    /** The stands run from the floor's edge to here; the outer wall is the ring beyond. */
    public static final int STANDS = 22;
    public static final int WALL = STANDS + 1;
    /** The floor's surface block; players stand on the layer above. */
    public static final int FLOOR = FLOOR_Y - 6;
    /** The pit under the tiles bottoms out here. */
    public static final int PIT_BOTTOM = FLOOR - 8;
    // The ground the arena is dug into, below the floor
    private static final int BASE = PIT_BOTTOM - 2;
    private static final int WALL_TOP = FLOOR_Y + 4;
    // Everything above the arena is cleared to here
    private static final int SKY = FLOOR_Y + 64;
    /** The goal rings: at each end of the floor, centered this high, this wide. */
    public static final int RING_Z = 14;
    public static final int RING_Y = FLOOR + 5;
    public static final int RING_RADIUS = 2;
    private static final int LINE_EVERY = 5;

    /** What goes in a block of the arena. */
    public enum Part {
        AIR, GROUND, FLOOR, LINE, TILE, PIT_BOTTOM, STAND, WALL, WALL_LIGHT, RING, CONSOLE
    }

    private final int centerX;
    private final int centerZ;

    public ArenaGeometry(Site site) {
        this.centerX = site.chunkX() * 16 + 8;
        this.centerZ = site.chunkZ() * 16 + 8;
    }

    public int centerX() {
        return centerX;
    }

    public int centerZ() {
        return centerZ;
    }

    /** Whether the arena reaches into a chunk. */
    public boolean touches(int chunkX, int chunkZ) {
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        return minX <= centerX + WALL && minX + 15 >= centerX - WALL && minZ <= centerZ + WALL && minZ + 15 >= centerZ - WALL;
    }

    /** Whether a column is inside the arena's outer wall (stands included). */
    public boolean inside(int x, int z) {
        return ring(x, z) <= WALL;
    }

    /** Whether a column is on the play floor. */
    public boolean onFloor(int x, int z) {
        return ring(x, z) <= PLAY;
    }

    /** Whether a column is in the tile field over the pit. */
    public boolean inTileField(int x, int z) {
        return ring(x, z) <= TILES;
    }

    // How many blocks out from the center a column is, on the square rings the arena is built in
    private int ring(int x, int z) {
        return Math.max(Math.abs(x - centerX), Math.abs(z - centerZ));
    }

    /** What goes at a block, or null where the arena leaves it to the grid. */
    @Nullable
    public Part partAt(int x, int y, int z) {
        int dx = x - centerX;
        int dz = z - centerZ;
        int ring = Math.max(Math.abs(dx), Math.abs(dz));
        if (ring > WALL || y < BASE) {
            return null;
        }
        boolean gate = dz < 0 && Math.abs(dx) <= 1 && ring > STANDS - 2;
        if (ring == WALL) {
            if (gate && y > FLOOR_Y) {
                return y > FLOOR_Y + 3 ? Part.WALL : Part.AIR;
            }
            if (y > WALL_TOP) {
                return y > SKY ? null : Part.AIR;
            }
            return y == WALL_TOP ? Part.WALL_LIGHT : Part.WALL;
        }
        if (ring > PLAY) {
            // Stands: a step up each ring out, from just above the floor to street height; the gate runs level
            int top = gate ? FLOOR_Y : FLOOR + (ring - PLAY);
            if (y <= top) {
                return Part.STAND;
            }
            if (consoleAt(x, y, z)) {
                return Part.CONSOLE;
            }
            return y > SKY ? null : Part.AIR;
        }
        if (y > FLOOR) {
            if (isRing(dx, y, dz)) {
                return Part.RING;
            }
            return y > SKY ? null : Part.AIR;
        }
        if (ring <= TILES) {
            if (y == FLOOR) {
                return Part.TILE;
            }
            if (y > PIT_BOTTOM) {
                return Part.AIR;
            }
            return y == PIT_BOTTOM ? Part.PIT_BOTTOM : Part.GROUND;
        }
        if (y == FLOOR) {
            return dx % LINE_EVERY == 0 || dz % LINE_EVERY == 0 ? Part.LINE : Part.FLOOR;
        }
        return Part.GROUND;
    }

    // The goal rings: an upright circle across the floor at each end, facing down the arena
    private static boolean isRing(int dx, int y, int dz) {
        if (Math.abs(dz) != RING_Z) {
            return false;
        }
        double distance = Math.sqrt(dx * dx + (double) (y - RING_Y) * (y - RING_Y));
        return Math.abs(distance - RING_RADIUS) < 0.5;
    }

    private boolean consoleAt(int x, int y, int z) {
        BlockPos console = console();
        return x == console.getX() && y == console.getY() && z == console.getZ();
    }

    /** The Arena Console, on the stands just inside the gate. */
    public BlockPos console() {
        int dz = -(STANDS - 1);
        return new BlockPos(centerX + 3, FLOOR + (STANDS - 1 - PLAY) + 1, centerZ + dz);
    }

    /** Where the challenger starts, on the floor, and where their opponent does. */
    public BlockPos challengerStart() {
        return new BlockPos(centerX, FLOOR + 1, centerZ - 10);
    }

    public BlockPos opponentStart() {
        return new BlockPos(centerX, FLOOR + 1, centerZ + 10);
    }

    /** Where a player who loses (or leaves) a round is put: up in the stands by the gate. */
    public BlockPos stands() {
        int ring = STANDS - 2;
        return new BlockPos(centerX, FLOOR + (ring - PLAY) + 1, centerZ - ring);
    }

    /** The center of the goal ring at one end: north (the challenger's own) or south. */
    public BlockPos ring(boolean north) {
        return new BlockPos(centerX, RING_Y, centerZ + (north ? -RING_Z : RING_Z));
    }

    /** Where trophies stand: along the top of the south stands, one per title won. */
    public List<BlockPos> trophySpots() {
        List<BlockPos> spots = new ArrayList<>();
        for (int dx = -8; dx <= 8; dx += 2) {
            spots.add(new BlockPos(centerX + dx, FLOOR + (STANDS - PLAY) + 1, centerZ + STANDS));
        }
        return spots;
    }

    /** The tile field's tiles, from the outside in (the order they fall in a disc duel). */
    public List<BlockPos> tilesOutsideIn() {
        List<BlockPos> tiles = new ArrayList<>();
        for (int ring = TILES; ring >= 0; ring--) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                        tiles.add(new BlockPos(centerX + dx, FLOOR, centerZ + dz));
                    }
                }
            }
        }
        return tiles;
    }
}
