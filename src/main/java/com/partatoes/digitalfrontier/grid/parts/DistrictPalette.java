package com.partatoes.digitalfrontier.grid.parts;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        return target == null ? state : copyProperties(state, target.getDefaultState());
    }

    private static Map<DyeColor, Block> findVariants(Block block) {
        Identifier id = Registries.BLOCK.getId(block);
        if (!id.getPath().contains("white")) {
            return Map.of();
        }
        Map<DyeColor, Block> variants = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            Identifier variantId = id.withPath(path -> path.replace("white", color.asString()));
            Registries.BLOCK.getOptionalValue(variantId).ifPresent(variant -> variants.put(color, variant));
        }
        return variants;
    }

    private static BlockState copyProperties(BlockState from, BlockState to) {
        for (Property<?> property : from.getProperties()) {
            if (to.contains(property)) {
                to = copyProperty(from, to, property);
            }
        }
        return to;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.with(property, from.get(property));
    }
}
