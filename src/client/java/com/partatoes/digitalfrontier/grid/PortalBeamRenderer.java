package com.partatoes.digitalfrontier.grid;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.partatoes.digitalfrontier.grid.exit.PortalBeams;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/**
 * Draws the Portal's beam of light (see {@link PortalBeams}) from anywhere in the grid. A beam further away than the
 * loaded world is drawn closer in and smaller by the same factor, which looks exactly the same from the camera but
 * stays in front of the far clipping plane; fog is left off it, so it shows through the haze like a beacon's.
 */
public final class PortalBeamRenderer {
    private static final int HEIGHT = 2048;
    // The beam never looks thinner than this many blocks per block of distance, so it stays a visible line far away
    private static final double MIN_ANGULAR_RADIUS = 0.0015;

    // The beacon renderer only queues its beam; this queue is drawn on its own after the main pass, with fog off
    private static final SubmitNodeStorage QUEUE = new SubmitNodeStorage();
    // Our own, since vanilla's is still drawing the frame when the main pass ends; made on the first beam
    private static FeatureRenderDispatcher dispatcher;
    private static Identifier world;
    private static List<PortalBeams.Beam> beams = List.of();

    private PortalBeamRenderer() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PortalBeams.BeamsPayload.ID, (payload, context) -> {
            world = payload.world();
            beams = payload.beams();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            world = null;
            beams = List.of();
        });
        LevelRenderEvents.END_MAIN.register(context -> render());
    }

    private static void render() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel clientWorld = client.level;
        if (beams.isEmpty() || clientWorld == null || !clientWorld.dimension().identifier().equals(world)) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        // Well inside the far plane (four times the view distance), and past the loaded terrain
        double near = client.options.getEffectiveRenderDistance() * 16 * 1.5;
        // Turning like a beacon's beam
        float rotation = Math.floorMod(clientWorld.getGameTime(), 40) + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        PoseStack matrices = new PoseStack();

        for (PortalBeams.Beam beam : beams) {
            double centerX = (beam.minX() + beam.maxX() + 1) / 2.0;
            double centerZ = (beam.minZ() + beam.maxZ() + 1) / 2.0;
            double distance = Math.hypot(centerX - camera.x, centerZ - camera.z);
            float radius = (float) Math.max(Math.max(beam.maxX() - beam.minX(), beam.maxZ() - beam.minZ()) / 2.0 + 0.5,
                    distance * MIN_ANGULAR_RADIUS);
            float scale = distance > near ? (float) (near / distance) : 1;

            matrices.pushPose();
            matrices.scale(scale, scale, scale);
            // The beacon renderer centers its beam on the block, half a block in
            matrices.translate(centerX - 0.5 - camera.x, beam.baseY() - camera.y, centerZ - 0.5 - camera.z);
            BeaconRenderer.submitBeaconBeam(matrices, QUEUE, BeaconRenderer.BEAM_LOCATION, 1, rotation, 0, HEIGHT,
                    PortalBeams.COLOR, radius * 0.8f, radius);
            matrices.popPose();
        }

        GpuBufferSlice fog = RenderSystem.getShaderFog();
        RenderSystem.setShaderFog(client.gameRenderer.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
        if (dispatcher == null) {
            dispatcher = new FeatureRenderDispatcher(new RenderBuffers(1), client.getModelManager(), client.getAtlasManager(),
                    client.font, client.gameRenderer.gameRenderState());
        }
        // Draws and empties the queue
        dispatcher.renderAllFeatures(QUEUE);
        RenderSystem.setShaderFog(fog);
    }
}
