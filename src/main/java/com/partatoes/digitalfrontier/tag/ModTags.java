package com.partatoes.digitalfrontier.tag;

import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> GRID_BLOCK = createTag("grid_block");

        private static TagKey<Block> createTag(String name) {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, name));
        }

    }

    public static class Items {
        public static final TagKey<Item> BIT = createTag("bit");
        private static TagKey<Item> createTag(String name) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, name));
        }

    }
}
