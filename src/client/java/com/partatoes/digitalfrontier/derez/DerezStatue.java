package com.partatoes.digitalfrontier.derez;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.partatoes.digitalfrontier.DigitalFrontier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds a derezzing body's statue (see {@link DerezStyle#VOXEL}): its model, posed as it was at the moment of death,
 * rebuilt from small cubes. Every face of every part is covered in a grid of cubes, each colored from the skin at
 * that spot, so the statue has the body's shape and looks. The cubes hold still, then break off from the head down
 * and fly apart.
 */
public final class DerezStatue {
    // Skins read back from the resource packs, by texture; null where a skin isn't a plain resource (players' skins)
    private static final Map<Identifier, Optional<NativeImage>> SKINS = new HashMap<>();

    private DerezStatue() {
    }

    /** A face of the model in world space, with its skin coordinates. */
    private record Face(Vector3f[] corners, float[][] uvs, Vector3f center, Quaternionf rotation) {
        float area() {
            Vector3f across = new Vector3f(corners[1]).sub(corners[0]);
            Vector3f down = new Vector3f(corners[3]).sub(corners[0]);
            return across.cross(down).length();
        }
    }

    /**
     * Raises a statue of a model and sets it falling apart.
     *
     * @param root    the model's root part, already posed for this frame
     * @param pose    the transform the model is drawn with (relative to the camera), as at the moment it's drawn
     * @param camera  where the camera is, which the pose is relative to
     * @param skin    the model's texture, or null to color it in {@code glow}
     * @param glow    the color for cubes of a model whose skin can't be read
     * @param centerX where the body stands, for flinging its cubes outward
     */
    public static void raise(ClientLevel level, ModelPart root, PoseStack pose, Vec3 camera, @Nullable Identifier skin,
                             int glow, double centerX, double centerZ) {
        List<Face> faces = new ArrayList<>();
        root.visit(pose, (partPose, path, index, cube) -> {
            Matrix4f matrix = partPose.pose();
            Quaternionf rotation = matrix.getNormalizedRotation(new Quaternionf());
            Vector3f center = matrix.transformPosition(new Vector3f(
                    (cube.minX + cube.maxX) / 32, (cube.minY + cube.maxY) / 32, (cube.minZ + cube.maxZ) / 32)).add(
                    (float) camera.x, (float) camera.y, (float) camera.z);
            for (ModelPart.Polygon polygon : cube.polygons) {
                ModelPart.Vertex[] vertices = polygon.vertices();
                if (vertices.length != 4) {
                    continue;
                }
                Vector3f[] corners = new Vector3f[4];
                float[][] uvs = new float[4][];
                for (int i = 0; i < 4; i++) {
                    corners[i] = matrix.transformPosition(new Vector3f(vertices[i].worldX(), vertices[i].worldY(), vertices[i].worldZ()))
                            .add((float) camera.x, (float) camera.y, (float) camera.z);
                    uvs[i] = new float[]{vertices[i].u(), vertices[i].v()};
                }
                faces.add(new Face(corners, uvs, center, rotation));
            }
        });
        if (faces.isEmpty()) {
            return;
        }
        double surface = faces.stream().mapToDouble(Face::area).sum();
        float cube = Derez.statueCube(surface);
        NativeImage image = skin == null ? null : skin(skin);

        // The statue's height, so its cubes break off from the head down
        float bottom = Float.MAX_VALUE;
        float top = -Float.MAX_VALUE;
        for (Face face : faces) {
            for (Vector3f corner : face.corners()) {
                bottom = Math.min(bottom, corner.y);
                top = Math.max(top, corner.y);
            }
        }
        float height = Math.max(0.01f, top - bottom);

        RandomSource random = level.getRandom();
        Set<Long> placed = new HashSet<>();
        Vector3f across = new Vector3f();
        Vector3f down = new Vector3f();
        Vector3f spot = new Vector3f();
        Vector3f inward = new Vector3f();
        for (Face face : faces) {
            Vector3f[] c = face.corners();
            across.set(c[1]).sub(c[0]);
            down.set(c[3]).sub(c[0]);
            int columns = Math.max(1, Math.round(across.length() / cube));
            int rows = Math.max(1, Math.round(down.length() / cube));
            // Half a cube in from the face, toward the part's middle, so the cubes sit inside the outline
            Vector3f normal = new Vector3f(across).cross(down).normalize();
            if (normal.dot(new Vector3f(face.center()).sub(c[0])) < 0) {
                normal.negate();
            }
            for (int column = 0; column < columns; column++) {
                for (int row = 0; row < rows; row++) {
                    float a = (column + 0.5f) / columns;
                    float b = (row + 0.5f) / rows;
                    bilerp(c, a, b, spot);
                    spot.add(inward.set(normal).mul(cube / 2));
                    // Faces meet at the edges: one cube per spot
                    if (!placed.add(key(spot, cube))) {
                        continue;
                    }
                    int color = color(image, face.uvs(), a, b, glow, random);
                    if (color < 0) {
                        continue;
                    }
                    double outX = spot.x - centerX;
                    double outZ = spot.z - centerZ;
                    double length = Math.max(0.05, Math.sqrt(outX * outX + outZ * outZ));
                    double fling = 0.05 + random.nextDouble() * 0.12;
                    Minecraft.getInstance().particleEngine.add(new DerezCubeParticle(level, spot.x, spot.y - cube / 2, spot.z,
                            outX / length * fling + (random.nextDouble() - 0.5) * 0.04,
                            0.04 + random.nextDouble() * 0.16,
                            outZ / length * fling + (random.nextDouble() - 0.5) * 0.04,
                            cube * 0.96f, color, new Quaternionf(face.rotation()),
                            Derez.releaseDelay((spot.y - bottom) / height) + random.nextInt(3)));
                }
            }
        }
    }

    private static void bilerp(Vector3f[] c, float a, float b, Vector3f out) {
        // c[0] → c[1] is across the face, c[0] → c[3] down it
        out.set(c[0]).lerp(c[1], a).lerp(new Vector3f(c[3]).lerp(c[2], a), b);
    }

    private static long key(Vector3f spot, float cube) {
        long x = Math.round(spot.x / cube * 2) & 0x1FFFFF;
        long y = Math.round(spot.y / cube * 2) & 0x1FFFFF;
        long z = Math.round(spot.z / cube * 2) & 0x1FFFFF;
        return x << 42 | y << 21 | z;
    }

    // The skin's color at a spot on a face, or -1 where the skin is see-through there (nothing to build)
    private static int color(@Nullable NativeImage image, float[][] uvs, float a, float b, int glow, RandomSource random) {
        if (image == null) {
            return random.nextFloat() < 0.7f ? glow : 0x14191E;
        }
        float u = lerp(lerp(uvs[0][0], uvs[1][0], a), lerp(uvs[3][0], uvs[2][0], a), b);
        float v = lerp(lerp(uvs[0][1], uvs[1][1], a), lerp(uvs[3][1], uvs[2][1], a), b);
        int x = Math.clamp((int) (u * image.getWidth()), 0, image.getWidth() - 1);
        int y = Math.clamp((int) (v * image.getHeight()), 0, image.getHeight() - 1);
        int pixel = image.getPixel(x, y);
        return ARGB.alpha(pixel) < 128 ? -1 : pixel & 0xFFFFFF;
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    @Nullable
    private static NativeImage skin(Identifier texture) {
        return SKINS.computeIfAbsent(texture, id -> Minecraft.getInstance().getResourceManager().getResource(id).flatMap(resource -> {
            try (InputStream stream = resource.open()) {
                return Optional.of(NativeImage.read(stream));
            } catch (Exception e) {
                DigitalFrontier.LOGGER.debug("Couldn't read {} for a derez statue", id, e);
                return Optional.empty();
            }
        })).orElse(null);
    }

    /** Forgets the skins read so far, so a resource reload's new ones are used. */
    public static void clearSkins() {
        SKINS.values().forEach(skin -> skin.ifPresent(NativeImage::close));
        SKINS.clear();
    }
}
