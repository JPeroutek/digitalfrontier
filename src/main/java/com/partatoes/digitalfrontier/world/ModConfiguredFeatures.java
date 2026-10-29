package com.partatoes.digitalfrontier.world;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

public class ModConfiguredFeatures {
    public static final ResourceKey<Feature> LUMINANCE_ORE_KEY = registerKey("luminance_ore");


    public static void bootstrap(BootstrapContext<Feature> context) {
        RuleTest stoneReplacables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplacables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest netherReplacables = new TagMatchTest(BlockTags.BASE_STONE_NETHER);
        RuleTest endReplacables = new BlockMatchTest(Blocks.END_STONE);

        List<BlockReplacement> overworldLuminanceOres =
                List.of(BlockReplacement.replace(stoneReplacables, ModBlocks.LUMINANCE_ORE.defaultBlockState()),
                        BlockReplacement.replace(deepslateReplacables, ModBlocks.LUMINANCE_ORE.defaultBlockState()));

        context.register(LUMINANCE_ORE_KEY, new OreFeature(overworldLuminanceOres, 10));
    }

    public static ResourceKey<Feature> registerKey(String name) {
        return ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, name));
    }
}
