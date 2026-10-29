package com.partatoes.digitalfrontier.grid.parts;

import com.partatoes.digitalfrontier.block.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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
    @Nullable
    private final BlockPos arrivalPad;

    private GridPart(int sizeX, int sizeY, int sizeZ, BlockState[] palette, short[] blocks, @Nullable BlockPos arrivalPad) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.palette = palette;
        this.blocks = blocks;
        this.arrivalPad = arrivalPad;
    }

    public static GridPart fromNbt(CompoundTag nbt) {
        // Upgrade templates saved by older Minecraft versions, as the vanilla structure loader does
        CompoundTag fixed = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), nbt, NbtUtils.getDataVersion(nbt, 500));

        ListTag size = fixed.getListOrEmpty("size");
        int sizeX = size.getIntOr(0, 0);
        int sizeY = size.getIntOr(1, 0);
        int sizeZ = size.getIntOr(2, 0);

        // Templates saved with several palettes (rare; e.g. shipwrecks) use the first one
        ListTag paletteNbt = fixed.contains("palettes")
                ? fixed.getListOrEmpty("palettes").getListOrEmpty(0)
                : fixed.getListOrEmpty("palette");
        BlockState[] palette = new BlockState[paletteNbt.size()];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, paletteNbt.getCompoundOrEmpty(i));
        }

        short[] blocks = new short[sizeX * sizeY * sizeZ];
        Arrays.fill(blocks, VOID);
        BlockPos arrivalPad = null;
        ListTag blockList = fixed.getListOrEmpty("blocks");
        for (int i = 0; i < blockList.size(); i++) {
            CompoundTag block = blockList.getCompoundOrEmpty(i);
            int stateIndex = block.getIntOr("state", 0);
            if (palette[stateIndex].is(Blocks.STRUCTURE_VOID)) {
                continue;
            }
            ListTag pos = block.getListOrEmpty("pos");
            blocks[index(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0), sizeX, sizeZ)] = (short) stateIndex;
            if (arrivalPad == null && palette[stateIndex].is(ModBlocks.ARRIVAL_PAD)) {
                arrivalPad = new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0));
            }
        }
        return new GridPart(sizeX, sizeY, sizeZ, palette, blocks, arrivalPad);
    }

    /** Template position of the first Arrival Pad in this part, if it has one. */
    @Nullable
    public BlockPos arrivalPad() {
        return arrivalPad;
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
