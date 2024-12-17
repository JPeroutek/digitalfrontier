package com.partatoes.digitalfrontier.mixin.client;

import com.partatoes.digitalfrontier.entity.vehicle.LightCycleEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "tickRiding", at = @At("TAIL"))
    public void addVehicleRidingCode(CallbackInfo ci) {
        ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity)(Object) this;
        if (clientPlayerEntity.getControllingVehicle() instanceof LightCycleEntity vehicle) {
            vehicle.setInputs(clientPlayerEntity.input.playerInput.left(),
                    clientPlayerEntity.input.playerInput.right(),
                    clientPlayerEntity.input.playerInput.forward(),
                    clientPlayerEntity.input.playerInput.backward());
            clientPlayerEntity.riding |= clientPlayerEntity.input.playerInput.left() ||
                    clientPlayerEntity.input.playerInput.right() ||
                    clientPlayerEntity.input.playerInput.forward() ||
                    clientPlayerEntity.input.playerInput.backward();
        }
    }
}
