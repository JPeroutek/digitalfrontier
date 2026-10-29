package com.partatoes.digitalfrontier.grid.stronghold;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An item or block in trigger JSON: an id ({@code "minecraft:echo_shard"}) or a tag ({@code "#minecraft:logs"}).
 * Unknown ids simply never match, so a datapack can name items from mods that aren't installed.
 */
public record RegistryMatcher(boolean tag, Identifier id) {
    public static final Codec<RegistryMatcher> CODEC = Codec.STRING.comapFlatMap(RegistryMatcher::parse, RegistryMatcher::toString);

    private static DataResult<RegistryMatcher> parse(String text) {
        boolean tag = text.startsWith("#");
        return Identifier.read(tag ? text.substring(1) : text).map(id -> new RegistryMatcher(tag, id));
    }

    public boolean matches(ItemStack stack) {
        if (tag) {
            return stack.is(TagKey.create(Registries.ITEM, id));
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return stack.is(item) && BuiltInRegistries.ITEM.getKey(item).equals(id);
    }

    public boolean matches(BlockState state) {
        if (tag) {
            return state.is(TagKey.create(Registries.BLOCK, id));
        }
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        return state.is(block) && BuiltInRegistries.BLOCK.getKey(block).equals(id);
    }

    @Override
    public String toString() {
        return (tag ? "#" : "") + id;
    }
}
