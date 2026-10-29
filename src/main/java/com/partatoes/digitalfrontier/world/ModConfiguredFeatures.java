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
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?,?>> LUMINANCE_ORE_KEY = registerKey("luminance_ore");


    public static void bootstrap(BootstrapContext<ConfiguredFeature<?,?>> context) {
        RuleTest stoneReplacables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplacables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest netherReplacables = new TagMatchTest(BlockTags.BASE_STONE_NETHER);
        RuleTest endReplacables = new BlockMatchTest(Blocks.END_STONE);

        List<OreConfiguration.TargetBlockState> overworldLuminanceOres =
                List.of(OreConfiguration.target(stoneReplacables, ModBlocks.LUMINANCE_ORE.defaultBlockState()),
                        OreConfiguration.target(deepslateReplacables, ModBlocks.LUMINANCE_ORE.defaultBlockState()));

        register(context, LUMINANCE_ORE_KEY, Feature.ORE, new OreConfiguration(overworldLuminanceOres, 10));
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                   ResourceKey<ConfiguredFeature<?,?>> key,
                                                                                   F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
