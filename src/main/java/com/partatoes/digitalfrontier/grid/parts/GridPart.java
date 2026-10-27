package com.partatoes.digitalfrontier.grid.parts;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.datafixer.Schemas;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * A block template loaded from a vanilla structure (.nbt) file, flattened into a palette and an index array
 * so worldgen threads can read it cheaply. Positions with no block (structure void) are "void": generation
 * leaves whatever is already there.
 */
public final class GridPart {
    private static final short VOID = -1;

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final BlockState[] palette;
    private final short[] blocks;

    private GridPart(int sizeX, int sizeY, int sizeZ, BlockState[] palette, short[] blocks) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.palette = palette;
        this.blocks = blocks;
    }

    public static GridPart fromNbt(NbtCompound nbt) {
        // Upgrade templates saved by older Minecraft versions, as the vanilla structure loader does
        NbtCompound fixed = DataFixTypes.STRUCTURE.update(Schemas.getFixer(), nbt, NbtHelper.getDataVersion(nbt, 500));

        NbtList size = fixed.getList("size", NbtElement.INT_TYPE);
        int sizeX = size.getInt(0);
        int sizeY = size.getInt(1);
        int sizeZ = size.getInt(2);

        // Templates saved with several palettes (rare; e.g. shipwrecks) use the first one
        NbtList paletteNbt = fixed.contains("palettes", NbtElement.LIST_TYPE)
                ? fixed.getList("palettes", NbtElement.LIST_TYPE).getList(0)
                : fixed.getList("palette", NbtElement.COMPOUND_TYPE);
        BlockState[] palette = new BlockState[paletteNbt.size()];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = NbtHelper.toBlockState(Registries.BLOCK, paletteNbt.getCompound(i));
        }

        short[] blocks = new short[sizeX * sizeY * sizeZ];
        Arrays.fill(blocks, VOID);
        NbtList blockList = fixed.getList("blocks", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < blockList.size(); i++) {
            NbtCompound block = blockList.getCompound(i);
            int stateIndex = block.getInt("state");
            if (palette[stateIndex].isOf(Blocks.STRUCTURE_VOID)) {
                continue;
            }
            NbtList pos = block.getList("pos", NbtElement.INT_TYPE);
            blocks[index(pos.getInt(0), pos.getInt(1), pos.getInt(2), sizeX, sizeZ)] = (short) stateIndex;
        }
        return new GridPart(sizeX, sizeY, sizeZ, palette, blocks);
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    /** The block at a template position, or null if the template leaves it void. */
    @Nullable
    public BlockState get(int x, int y, int z) {
        short stateIndex = blocks[index(x, y, z, sizeX, sizeZ)];
        return stateIndex == VOID ? null : palette[stateIndex];
    }

    private static int index(int x, int y, int z, int sizeX, int sizeZ) {
        return (y * sizeZ + z) * sizeX + x;
    }
}
