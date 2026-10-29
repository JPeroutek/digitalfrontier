package com.partatoes.digitalfrontier.mixin.client;

import com.partatoes.digitalfrontier.entity.vehicle.LightCycleEntity;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "rideTick", at = @At("TAIL"))
    public void addVehicleRidingCode(CallbackInfo ci) {
        LocalPlayer clientPlayerEntity = (LocalPlayer)(Object) this;
        if (clientPlayerEntity.getControlledVehicle() instanceof LightCycleEntity vehicle) {
            vehicle.setInputs(clientPlayerEntity.input.keyPresses.left(),
                    clientPlayerEntity.input.keyPresses.right(),
                    clientPlayerEntity.input.keyPresses.forward(),
                    clientPlayerEntity.input.keyPresses.backward());
            clientPlayerEntity.handsBusy |= clientPlayerEntity.input.keyPresses.left() ||
                    clientPlayerEntity.input.keyPresses.right() ||
                    clientPlayerEntity.input.keyPresses.forward() ||
                    clientPlayerEntity.input.keyPresses.backward();
        }
    }
}
