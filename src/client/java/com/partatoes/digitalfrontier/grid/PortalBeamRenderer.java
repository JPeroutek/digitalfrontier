package com.partatoes.digitalfrontier.grid;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
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
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Draws the Portal's beam of light (see {@link PortalBeams}) from anywhere in the grid. A beam further away than the
 * loaded world is drawn closer in and smaller by the same factor, which looks exactly the same from the camera but
 * stays in front of the far clipping plane; fog is left off it, so it shows through the haze like a beacon's.
 */
public final class PortalBeamRenderer {
    private static final int HEIGHT = 2048;
    // The beam never looks thinner than this many blocks per block of distance, so it stays a visible line far away
    private static final double MIN_ANGULAR_RADIUS = 0.0015;

    // The beacon renderer only queues its beam; this queue is drawn on its own, with fog off
    private static final SubmitNodeStorage QUEUE = new SubmitNodeStorage();
    // Our own, since vanilla's is busy with the frame; made on the first beam
    private static FeatureRenderDispatcher dispatcher;
    // This frame's beams, prepared before any drawing starts and drawn once
    @Nullable
    private static FeatureRenderDispatcher.PreparedFrame prepared;
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
        // Normally drawn just before translucent terrain (see LevelRendererMixin), so water in front of a beam covers
        // it; with Fabulous transparency that never happens, so the beams are drawn at the end of the main pass instead
        LevelRenderEvents.END_MAIN.register(context -> {
            if (prepared != null) {
                RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
                // Into the world's colour and depth, so terrain still hides it
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Portal beams",
                        target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
                    draw(pass);
                }
            }
        });
    }

    /** Draws this frame's beams into a render pass on the world's colour and depth, if they haven't been yet. */
    public static void draw(RenderPass pass) {
        if (prepared == null) {
            return;
        }
        GpuBufferSlice fog = RenderSystem.getShaderFog();
        RenderSystem.setShaderFog(Minecraft.getInstance().gameRenderer.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
        FeatureRenderDispatcher.renderAllFeatures(pass, prepared);
        RenderSystem.setShaderFog(fog);
        prepared.close();
        prepared = null;
    }

    /** Queues and prepares this frame's beams; called as vanilla prepares its own frame, before any drawing starts. */
    public static void prepare() {
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

        if (dispatcher == null) {
            dispatcher = new FeatureRenderDispatcher(new RenderBuffers(1), client.getModelManager(), client.getAtlasManager(),
                    client.font, client.gameRenderer.gameRenderState());
        }
        if (prepared != null) {
            prepared.close();
        }
        // Uploads the beams' vertices and empties the queue
        prepared = dispatcher.prepareFrame(QUEUE);
    }
}
