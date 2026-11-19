package com.partatoes.digitalfrontier.mixin.client;

import com.partatoes.digitalfrontier.entity.vehicle.LightCycleEntity;
import com.partatoes.digitalfrontier.entity.vehicle.RecognizerEntity;
import net.minecraft.world.entity.player.Input;
import com.partatoes.digitalfrontier.entity.vehicle.RecognizerEntity;
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
        } else if (clientPlayerEntity.getControlledVehicle() instanceof RecognizerEntity recognizer) {
            // A stolen Recognizer: turn and thrust as on a cycle, jump to climb, sprint to dive
            Input keys = clientPlayerEntity.input.keyPresses;
            recognizer.setInputs(keys.left(), keys.right(), keys.forward(), keys.backward(), keys.jump(), keys.sprint());
            clientPlayerEntity.handsBusy |= keys.left() || keys.right() || keys.forward() || keys.backward();
        }
    }
}
