package com.partatoes.digitalfrontier.world;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.GridKey;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.world.World;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
public class ModDimensions {
    // One dimension type shared by every grid
    public static final Identifier DIGITAL_FRONTIER_TYPE_ID = Identifier.of(DigitalFrontier.MOD_ID, "digitalfrontier");
    public static final RegistryKey<DimensionType> DIGITAL_FRONTIER_TYPE_KEY = RegistryKey.of(RegistryKeys.DIMENSION_TYPE, DIGITAL_FRONTIER_TYPE_ID);

    // The default grid; tuned grids get their ids from GridKey at runtime
    public static final Identifier DEFAULT_GRID_ID = Identifier.of(DigitalFrontier.MOD_ID, GridKey.DEFAULT_PATH);
    public static final RegistryKey<DimensionOptions> DEFAULT_GRID_KEY = RegistryKey.of(RegistryKeys.DIMENSION, DEFAULT_GRID_ID);
    public static final RegistryKey<World> DEFAULT_GRID_WORLD_KEY = RegistryKey.of(RegistryKeys.WORLD, DEFAULT_GRID_ID);

    public static boolean isGrid(World world) {
        return world.getDimensionEntry().matchesKey(DIGITAL_FRONTIER_TYPE_KEY);
    }

    public static void bootstrapType(Registerable<DimensionType> context) {
        context.register(DIGITAL_FRONTIER_TYPE_KEY, new DimensionType(
                true, // hasFixedTime: no day and night on the grid
                false, // hasSkylight
                false, // hasCeiling
                1.0, // coordinateScale
                -64, // minY
                384, // height
                384, // logicalHeight
                BlockTags.INFINIBURN_OVERWORLD,
                0.05f, // ambientLight: a faint floor, so unlit corners aren't pitch black
                new DimensionType.MonsterSettings(ConstantIntProvider.create(0), 0),
                DimensionType.Skybox.END,
                DimensionType.CardinalLightType.DEFAULT,
                EnvironmentAttributeMap.builder()
                        // Beds explode and never set a spawn: players always respawn outside the grid unless something says otherwise
                        .with(EnvironmentAttributes.BED_RULE_GAMEPLAY, BedRule.OTHER_DIMENSION)
                        .with(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY, false)
                        .with(EnvironmentAttributes.CAN_START_RAID_GAMEPLAY, false)
                        .with(EnvironmentAttributes.PIGLINS_ZOMBIFY_GAMEPLAY, false)
                        .build(),
                RegistryEntryList.empty())); // timelines: none, so nothing changes with the time of day
    }
}
