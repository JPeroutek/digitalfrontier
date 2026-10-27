package com.partatoes.digitalfrontier.datagen.grid;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny builder for square structure templates, used to generate the starter grid parts in code.
 * Later calls overwrite earlier ones, so shapes can be layered (walls, then windows cut into them).
 */
final class StarterPart {
    private final int width;
    private final int height;
    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();

    StarterPart(int width, int height) {
        this.width = width;
        this.height = height;
    }

    StarterPart box(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    blocks.put(new BlockPos(x, y, z), state);
                }
            }
        }
        return this;
    }

    /** A solid square layer, {@code inset} blocks in from the edges. */
    StarterPart plate(int y, int inset, BlockState state) {
        int max = width - 1 - inset;
        return box(inset, y, inset, max, y, max, state);
    }

    /** The outline of a square, {@code inset} blocks in from the edges, from y0 to y1. */
    StarterPart ring(int y0, int y1, int inset, BlockState state) {
        int max = width - 1 - inset;
        box(inset, y0, inset, max, y1, inset, state);
        box(inset, y0, max, max, y1, max, state);
        box(inset, y0, inset, inset, y1, max, state);
        return box(max, y0, inset, max, y1, max, state);
    }

    /** A centered opening of {@code openingWidth} blocks in each of the four walls of a ring. */
    StarterPart openings(int y0, int y1, int inset, int openingWidth, BlockState fill) {
        int start = (width - openingWidth) / 2;
        int end = start + openingWidth - 1;
        int max = width - 1 - inset;
        box(start, y0, inset, end, y1, inset, fill);
        box(start, y0, max, end, y1, max, fill);
        box(inset, y0, start, inset, y1, end, fill);
        return box(max, y0, start, max, y1, end, fill);
    }

    /** Vanilla structure template format, as a structure block would save it. */
    NbtCompound toNbt() {
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> paletteIndex = new LinkedHashMap<>();
        NbtList blockList = new NbtList();
        blocks.forEach((pos, state) -> {
            int index = paletteIndex.computeIfAbsent(state, newState -> {
                palette.add(newState);
                return palette.size() - 1;
            });
            NbtCompound block = new NbtCompound();
            block.put("pos", intList(pos.getX(), pos.getY(), pos.getZ()));
            block.putInt("state", index);
            blockList.add(block);
        });

        NbtList paletteList = new NbtList();
        palette.forEach(state -> paletteList.add(NbtHelper.fromBlockState(state)));

        NbtCompound root = new NbtCompound();
        root.put("size", intList(width, height, width));
        root.put("palette", paletteList);
        root.put("blocks", blockList);
        root.put("entities", new NbtList());
        return NbtHelper.putDataVersion(root);
    }

    private static NbtList intList(int... values) {
        NbtList list = new NbtList();
        for (int value : values) {
            list.add(NbtInt.of(value));
        }
        return list;
    }
}
