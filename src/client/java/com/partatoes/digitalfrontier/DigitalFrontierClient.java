package com.partatoes.digitalfrontier;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.*;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import com.partatoes.digitalfrontier.entity.core.SystemCoreModel;
import com.partatoes.digitalfrontier.entity.core.SystemCoreRenderer;
import com.partatoes.digitalfrontier.entity.bool.BoolModel;
import com.partatoes.digitalfrontier.entity.bool.BoolRenderer;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleModel;
import com.partatoes.digitalfrontier.entity.vehicle.LightCycleRenderer;
import com.partatoes.digitalfrontier.entity.custom.Decay;
import com.partatoes.digitalfrontier.entity.custom.LieutenantEntity;
import com.partatoes.digitalfrontier.entity.custom.ProgramEntity;
import com.partatoes.digitalfrontier.grid.GridDebugEntry;
import com.partatoes.digitalfrontier.grid.PortalBeamRenderer;
import com.partatoes.digitalfrontier.grid.tuning.GridTuning;
import com.partatoes.digitalfrontier.grid.tuning.GridTuningNetworking;
import com.partatoes.digitalfrontier.screen.GridTuningScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

public class DigitalFrontierClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		// Vehicles
		EntityRendererRegistry.register(ModVehicles.LIGHTCYCLE, LightCycleRenderer::new);
		ModelLayerRegistry.registerModelLayer(ModModelLayers.LIGHTCYCLE, LightCycleModel::getTexturedModelData);

		// Entities
		// A program with a job wears its job's skin. One drifting into decay jitters more the further it's gone, and its
		// circuitry frays orange near the end
		Function<ProgramEntity, Identifier> programSkins = GridMobRenderer.variants("program_0", "program_1", "program_2");
		Identifier fraying = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "textures/entity/program_fraying.png");
		EntityRendererRegistry.register(ModEntities.PROGRAM, context -> new GridMobRenderer<ProgramEntity>(context,
				program -> program.job() != null
						? Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "textures/entity/job/" + program.job().getSerializedName() + ".png")
						: program.decay() >= Decay.FRAYING ? fraying : programSkins.apply(program),
				program -> Decay.jitter(program.decay())));
		EntityRendererRegistry.register(ModEntities.CORRUPTED_PROGRAM, context -> new GridMobRenderer<>(context,
				GridMobRenderer.variants("corrupted_program_0", "corrupted_program_1", "corrupted_program_2"), 0.6f));
		EntityRendererRegistry.register(ModEntities.ENFORCER, context -> new GridMobRenderer<>(context,
				GridMobRenderer.variants("guard_red64", "guard_orange64"), 0));
		EntityRendererRegistry.register(ModEntities.RECRUITER, context -> new GridMobRenderer<>(context,
				GridMobRenderer.variants("recruiter64"), 0));
		// The lieutenant's skin comes from its definition, so lore packs can swap it; a client without the pack's
		// resource pack sees the archetype's skin instead
		PackTextures.register();
		EntityRendererRegistry.register(ModEntities.LIEUTENANT, context -> new GridMobRenderer<LieutenantEntity>(context,
				entity -> entity.texture() != null
						? PackTextures.orFallback(entity.texture(), entity.defaultTexture())
						: entity.defaultTexture(), 0));
		ModelLayerRegistry.registerModelLayer(ModModelLayers.PROGRAM, ProgramModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.BOOL, BoolRenderer::new);
		EntityRendererRegistry.register(ModEntities.DATA_DISC, ThrownItemRenderer::new);
		ModelLayerRegistry.registerModelLayer(ModModelLayers.BOOL, BoolModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.SYSTEM_CORE, SystemCoreRenderer::new);
		ModelLayerRegistry.registerModelLayer(ModModelLayers.SYSTEM_CORE, SystemCoreModel::getTexturedModelData);

		// The Portal's beam, seen from anywhere in the grid
		PortalBeamRenderer.register();

		// An F3 section of what the grid generates where you stand: enable digitalfrontier:grid in the debug options (F3 + F6)
		GridDebugEntry.register();

		// Grid Tuning Laser screen, opened by the server with the laser's current grid code
		ClientPlayNetworking.registerGlobalReceiver(GridTuningNetworking.OpenScreenPayload.ID, (payload, context) ->
				GridTuning.fromCode(payload.code()).ifPresent(tuning -> context.client().gui.setScreen(new GridTuningScreen(payload.pos(), tuning))));
	}
}