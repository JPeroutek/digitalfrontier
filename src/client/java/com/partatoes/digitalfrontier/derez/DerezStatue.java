package com.partatoes.digitalfrontier.derez;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.mixin.client.ModelPartAccessor;
import com.partatoes.digitalfrontier.mixin.client.PalettedBaseTextureAccessor;
import com.partatoes.digitalfrontier.mixin.client.PalettedSlotKeyAccessor;
import com.partatoes.digitalfrontier.mixin.client.PalettedTextureManagerAccessor;
import com.partatoes.digitalfrontier.mixin.client.RenderSetupAccessor;
import com.partatoes.digitalfrontier.mixin.client.RenderTypeAccessor;
import com.partatoes.digitalfrontier.mixin.client.TextureBindingAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.palette.PaletteMapping;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds a derezzing body's statue (see {@link DerezStyle#VOXEL}): its model, posed as it was at the moment of death,
 * rebuilt from small cubes, with its armour and held items. Every face is covered in a grid of cubes, each colored
 * from the texture at that spot, so the statue has the body's shape and looks. The cubes hold still, then break off
 * from the head down and fly apart.
 *
 * <p>A {@link Builder} collects faces from the body's model and from whatever its render layers submit (armour models,
 * item quads) through a {@link Builder#collector() stand-in collector}, then raises them all at once.
 */
public final class DerezStatue {
    // Textures read back from the resource packs, by texture; empty where one can't be read
    private static final Map<Identifier, Optional<NativeImage>> TEXTURES = new HashMap<>();
    // Paletted textures (armour trims) recolored for statues, by the GPU slot each is drawn from
    private static final Map<PalettedTextureManager.Handle, Optional<NativeImage>> PALETTED = new IdentityHashMap<>();
    private static final int SHELL = 0x14191E;

    private DerezStatue() {
    }

    /** A face in world space, with where it lies on its texture (0 to 1) and how to color it. */
    private record Face(Vector3f[] corners, float[][] uvs, @Nullable NativeImage image, int tint, @Nullable Vector3f center,
                        Quaternionf rotation) {
        float area() {
            Vector3f across = new Vector3f(corners[1]).sub(corners[0]);
            Vector3f down = new Vector3f(corners[3]).sub(corners[0]);
            return across.cross(down).length();
        }
    }

    /** Collects a statue's faces, then raises it. */
    public static final class Builder {
        private final Vec3 camera;
        private final int glow;
        private final List<Face> faces = new ArrayList<>();

        /**
         * @param camera where the camera is: the poses handed in are relative to it
         * @param glow   the color for cubes of a texture that can't be read
         */
        public Builder(Vec3 camera, int glow) {
            this.camera = camera;
            this.glow = glow;
        }

        /** Adds a model's showing parts, posed as they are now, drawn with this texture and tint (-1 for none). */
        public void addModel(ModelPart root, PoseStack pose, @Nullable Identifier texture, int tint) {
            addPart(root, pose, texture == null ? null : image(texture), tint);
        }

        // A model drawn through a texture mapping: a paletted one (an armour trim) is recolored here, as it only exists on
        // the GPU; anything else is read from the render type's texture
        private void addModel(ModelPart root, PoseStack pose, RenderType type, @Nullable UvMapping mapping, int tint) {
            if (mapping instanceof PalettedTextureManager.Handle handle) {
                addPart(root, pose, paletted(handle), tint);
            } else {
                addModel(root, pose, texture(type), tint);
            }
        }

        private void addPart(ModelPart part, PoseStack pose, @Nullable NativeImage image, int tint) {
            if (!part.visible) {
                return;
            }
            ModelPartAccessor access = (ModelPartAccessor) (Object) part;
            pose.pushPose();
            part.translateAndRotate(pose);
            if (!part.skipDraw) {
                Matrix4f matrix = pose.last().pose();
                Quaternionf rotation = matrix.getNormalizedRotation(new Quaternionf());
                for (ModelPart.Cube cube : access.digitalfrontier$cubes()) {
                    Vector3f center = world(matrix, (cube.minX + cube.maxX) / 32, (cube.minY + cube.maxY) / 32, (cube.minZ + cube.maxZ) / 32);
                    for (ModelPart.Polygon polygon : cube.polygons) {
                        ModelPart.Vertex[] vertices = polygon.vertices();
                        if (vertices.length != 4) {
                            continue;
                        }
                        Vector3f[] corners = new Vector3f[4];
                        float[][] uvs = new float[4][];
                        for (int i = 0; i < 4; i++) {
                            corners[i] = world(matrix, vertices[i].worldX(), vertices[i].worldY(), vertices[i].worldZ());
                            uvs[i] = new float[]{vertices[i].u(), vertices[i].v()};
                        }
                        faces.add(new Face(corners, uvs, image, tint, center, rotation));
                    }
                }
            }
            for (ModelPart child : access.digitalfrontier$children().values()) {
                addPart(child, pose, image, tint);
            }
            pose.popPose();
        }

        /** Adds an item's quads, as placed by this pose, colored from their sprites and tinted by layer. */
        public void addQuads(List<BakedQuad> quads, PoseStack pose, int[] tints) {
            Matrix4f matrix = pose.last().pose();
            Quaternionf rotation = matrix.getNormalizedRotation(new Quaternionf());
            for (BakedQuad quad : quads) {
                TextureAtlasSprite sprite = quad.materialInfo().sprite();
                Identifier name = sprite.contents().name();
                NativeImage image = image(name.withPath(path -> "textures/" + path + ".png"));
                int tintIndex = quad.materialInfo().tintIndex();
                int tint = tintIndex >= 0 && tintIndex < tints.length ? tints[tintIndex] : -1;
                Vector3f[] corners = new Vector3f[4];
                float[][] uvs = new float[4][];
                for (int i = 0; i < 4; i++) {
                    Vector3fc position = quad.position(i);
                    corners[i] = world(matrix, position.x(), position.y(), position.z());
                    long packed = quad.packedUV(i);
                    // Atlas coordinates back to the sprite's own
                    uvs[i] = new float[]{
                            (UVPair.unpackU(packed) - sprite.getU0()) / (sprite.getU1() - sprite.getU0()),
                            (UVPair.unpackV(packed) - sprite.getV0()) / (sprite.getV1() - sprite.getV0())};
                }
                faces.add(new Face(corners, uvs, image, tint, null, rotation));
            }
        }

        private Vector3f world(Matrix4f matrix, float x, float y, float z) {
            return matrix.transformPosition(new Vector3f(x, y, z)).add((float) camera.x, (float) camera.y, (float) camera.z);
        }

        /**
         * A stand-in for the render queue to hand a body's render layers: instead of drawing, everything they submit
         * that has a shape (armour and other models, held items) joins the statue. Anything else is ignored.
         */
        public SubmitNodeCollector collector() {
            InvocationHandler handler = (proxy, method, args) -> {
                switch (method.getName()) {
                    case "order" -> {
                        return proxy;
                    }
                    case "submitModel" -> {
                        // The full form (model, state, pose, render type, light, overlay, tint, uv mapping, outline);
                        // the shorter forms fill in their defaults and land here
                        if (args.length == 9 && args[3] instanceof RenderType type) {
                            submitModel((Model<?>) args[0], args[1], (PoseStack) args[2], type, (UvMapping) args[7], (int) args[6]);
                            return null;
                        }
                    }
                    case "submitModelPart" -> {
                        if (args[2] instanceof RenderType type) {
                            addModel((ModelPart) args[0], (PoseStack) args[1], type, (UvMapping) args[5], args.length > 6 ? (int) args[6] : -1);
                        }
                        return null;
                    }
                    case "submitItem" -> {
                        addQuads(((ItemQuads) args[6]).all(), (PoseStack) args[0], (int[]) args[5]);
                        return null;
                    }
                    default -> {
                    }
                }
                if (method.isDefault()) {
                    return InvocationHandler.invokeDefault(proxy, method, args);
                }
                return method.getReturnType() == boolean.class ? false : null;
            };
            return (SubmitNodeCollector) Proxy.newProxyInstance(DerezStatue.class.getClassLoader(),
                    new Class<?>[]{SubmitNodeCollector.class}, handler);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void submitModel(Model model, Object state, PoseStack pose, RenderType type, @Nullable UvMapping mapping, int tint) {
            // Models are posed when they're drawn; a layer's model is posed for this body now
            model.setupAnim(state);
            addModel(model.root(), pose, type, mapping, tint);
        }

        /** Raises the statue and sets it falling apart. */
        public void raise(ClientLevel level, double centerX, double centerZ) {
            if (faces.isEmpty()) {
                return;
            }
            float cube = Derez.statueCube(faces.stream().mapToDouble(Face::area).sum());

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
            // One cube per spot. Faces are in drawing order, so where two meet at the same spot (an armour trim over its
            // armour) the later one's color wins, as it would on screen
            Map<Long, Voxel> voxels = new LinkedHashMap<>();
            Vector3f across = new Vector3f();
            Vector3f down = new Vector3f();
            Vector3f spot = new Vector3f();
            for (Face face : faces) {
                Vector3f[] c = face.corners();
                across.set(c[1]).sub(c[0]);
                down.set(c[3]).sub(c[0]);
                int columns = Math.max(1, Math.round(across.length() / cube));
                int rows = Math.max(1, Math.round(down.length() / cube));
                // Half a cube in from a model's face, toward its part's middle, so the cubes sit inside the outline
                Vector3f inward = new Vector3f();
                if (face.center() != null) {
                    inward.set(across).cross(down).normalize();
                    if (inward.dot(new Vector3f(face.center()).sub(c[0])) < 0) {
                        inward.negate();
                    }
                    inward.mul(cube / 2);
                }
                for (int column = 0; column < columns; column++) {
                    for (int row = 0; row < rows; row++) {
                        float a = (column + 0.5f) / columns;
                        float b = (row + 0.5f) / rows;
                        bilerp(c, a, b, spot);
                        spot.add(inward);
                        int color = color(face, a, b, random);
                        if (color < 0) {
                            continue;
                        }
                        long key = key(spot, cube);
                        Voxel earlier = voxels.get(key);
                        voxels.put(key, earlier == null
                                ? new Voxel(new Vector3f(spot), color, face.rotation())
                                : new Voxel(earlier.spot(), color, earlier.rotation()));
                    }
                }
            }
            for (Voxel voxel : voxels.values()) {
                Vector3f at = voxel.spot();
                double outX = at.x - centerX;
                double outZ = at.z - centerZ;
                double length = Math.max(0.05, Math.sqrt(outX * outX + outZ * outZ));
                double fling = 0.05 + random.nextDouble() * 0.12;
                Minecraft.getInstance().particleEngine.add(new DerezCubeParticle(level, at.x, at.y - cube / 2, at.z,
                        outX / length * fling + (random.nextDouble() - 0.5) * 0.04,
                        0.04 + random.nextDouble() * 0.16,
                        outZ / length * fling + (random.nextDouble() - 0.5) * 0.04,
                        cube * 0.96f, voxel.color(), new Quaternionf(voxel.rotation()),
                        Derez.releaseDelay((at.y - bottom) / height) + random.nextInt(3)));
            }
        }

        /** One of a statue's cubes before it's raised. */
        private record Voxel(Vector3f spot, int color, Quaternionf rotation) {
        }

        // The texture's color at a spot on a face, tinted, or -1 where the texture is see-through (nothing to build)
        private int color(Face face, float a, float b, RandomSource random) {
            NativeImage image = face.image();
            if (image == null) {
                return random.nextFloat() < 0.7f ? glow : SHELL;
            }
            float[][] uvs = face.uvs();
            float u = lerp(lerp(uvs[0][0], uvs[1][0], a), lerp(uvs[3][0], uvs[2][0], a), b);
            float v = lerp(lerp(uvs[0][1], uvs[1][1], a), lerp(uvs[3][1], uvs[2][1], a), b);
            int x = Math.clamp((int) (u * image.getWidth()), 0, image.getWidth() - 1);
            int y = Math.clamp((int) (v * image.getHeight()), 0, image.getHeight() - 1);
            int pixel = image.getPixel(x, y);
            if (ARGB.alpha(pixel) < 128) {
                return -1;
            }
            return (face.tint() == -1 ? pixel : ARGB.multiply(pixel, face.tint())) & 0xFFFFFF;
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

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /** The main texture a render type draws with, if it has one. */
    @Nullable
    static Identifier texture(RenderType type) {
        Map<String, ?> textures = ((RenderSetupAccessor) (Object) ((RenderTypeAccessor) type).digitalfrontier$state()).digitalfrontier$textures();
        Object binding = textures.containsKey("Sampler0") ? textures.get("Sampler0") : textures.values().stream().findFirst().orElse(null);
        return binding == null ? null : ((TextureBindingAccessor) binding).digitalfrontier$location();
    }

    /**
     * A texture's pixels: a texture held in memory (downloaded player skins) as it is, anything else read back from
     * the resource packs. Null if neither works.
     */
    @Nullable
    private static NativeImage image(Identifier texture) {
        AbstractTexture loaded = Minecraft.getInstance().getTextureManager().getTexture(texture);
        if (loaded instanceof DynamicTexture dynamic && dynamic.getPixels() != null) {
            return dynamic.getPixels();
        }
        return TEXTURES.computeIfAbsent(texture, id -> Minecraft.getInstance().getResourceManager().getResource(id).flatMap(resource -> {
            try (InputStream stream = resource.open()) {
                return Optional.of(NativeImage.read(stream));
            } catch (Exception e) {
                DigitalFrontier.LOGGER.debug("Couldn't read {} for a derez statue", id, e);
                return Optional.empty();
            }
        })).orElse(null);
    }

    /**
     * A paletted texture (an armour trim), recolored on the CPU as vanilla does on the GPU: its greyscale base image
     * mapped from its base palette to its own. Null if it can't be traced back.
     */
    @Nullable
    private static NativeImage paletted(PalettedTextureManager.Handle handle) {
        return PALETTED.computeIfAbsent(handle, ignored -> {
            PalettedTextureManagerAccessor manager = (PalettedTextureManagerAccessor) Minecraft.getInstance().getPalettedTextureManager();
            Object key = manager.digitalfrontier$handles().entrySet().stream()
                    .filter(entry -> entry.getValue() == handle).map(Map.Entry::getKey).findFirst().orElse(null);
            if (key == null) {
                return Optional.empty();
            }
            PalettedSlotKeyAccessor slot = (PalettedSlotKeyAccessor) key;
            return manager.digitalfrontier$baseTextures().getUnchecked(slot.digitalfrontier$baseTexture()).map(found -> {
                PalettedBaseTextureAccessor base = (PalettedBaseTextureAccessor) found;
                PaletteMapping mapping = manager.digitalfrontier$paletteMappings()
                        .apply(base.digitalfrontier$paletteMetadata().basePalette(), slot.digitalfrontier$paletteId());
                return base.digitalfrontier$image().mappedCopy(mapping);
            });
        }).orElse(null);
    }

    /** Forgets the textures read so far, so a resource reload's new ones are used. */
    public static void clearSkins() {
        TEXTURES.values().forEach(image -> image.ifPresent(NativeImage::close));
        TEXTURES.clear();
        PALETTED.values().forEach(image -> image.ifPresent(NativeImage::close));
        PALETTED.clear();
    }
}
