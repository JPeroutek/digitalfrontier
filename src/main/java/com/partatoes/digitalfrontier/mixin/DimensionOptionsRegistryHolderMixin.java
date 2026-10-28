package com.partatoes.digitalfrontier.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Lifecycle;
import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.stream.Stream;

/**
 * A world's dimension registry is marked experimental for any dimension that isn't vanilla, and for having more than
 * the vanilla three; so every world would warn about experimental settings when created or loaded, just for having
 * the grid. The grid's own dimensions are left out of both checks; any other dimension still counts.
 */
@Mixin(DimensionOptionsRegistryHolder.class)
public abstract class DimensionOptionsRegistryHolderMixin {
    @Shadow
    public abstract Map<RegistryKey<DimensionOptions>, DimensionOptions> dimensions();

    @Inject(method = "getLifecycle(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/world/dimension/DimensionOptions;)Lcom/mojang/serialization/Lifecycle;",
            at = @At("HEAD"), cancellable = true)
    private static void digitalfrontier$gridIsStable(RegistryKey<DimensionOptions> key, DimensionOptions options,
                                                     CallbackInfoReturnable<Lifecycle> cir) {
        if (isGrid(key)) {
            cir.setReturnValue(Lifecycle.stable());
        }
    }

    // The number of dimensions compared with the vanilla three: the same dimensions toConfig collects, less the grid's
    @ModifyExpressionValue(method = "toConfig", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private int digitalfrontier$leaveOutGrid(int size, Registry<DimensionOptions> existingRegistry) {
        long grid = Stream.concat(existingRegistry.getKeys().stream(), dimensions().keySet().stream())
                .distinct()
                .filter(DimensionOptionsRegistryHolderMixin::isGrid)
                .count();
        return size - (int) grid;
    }

    private static boolean isGrid(RegistryKey<DimensionOptions> key) {
        return key.getValue().getNamespace().equals(DigitalFrontier.MOD_ID);
    }
}
