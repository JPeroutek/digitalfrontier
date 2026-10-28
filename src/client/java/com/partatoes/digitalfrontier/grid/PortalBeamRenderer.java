package com.partatoes.digitalfrontier.grid;

import com.mojang.blaze3d.systems.RenderSystem;
import com.partatoes.digitalfrontier.grid.exit.PortalBeams;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
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
        WorldRenderEvents.AFTER_ENTITIES.register(PortalBeamRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        if (beams.isEmpty() || !context.world().getRegistryKey().getValue().equals(world)) {
            return;
        }
        Vec3d camera = context.camera().getPos();
        // Well inside the far plane (four times the view distance), and past the loaded terrain
        double near = MinecraftClient.getInstance().gameRenderer.getViewDistanceBlocks() * 1.5;
        float tickDelta = context.tickCounter().getTickProgress(false);
        long time = context.world().getTime();
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
            BeaconBlockEntityRenderer.renderBeam(matrices, BUFFERS, BeaconBlockEntityRenderer.BEAM_TEXTURE, tickDelta, 1, time,
                    0, HEIGHT, PortalBeams.COLOR, radius * 0.8f, radius);
            matrices.pop();
        }

        Fog fog = RenderSystem.getShaderFog();
        RenderSystem.setShaderFog(Fog.DUMMY);
        BUFFERS.draw();
        RenderSystem.setShaderFog(fog);
    }
}
