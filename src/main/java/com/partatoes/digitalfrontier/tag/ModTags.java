package com.partatoes.digitalfrontier.tag;

import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTags {
    public static class Blocks {

        private static TagKey<Block> createTag(String name) {
            return TagKey.of(RegistryKeys.BLOCK, new Identifier(DigitalFrontier.MOD_ID, name));
        }

    }

    public static class Items {
        public static final TagKey<Item>BIT = createTag("bit");
        private static TagKey<Item> createTag(String name) {
            return TagKey.of(RegistryKeys.ITEM, new Identifier(DigitalFrontier.MOD_ID, name));
        }

    }
}
