package com.partatoes.digitalfrontier.derez;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws derez cubes ({@link DerezCubeParticle}) as real cubes: six faces each, full bright, in one batch per frame.
 * Vanilla particles are flat sprites, so they get a group of their own (registered with Fabric's
 * {@code ParticleGroupRegistry}), modelled on vanilla's elder guardian particles.
 */
public class DerezCubeGroup extends ParticleGroup<DerezCubeParticle> {
    public static final ParticleRenderType TYPE = new ParticleRenderType("digitalfrontier:derez_cubes", "DEREZ");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "textures/entity/derez_cube.png");
    // Translucent but writing depth, so the cubes sort against the world: glass and water behind them stay behind.
    // How see-through each cube is comes from its alpha: a statue stands nearly solid, loose cubes are lighter
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(TEXTURE);

    // The corners of a unit cube around its center, and its faces as corner indices (counter-clockwise from outside)
    private static final float[][] CORNERS = {
            {-0.5f, -0.5f, -0.5f}, {0.5f, -0.5f, -0.5f}, {0.5f, 0.5f, -0.5f}, {-0.5f, 0.5f, -0.5f},
            {-0.5f, -0.5f, 0.5f}, {0.5f, -0.5f, 0.5f}, {0.5f, 0.5f, 0.5f}, {-0.5f, 0.5f, 0.5f}};
    private static final int[][] FACES = {
            {0, 3, 2, 1}, {4, 5, 6, 7}, {0, 4, 7, 3}, {1, 2, 6, 5}, {0, 1, 5, 4}, {3, 7, 6, 2}};
    private static final float[][] NORMALS = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}};
    private static final float[][] UVS = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};

    public DerezCubeGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTick) {
        Vec3 eye = camera.position();
        List<Cube> cubes = new ArrayList<>(particles.size());
        for (DerezCubeParticle particle : particles) {
            float opacity = particle.standing() ? Derez.STATUE_OPACITY : Derez.LOOSE_OPACITY;
            int alpha = (int) (particle.alpha(partialTick) * opacity * 255);
            if (alpha <= 0) {
                continue;
            }
            cubes.add(new Cube((float) (particle.x(partialTick) - eye.x), (float) (particle.y(partialTick) + particle.size / 2 - eye.y),
                    (float) (particle.z(partialTick) - eye.z), particle.size, particle.rotation(partialTick),
                    ARGB.color(alpha, particle.color(partialTick))));
        }
        return new State(cubes);
    }

    /** A cube as it's drawn this frame, its center relative to the camera (a particle's position is its base). */
    private record Cube(float x, float y, float z, float size, Quaternionf rotation, int color) {
    }

    private record State(List<Cube> cubes) implements ParticleGroupRenderState {
        @Override
        public void submit(SubmitNodeCollector queue, CameraRenderState camera) {
            if (!cubes.isEmpty()) {
                queue.submitCustomGeometry(new PoseStack(), RENDER_TYPE, (pose, vertices) -> draw(cubes, pose, vertices));
            }
        }

        private static void draw(List<Cube> cubes, PoseStack.Pose pose, VertexConsumer vertices) {
            Matrix4f matrix = pose.pose();
            Vector3f corner = new Vector3f();
            Vector3f normal = new Vector3f();
            for (Cube cube : cubes) {
                for (int face = 0; face < FACES.length; face++) {
                    cube.rotation().transform(NORMALS[face][0], NORMALS[face][1], NORMALS[face][2], normal);
                    for (int i = 0; i < 4; i++) {
                        float[] c = CORNERS[FACES[face][i]];
                        cube.rotation().transform(c[0] * cube.size(), c[1] * cube.size(), c[2] * cube.size(), corner);
                        corner.add(cube.x(), cube.y(), cube.z());
                        matrix.transformPosition(corner);
                        vertices.addVertex(corner.x, corner.y, corner.z, cube.color(), UVS[i][0], UVS[i][1],
                                OverlayTexture.NO_OVERLAY, LightCoordsUtil.FULL_BRIGHT, normal.x, normal.y, normal.z);
                    }
                }
            }
        }
    }
}
