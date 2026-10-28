package com.partatoes.digitalfrontier.world;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.grid.GridKey;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;

import java.util.OptionalLong;

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
                OptionalLong.of(6000), // fixedTime
                false, // hasSkylight
                false, // hasCeiling
                false, // ultrawarm
                false, // natural
                1.0, // coordinateScale
                false, // bedWorks: players always respawn outside the grid unless something says otherwise
                false, // respawnAnchorWorks
                -64, // minY
                384, // height
                384, // logicalHeight
                BlockTags.INFINIBURN_OVERWORLD,
                DimensionTypes.THE_END_ID, // effects
                0.0f, // ambientLight
                new DimensionType.MonsterSettings(true, false, ConstantIntProvider.create(0), 0)));
    }
}
