package com.partatoes.digitalfrontier.grid.stronghold;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * An item or block in trigger JSON: an id ({@code "minecraft:echo_shard"}) or a tag ({@code "#minecraft:logs"}).
 * Unknown ids simply never match, so a datapack can name items from mods that aren't installed.
 */
public record RegistryMatcher(boolean tag, Identifier id) {
    public static final Codec<RegistryMatcher> CODEC = Codec.STRING.comapFlatMap(RegistryMatcher::parse, RegistryMatcher::toString);

    private static DataResult<RegistryMatcher> parse(String text) {
        boolean tag = text.startsWith("#");
        return Identifier.validate(tag ? text.substring(1) : text).map(id -> new RegistryMatcher(tag, id));
    }

    public boolean matches(ItemStack stack) {
        if (tag) {
            return stack.isIn(TagKey.of(RegistryKeys.ITEM, id));
        }
        Item item = Registries.ITEM.get(id);
        return stack.isOf(item) && Registries.ITEM.getId(item).equals(id);
    }

    public boolean matches(BlockState state) {
        if (tag) {
            return state.isIn(TagKey.of(RegistryKeys.BLOCK, id));
        }
        Block block = Registries.BLOCK.get(id);
        return state.isOf(block) && Registries.BLOCK.getId(block).equals(id);
    }

    @Override
    public String toString() {
        return (tag ? "#" : "") + id;
    }
}
