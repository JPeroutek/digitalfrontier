package com.partatoes.digitalfrontier.mixin;

import com.partatoes.digitalfrontier.item.custom.ForceShield;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A raised Force Shield sends projectiles that hit its face back the way they came (see {@link ForceShield}). */
@Mixin(Entity.class)
public abstract class EntityDeflectionMixin {
    @Inject(method = "deflection", at = @At("HEAD"), cancellable = true)
    private void digitalfrontier$forceShield(Projectile projectile, CallbackInfoReturnable<ProjectileDeflection> cir) {
        if ((Object) this instanceof LivingEntity holder && ForceShield.reflects(holder, projectile)) {
            cir.setReturnValue(ProjectileDeflection.REVERSE);
        }
    }
}
