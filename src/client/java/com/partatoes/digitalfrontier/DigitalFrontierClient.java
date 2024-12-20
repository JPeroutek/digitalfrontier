package com.partatoes.digitalfrontier;

import com.partatoes.digitalfrontier.entity.*;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleModel;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class DigitalFrontierClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		// Vehicles
		EntityRendererRegistry.register(ModVehicles.LIGHTCYCLE, LightCycleRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(ModModelLayers.LIGHTCYCLE, LightCycleModel::getTexturedModelData);

		// Entities
		EntityRendererRegistry.register(ModEntities.PROGRAM, ProgramRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PROGRAM, ProgramModel::getTexturedModelData);
	}
}