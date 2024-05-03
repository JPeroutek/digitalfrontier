package com.partatoes.digitalfrontier;

import com.partatoes.digitalfrontier.entity.ModModelLayers;
import com.partatoes.digitalfrontier.entity.ModVehicles;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleModel;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class DigitalFrontierClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		EntityRendererRegistry.register(ModVehicles.LIGHTCYCLE, LightCycleRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(ModModelLayers.LIGHTCYCLE, LightCycleModel::getTexturedModelData);
	}
}