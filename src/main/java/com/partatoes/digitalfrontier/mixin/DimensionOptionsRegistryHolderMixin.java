package com.partatoes.digitalfrontier.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Lifecycle;
import com.partatoes.digitalfrontier.DigitalFrontier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

/**
 * A world's dimension registry is marked experimental for any dimension that isn't vanilla, and for having more than
 * the vanilla three; so every world would warn about experimental settings when created or loaded, just for having
 * the grid. The grid's own dimensions are left out of both checks; any other dimension still counts.
 */
@Mixin(WorldDimensions.class)
public abstract class DimensionOptionsRegistryHolderMixin {
    @Shadow
    public abstract Map<ResourceKey<LevelStem>, LevelStem> dimensions();

    @Inject(method = "checkStability(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/dimension/LevelStem;)Lcom/mojang/serialization/Lifecycle;",
            at = @At("HEAD"), cancellable = true)
    private static void digitalfrontier$gridIsStable(ResourceKey<LevelStem> key, LevelStem options,
                                                     CallbackInfoReturnable<Lifecycle> cir) {
        if (isGrid(key)) {
            cir.setReturnValue(Lifecycle.stable());
        }
    }

    // The number of dimensions compared with the vanilla three: the same dimensions toConfig collects, less the grid's
    @ModifyExpressionValue(method = "bake", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private int digitalfrontier$leaveOutGrid(int size, Registry<LevelStem> existingRegistry) {
        long grid = Stream.concat(existingRegistry.registryKeySet().stream(), dimensions().keySet().stream())
                .distinct()
                .filter(DimensionOptionsRegistryHolderMixin::isGrid)
                .count();
        return size - (int) grid;
    }

    private static boolean isGrid(ResourceKey<LevelStem> key) {
        return key.identifier().getNamespace().equals(DigitalFrontier.MOD_ID);
    }
}
