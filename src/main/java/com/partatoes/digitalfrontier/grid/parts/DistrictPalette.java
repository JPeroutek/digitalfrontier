package com.partatoes.digitalfrontier.grid.parts;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Recolors white blocks to a district's color. Any block whose id contains "white" is swapped for the block
 * with that word replaced by the color, if one exists: white pattern gridstone becomes the district's pattern
 * gridstone, white stained glass becomes that color's glass, and so on. Block state properties are kept.
 */
public final class DistrictPalette {
    private static final Map<Block, Map<DyeColor, Block>> VARIANTS = new ConcurrentHashMap<>();

    private DistrictPalette() {
    }

    public static BlockState remap(BlockState state, DyeColor color) {
        if (color == DyeColor.WHITE) {
            return state;
        }
        Block target = VARIANTS.computeIfAbsent(state.getBlock(), DistrictPalette::findVariants).get(color);
        return target == null ? state : copyProperties(state, target.defaultBlockState());
    }

    private static Map<DyeColor, Block> findVariants(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (!id.getPath().contains("white")) {
            return Map.of();
        }
        Map<DyeColor, Block> variants = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            Identifier variantId = id.withPath(path -> path.replace("white", color.getSerializedName()));
            BuiltInRegistries.BLOCK.getOptional(variantId).ifPresent(variant -> variants.put(color, variant));
        }
        return variants;
    }

    private static BlockState copyProperties(BlockState from, BlockState to) {
        for (Property<?> property : from.getProperties()) {
            if (to.hasProperty(property)) {
                to = copyProperty(from, to, property);
            }
        }
        return to;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }
}
