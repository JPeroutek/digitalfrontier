package com.partatoes.digitalfrontier.world;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.GridKey;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import java.util.Optional;

public class ModDimensions {
    // One dimension type shared by every grid
    public static final Identifier DIGITAL_FRONTIER_TYPE_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "digitalfrontier");
    public static final ResourceKey<DimensionType> DIGITAL_FRONTIER_TYPE_KEY = ResourceKey.create(Registries.DIMENSION_TYPE, DIGITAL_FRONTIER_TYPE_ID);

    // The default grid; tuned grids get their ids from GridKey at runtime
    public static final Identifier DEFAULT_GRID_ID = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, GridKey.DEFAULT_PATH);
    public static final ResourceKey<LevelStem> DEFAULT_GRID_KEY = ResourceKey.create(Registries.LEVEL_STEM, DEFAULT_GRID_ID);
    public static final ResourceKey<Level> DEFAULT_GRID_WORLD_KEY = ResourceKey.create(Registries.DIMENSION, DEFAULT_GRID_ID);

    public static boolean isGrid(Level world) {
        return world.dimensionTypeRegistration().is(DIGITAL_FRONTIER_TYPE_KEY);
    }

    public static void bootstrapType(BootstrapContext<DimensionType> context) {
        context.register(DIGITAL_FRONTIER_TYPE_KEY, new DimensionType(
                true, // hasFixedTime: no day and night on the grid
                false, // hasSkylight
                false, // hasCeiling
                false, // hasEnderDragonFight
                1.0, // coordinateScale
                -64, // minY
                384, // height
                384, // logicalHeight
                context.lookup(Registries.BLOCK).getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
                0.08f, // ambientLight: a faint floor, so unlit corners aren't pitch black (the lightmap uses AMBIENT_LIGHT_COLOR below)
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.END,
                CardinalLighting.Type.DEFAULT,
                EnvironmentAttributeMap.builder()
                        // The lightmap's floor: dark, faintly blue, but enough to make out unlit blocks
                        .set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, 0x141A20)
                        // Beds explode and never set a spawn: players always respawn outside the grid unless something says otherwise
                        .set(EnvironmentAttributes.BED_RULE, BedRule.EXPLODES)
                        .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
                        .set(EnvironmentAttributes.CAN_START_RAID, false)
                        .set(EnvironmentAttributes.PIGLINS_ZOMBIFY, false)
                        .build(),
                HolderSet.empty(), // timelines: none, so nothing changes with the time of day
                Optional.empty())); // defaultClock: none, the time is fixed
    }
}
