package com.partatoes.digitalfrontier;

import com.partatoes.digitalfrontier.entity.*;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleModel;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleRenderer;
import com.partatoes.digitalfrontier.block.ModBlocks;
import com.partatoes.digitalfrontier.grid.tuning.GridTuning;
import com.partatoes.digitalfrontier.grid.tuning.GridTuningNetworking;
import com.partatoes.digitalfrontier.screen.GridTuningScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
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

		// Pattern blocks draw a transparent glow overlay over their base texture, which needs cutout rendering
		ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS.values().forEach(colorMapping ->
				BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), colorMapping.values().toArray(Block[]::new)));
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), ModBlocks.ARRIVAL_PAD, ModBlocks.GRID_PORTAL);

		// Grid Tuning Laser screen, opened by the server with the laser's current grid code
		ClientPlayNetworking.registerGlobalReceiver(GridTuningNetworking.OpenScreenPayload.ID, (payload, context) ->
				GridTuning.fromCode(payload.code()).ifPresent(tuning -> context.client().setScreen(new GridTuningScreen(payload.pos(), tuning))));
	}
}