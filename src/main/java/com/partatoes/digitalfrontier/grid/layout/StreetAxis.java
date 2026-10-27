package com.partatoes.digitalfrontier.grid.layout;

import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

/**
 * The city's street pattern along one axis: which chunk rows (or columns) are roads, how wide each road is, and
 * where the city blocks between them start and end. Combining the X and Z axes gives the street grid.
 *
 * <ul>
 *   <li>{@link StreetStyle#UNIFORM}: every road is a one-chunk street every {@code blockSize + 1} chunks, the
 *       original layout.</li>
 *   <li>{@link StreetStyle#VARIED}: a seeded sequence of lanes, streets and two-chunk boulevards, with city blocks
 *       of varying size between them. The road through the city center is a boulevard whose median runs along the
 *       axis itself (between chunks -1 and 0).</li>
 * </ul>
 *
 * Positions are precomputed out to just past the city edge; further out every chunk counts as part of a block.
 */
public final class StreetAxis {
    public enum StreetStyle implements StringIdentifiable {
        UNIFORM("uniform"),
        VARIED("varied");

        public static final Codec<StreetStyle> CODEC = StringIdentifiable.createCodec(StreetStyle::values);

        private final String name;

        StreetStyle(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
    }

    /** How wide a road is. Lanes and streets are one chunk; boulevards two. */
    public enum RoadClass implements StringIdentifiable {
        /** A narrow lane down the middle of the chunk, between strips of walls and planters. */
        LANE("lane", 1),
        STREET("street", 1),
        /** Two chunks wide, with a lit median where its halves meet. */
        BOULEVARD("boulevard", 2);

        public static final Codec<RoadClass> CODEC = StringIdentifiable.createCodec(RoadClass::values);

        private final String name;
        private final int chunks;

        RoadClass(String name, int chunks) {
            this.name = name;
            this.chunks = chunks;
        }

        public int chunks() {
            return chunks;
        }

        @Override
        public String asString() {
            return name;
        }
    }

    /**
     * A road crossing one chunk.
     *
     * @param half    which of a boulevard's two chunks this is: 0 for the one at lower coordinates, 1 for the other
     * @param central the road through the city center (the old "avenue")
     */
    public record Road(RoadClass roadClass, int half, boolean central) {
    }

    /** The city block a chunk is in, along this axis: the block's index, first chunk and size in chunks. */
    public record Block(int index, int start, int size) {
    }

    private static final double BOULEVARD_CHANCE = 0.12;
    private static final double LANE_CHANCE = 0.3;
    private static final int SALT_ROAD = 0;
    private static final int SALT_BLOCK = 1;
    private static final int SALT_AXIS = 0x5A;
    private static final Road CENTRAL_HALF_0 = new Road(RoadClass.BOULEVARD, 0, true);
    private static final Road CENTRAL_HALF_1 = new Road(RoadClass.BOULEVARD, 1, true);

    private final StreetStyle style;
    private final int blockSize;
    private final int extent;
    // Indexed by chunk coordinate + extent
    private final Road[] roads;
    private final Block[] blocks;

    StreetAxis(StreetStyle style, long seed, int salt, int blockSize, int extent) {
        this.style = style;
        this.blockSize = blockSize;
        this.extent = extent;
        this.roads = new Road[2 * extent + 1];
        this.blocks = new Block[2 * extent + 1];
        if (style == StreetStyle.VARIED) {
            layOutVaried(seed, salt);
        }
    }

    // From the central boulevard outward in both directions: block, road, block, road...
    private void layOutVaried(long seed, int salt) {
        // Each axis gets its own fully mixed seed: hashing small block indices straight against the grid seed and a
        // small salt gave visibly correlated rolls (one axis came out nearly all streets)
        long axisSeed = GridRandom.hash(seed, salt, 0, SALT_AXIS);
        set(-1, CENTRAL_HALF_0);
        set(0, CENTRAL_HALF_1);
        layOutSide(axisSeed, 1, 1);
        layOutSide(axisSeed, -2, -1);
    }

    private void layOutSide(long axisSeed, int first, int direction) {
        int minBlock = Math.max(2, blockSize - 1);
        int maxBlock = blockSize + 2;
        int c = first;
        for (int index = 0; Math.abs(c) <= extent; index++) {
            int blockIndex = direction > 0 ? index : -1 - index;
            long blockHash = GridRandom.hash(axisSeed, blockIndex, 0, SALT_BLOCK);
            int size = minBlock + GridRandom.nextInt(blockHash, maxBlock - minBlock + 1);
            int start = direction > 0 ? c : c - size + 1;
            Block block = new Block(blockIndex, start, size);
            for (int i = 0; i < size; i++) {
                setBlock(c, block);
                c += direction;
            }

            double roll = GridRandom.unit(GridRandom.hash(axisSeed, blockIndex, 0, SALT_ROAD));
            if (roll < BOULEVARD_CHANCE) {
                // The half at lower coordinates is half 0, whichever way the sequence is being laid out
                set(c, new Road(RoadClass.BOULEVARD, direction > 0 ? 0 : 1, false));
                set(c + direction, new Road(RoadClass.BOULEVARD, direction > 0 ? 1 : 0, false));
                c += 2 * direction;
            } else {
                set(c, new Road(roll < BOULEVARD_CHANCE + LANE_CHANCE ? RoadClass.LANE : RoadClass.STREET, 0, false));
                c += direction;
            }
        }
    }

    private void set(int c, Road road) {
        if (Math.abs(c) <= extent) {
            roads[c + extent] = road;
        }
    }

    private void setBlock(int c, Block block) {
        if (Math.abs(c) <= extent) {
            blocks[c + extent] = block;
        }
    }

    /** The road along this chunk row (or column), or null if it's part of a city block. */
    @Nullable
    public Road roadAt(int c) {
        if (style == StreetStyle.UNIFORM) {
            if (Math.floorMod(c, blockSize + 1) != 0) {
                return null;
            }
            return new Road(RoadClass.STREET, 0, c == 0);
        }
        return Math.abs(c) <= extent ? roads[c + extent] : null;
    }

    /** The block this chunk is in; only meaningful where {@link #roadAt} is null. */
    public Block blockAt(int c) {
        if (style == StreetStyle.UNIFORM || Math.abs(c) > extent || blocks[c + extent] == null) {
            int period = blockSize + 1;
            int index = Math.floorDiv(c, period);
            return new Block(index, index * period + 1, blockSize);
        }
        return blocks[c + extent];
    }
}
