package com.partatoes.digitalfrontier.grid;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.partatoes.digitalfrontier.grid.exit.PortalBeams;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import net.minecraft.client.render.command.CustomCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

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
    private static final OrderedRenderCommandQueueImpl QUEUE = new OrderedRenderCommandQueueImpl();
    private static final CustomCommandRenderer CUSTOM_RENDERER = new CustomCommandRenderer();
    private static final VertexConsumerProvider.Immediate BUFFERS = VertexConsumerProvider.immediate(new BufferAllocator(1536));
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
    }

    /** Called from the world renderer's main pass, after entities (see WorldRendererMixin). */
    public static void render() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld clientWorld = client.world;
        if (beams.isEmpty() || clientWorld == null || !clientWorld.getRegistryKey().getValue().equals(world)) {
            return;
        }
        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        // Well inside the far plane (four times the view distance), and past the loaded terrain
        double near = client.gameRenderer.getViewDistanceBlocks() * 1.5;
        // Turning like a beacon's beam
        float rotation = Math.floorMod(clientWorld.getTime(), 40) + client.getRenderTickCounter().getTickProgress(false);
        MatrixStack matrices = new MatrixStack();

        for (PortalBeams.Beam beam : beams) {
            double centerX = (beam.minX() + beam.maxX() + 1) / 2.0;
            double centerZ = (beam.minZ() + beam.maxZ() + 1) / 2.0;
            double distance = Math.hypot(centerX - camera.x, centerZ - camera.z);
            float radius = (float) Math.max(Math.max(beam.maxX() - beam.minX(), beam.maxZ() - beam.minZ()) / 2.0 + 0.5,
                    distance * MIN_ANGULAR_RADIUS);
            float scale = distance > near ? (float) (near / distance) : 1;

            matrices.push();
            matrices.scale(scale, scale, scale);
            // The beacon renderer centers its beam on the block, half a block in
            matrices.translate(centerX - 0.5 - camera.x, beam.baseY() - camera.y, centerZ - 0.5 - camera.z);
            BeaconBlockEntityRenderer.renderBeam(matrices, QUEUE, BeaconBlockEntityRenderer.BEAM_TEXTURE, 1, rotation, 0, HEIGHT,
                    PortalBeams.COLOR, radius * 0.8f, radius);
            matrices.pop();
        }

        for (BatchingRenderCommandQueue queue : QUEUE.getBatchingQueues().values()) {
            CUSTOM_RENDERER.render(queue, BUFFERS);
        }
        QUEUE.clear();
        GpuBufferSlice fog = RenderSystem.getShaderFog();
        RenderSystem.setShaderFog(client.gameRenderer.fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));
        BUFFERS.draw();
        RenderSystem.setShaderFog(fog);
    }
}
