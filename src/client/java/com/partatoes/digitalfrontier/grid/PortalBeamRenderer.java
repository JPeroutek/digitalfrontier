package com.partatoes.digitalfrontier.grid;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.partatoes.digitalfrontier.grid.exit.PortalBeams;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
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

    // The beacon renderer only queues its beam; this queue is drawn straight into BUFFERS, away from the fogged entity pass
    private static final SubmitNodeStorage QUEUE = new SubmitNodeStorage();
    private static final CustomFeatureRenderer CUSTOM_RENDERER = new CustomFeatureRenderer();
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
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
        WorldRenderEvents.AFTER_ENTITIES.register(context -> render());
    }

    private static void render() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel clientWorld = client.level;
        if (beams.isEmpty() || clientWorld == null || !clientWorld.dimension().identifier().equals(world)) {
            return;
        }
        Vec3 camera = client.gameRenderer.getMainCamera().position();
        // Well inside the far plane (four times the view distance), and past the loaded terrain
        double near = client.gameRenderer.getRenderDistance() * 1.5;
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

        for (SubmitNodeCollection queue : QUEUE.getSubmitsPerOrder().values()) {
            CUSTOM_RENDERER.render(queue, BUFFERS);
        }
        QUEUE.clear();
        GpuBufferSlice fog = RenderSystem.getShaderFog();
        RenderSystem.setShaderFog(client.gameRenderer.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
        BUFFERS.endBatch();
        RenderSystem.setShaderFog(fog);
    }
}
