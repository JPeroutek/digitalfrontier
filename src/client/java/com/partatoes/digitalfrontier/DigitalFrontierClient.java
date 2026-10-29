package com.partatoes.digitalfrontier;

import com.partatoes.digitalfrontier.entity.*;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleModel;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleRenderer;
import com.partatoes.digitalfrontier.entity.custom.LieutenantEntity;
import com.partatoes.digitalfrontier.grid.GridDebugEntry;
import com.partatoes.digitalfrontier.grid.PortalBeamRenderer;
import com.partatoes.digitalfrontier.grid.stronghold.LieutenantDef;
import com.partatoes.digitalfrontier.grid.tuning.GridTuning;
import com.partatoes.digitalfrontier.grid.tuning.GridTuningNetworking;
import com.partatoes.digitalfrontier.screen.GridTuningScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public class DigitalFrontierClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		// Vehicles
		EntityRendererRegistry.register(ModVehicles.LIGHTCYCLE, LightCycleRenderer::new);
		ModelLayerRegistry.registerModelLayer(ModModelLayers.LIGHTCYCLE, LightCycleModel::getTexturedModelData);

		// Entities
		EntityRendererRegistry.register(ModEntities.PROGRAM, ProgramRenderer::new);
		EntityRendererRegistry.register(ModEntities.CORRUPTED_PROGRAM, context -> new GridMobRenderer<>(context,
				GridMobRenderer.variants("corrupted_program_0", "corrupted_program_1", "corrupted_program_2"), 0.6f));
		EntityRendererRegistry.register(ModEntities.ENFORCER, context -> new GridMobRenderer<>(context,
				GridMobRenderer.variants("guard_red64", "guard_orange64"), 0.2f));
		// The lieutenant's skin comes from its definition, so lore packs can swap it
		EntityRendererRegistry.register(ModEntities.LIEUTENANT, context -> new GridMobRenderer<LieutenantEntity>(context,
				entity -> entity.texture() != null ? entity.texture() : LieutenantDef.Archetype.RULER.defaultTexture(), 0.35f));
		ModelLayerRegistry.registerModelLayer(ModModelLayers.PROGRAM, ProgramModel::getTexturedModelData);

		// The Portal's beam, seen from anywhere in the grid
		PortalBeamRenderer.register();

		// An F3 section of what the grid generates where you stand: enable digitalfrontier:grid in the debug options (F3 + F6)
		GridDebugEntry.register();

		// Grid Tuning Laser screen, opened by the server with the laser's current grid code
		ClientPlayNetworking.registerGlobalReceiver(GridTuningNetworking.OpenScreenPayload.ID, (payload, context) ->
				GridTuning.fromCode(payload.code()).ifPresent(tuning -> context.client().gui.setScreen(new GridTuningScreen(payload.pos(), tuning))));
	}
}