package com.partatoes.digitalfrontier.datagen;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModTextureProvider implements DataProvider {
    protected final FabricPackOutput dataOutput;
    private static final Logger LOGGER = LoggerFactory.getLogger(DigitalFrontier.MOD_ID);

    public ModTextureProvider(FabricPackOutput output) {
        this.dataOutput = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput writer) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        ModBlocks.GRIDSTONE_PATTERNS_AND_COLORS
                .forEach((patternName, dyeBlockMap) -> {
                    dyeBlockMap.forEach((color, block) -> {
                        futures.add(CompletableFuture.runAsync(() -> {
                            createVariant(writer, "gridstone_pattern_" + patternName, color);
                        }));
                    });
                });

        // Corruption blocks: animated, so each texture is a vertical strip of frames plus an .mcmeta
        futures.add(CompletableFuture.runAsync(() -> createStatic(writer)));
        ModBlocks.GLITCHED_GRIDSTONE.keySet().forEach(color -> {
            for (int template = 0; template < GLITCH_TEMPLATES.length; template++) {
                int glitch = template;
                futures.add(CompletableFuture.runAsync(() -> createGlitched(writer, color, glitch)));
            }
        });
        for (int i = 0; i < CORRUPTED_PROGRAM_SOURCES.length; i++) {
            int variant = i;
            futures.add(CompletableFuture.runAsync(() -> createCorruptedProgram(writer, variant)));
        }

        // Combine all futures into a single future that completes when all variants are done
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private static final int STATIC_FRAMES = 8;
    private static final int GLITCH_FRAMES = 4;
    // The patterns glitched gridstone is made from, each glitched its own way; the block picks one by position
    static final String[] GLITCH_TEMPLATES = {
            "thick_ring", "cross", "curved_t", "checker_4x4", "exchanger", "border_3", "center_stripe", "single_entry_circle",
    };

    // Template 0 keeps the plain name
    static String templateSuffix(int template) {
        return template == 0 ? "" : "_t" + template;
    }
    // Ticks lit and out, in turn: mostly lit, dropping out for a tick or two every few seconds
    private static final int[][] FLICKER_TIMINGS = {
            {90, 2, 4, 1, 150, 3},
            {130, 1, 60, 2, 3, 2},
    };
    /**
     * Every block showing an animated texture shows the same frame at the same time, so each animation is written
     * several times, each copy started at a different point in its cycle; the block state picks a copy by position
     * (see ModModelProvider). Copy 0 keeps the plain name, the others get {@link #phaseSuffix}.
     */
    static final int ANIMATION_PHASES = 4;
    static final int FLICKER_PHASES = 2;

    static String phaseSuffix(int phase) {
        return phase == 0 ? "" : "_p" + phase;
    }

    // TV static: gray noise with brighter scan lines; the brightest specks also go in the glow layer
    private void createStatic(CachedOutput writer) {
        Random random = new Random(0x57A71CL);
        BufferedImage image = new BufferedImage(16, 16 * STATIC_FRAMES, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(16, 16 * STATIC_FRAMES, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < STATIC_FRAMES; frame++) {
            int scanLine = random.nextInt(16);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int gray = 0x18 + random.nextInt(0xB0) + (y == scanLine ? 0x30 : 0);
                    gray = Math.min(gray, 0xFF);
                    int pixel = 0xFF000000 | gray << 16 | gray << 8 | gray;
                    image.setRGB(x, frame * 16 + y, pixel);
                    glow.setRGB(x, frame * 16 + y, gray > 0xB8 ? pixel : 0);
                }
            }
        }
        writePhases(writer, image, glow, "static_block", 1, ANIMATION_PHASES);
    }

    // A pattern whose rows jump sideways from frame to frame, in the given color
    private void createGlitched(CachedOutput writer, DyeColor color, int templateIndex) {
        BufferedImage template = readTemplate("gridstone_pattern_" + GLITCH_TEMPLATES[templateIndex]);
        if (template == null) {
            return;
        }
        // The same glitches in every color, different ones for each pattern
        Random random = new Random(0x611C7L + templateIndex * 7919L);
        BufferedImage image = new BufferedImage(16, 16 * GLITCH_FRAMES, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(16, 16 * GLITCH_FRAMES, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < GLITCH_FRAMES; frame++) {
            for (int y = 0; y < 16; y++) {
                int shift = random.nextFloat() < 0.4f ? random.nextInt(9) - 4 : 0;
                int sourceY = random.nextFloat() < 0.15f ? random.nextInt(16) : y;
                for (int x = 0; x < 16; x++) {
                    int pixel = template.getRGB(Math.floorMod(x - shift, 16), sourceY);
                    boolean line = pixel == -1;
                    int base = line ? applyAlpha(color.getTextColor(), 0xFF) : applyAlpha(pixel, 0xFF);
                    image.setRGB(x, frame * 16 + y, base);
                    glow.setRGB(x, frame * 16 + y, line ? base : 0);
                }
            }
        }
        writePhases(writer, image, glow, "glitched_gridstone_" + color.getSerializedName() + templateSuffix(templateIndex), 3, ANIMATION_PHASES);
    }

    // Program skins that corrupted programs are made from
    private static final String[] CORRUPTED_PROGRAM_SOURCES = {"quorra64", "sam64", "zuse64"};

    /**
     * A program skin gone wrong: its glowing circuit lines (bright and blue-ish) turn the regime's red-orange,
     * everything else loses most of its color, and scattered pixels become static.
     */
    private void createCorruptedProgram(CachedOutput writer, int variant) {
        String path = String.format("assets/%s/textures/entity/%s.png", DigitalFrontier.MOD_ID, CORRUPTED_PROGRAM_SOURCES[variant]);
        BufferedImage source;
        try (var stream = ModTextureProvider.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                LOGGER.error("Could not find resource at {}", path);
                return;
            }
            source = ImageIO.read(stream);
        } catch (IOException e) {
            LOGGER.error("Failed to load image from {}", path, e);
            return;
        }
        Random random = new Random(0xC0DEL + variant);
        BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int pixel = source.getRGB(x, y);
                int alpha = pixel >>> 24;
                int r = pixel >> 16 & 0xFF;
                int g = pixel >> 8 & 0xFF;
                int b = pixel & 0xFF;
                int result;
                if (alpha == 0) {
                    result = pixel;
                } else if (random.nextFloat() < 0.08f) {
                    int gray = 0x30 + random.nextInt(0xB0);
                    result = alpha << 24 | gray << 16 | gray << 8 | gray;
                } else if (Math.max(Math.max(r, g), b) > 150 && b >= r) {
                    int bright = Math.max(Math.max(r, g), b);
                    result = alpha << 24 | bright << 16 | (bright * 3 / 10) << 8 | (bright / 10);
                } else {
                    int gray = (r * 3 + g * 6 + b) / 10;
                    result = alpha << 24 | mix(r, gray) << 16 | mix(g, gray) << 8 | mix(b, gray);
                }
                image.setRGB(x, y, result);
            }
        }
        PackOutput.PathProvider resolver = dataOutput.createPathProvider(Target.RESOURCE_PACK, "textures/entity");
        writeImage(writer, image, resolver.file(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "corrupted_program_" + variant), "png"));
    }

    // Mostly gray, with a little of the original color left
    private static int mix(int channel, int gray) {
        return (channel + gray * 3) / 4;
    }

    private void writeImage(CachedOutput writer, BufferedImage image, Path path) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            HashingOutputStream hashing = new HashingOutputStream(Hashing.sha1(), bytes);
            ImageIO.write(image, "png", hashing);
            writer.writeIfNeeded(path, bytes.toByteArray(), hashing.hash());
        } catch (IOException e) {
            LOGGER.error("Failed to save file to {}", path, e);
        }
    }

    private void writeAnimated(CachedOutput writer, BufferedImage image, String assetName, int frameTime) {
        writeBufferedImageToPath(writer, image, assetName);
        JsonObject animation = new JsonObject();
        animation.addProperty("frametime", frameTime);
        JsonObject meta = new JsonObject();
        meta.add("animation", animation);
        PackOutput.PathProvider resolver = dataOutput.createPathProvider(Target.RESOURCE_PACK, "textures/block");
        DataProvider.saveStable(writer, meta, resolver.file(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, assetName), "png.mcmeta")).join();
    }

    // An animation and its glow layer, once per phase, each copy's frames starting further into the cycle
    private void writePhases(CachedOutput writer, BufferedImage image, BufferedImage glow, String name, int frameTime, int phases) {
        int frames = image.getHeight() / 16;
        for (int phase = 0; phase < phases; phase++) {
            int shift = phase * frames / phases;
            writeAnimated(writer, shiftFrames(image, shift), name + phaseSuffix(phase), frameTime);
            writeAnimated(writer, shiftFrames(glow, shift), name + phaseSuffix(phase) + "_glow", frameTime);
        }
    }

    // A vertical strip of 16×16 frames, starting at frame {@code shift} and wrapping round
    private static BufferedImage shiftFrames(BufferedImage strip, int shift) {
        int frames = strip.getHeight() / 16;
        BufferedImage shifted = new BufferedImage(16, strip.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < frames; frame++) {
            int source = (frame + shift) % frames;
            shifted.setRGB(0, frame * 16, 16, 16, strip.getRGB(0, source * 16, 16, 16, null, 0, 16), 0, 16);
        }
        return shifted;
    }

    // A two-frame strip shown in turn, frame 0 then frame 1, for each of the times, starting at times[start]
    private void writeAnimated(CachedOutput writer, BufferedImage image, String assetName, int[] times, int start) {
        writeBufferedImageToPath(writer, image, assetName);
        JsonArray frames = new JsonArray();
        for (int i = 0; i < times.length; i++) {
            int entry = (start + i) % times.length;
            JsonObject frame = new JsonObject();
            frame.addProperty("index", entry % 2);
            frame.addProperty("time", times[entry]);
            frames.add(frame);
        }
        JsonObject animation = new JsonObject();
        animation.add("frames", frames);
        JsonObject meta = new JsonObject();
        meta.add("animation", animation);
        PackOutput.PathProvider resolver = dataOutput.createPathProvider(Target.RESOURCE_PACK, "textures/block");
        DataProvider.saveStable(writer, meta, resolver.file(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, assetName), "png.mcmeta")).join();
    }

    private BufferedImage readTemplate(String templateName) {
        String resourcePath = String.format("assets/%s/textures/block/%s.png", DigitalFrontier.MOD_ID, templateName);
        try (var resourceStream = ModTextureProvider.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (resourceStream == null) {
                LOGGER.error("Could not find resource at {}", resourcePath);
                return null;
            }
            return ImageIO.read(resourceStream);
        } catch (IOException e) {
            LOGGER.error("Failed to load image from {}", resourcePath, e);
            return null;
        }
    }

    private void createVariant(CachedOutput writer, String templateName, DyeColor color) {
        // Read the image from the resources folder
        String resourcePath = String.format("assets/%s/textures/block/%s.png", DigitalFrontier.MOD_ID, templateName);
        BufferedImage templateImage;
        try {
            // Use getResourceAsStream to load from the resources folder
            var resourceStream = ModTextureProvider.class.getClassLoader().getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                LOGGER.error("Could not find resource at {}", resourcePath);
                return;
            }
            templateImage = ImageIO.read(resourceStream);
        } catch (IOException e) {
            LOGGER.error("Failed to load image from {}", resourcePath, e);
            return;
        }

        // Create the colored variant by making only pixels matching the dye color white
        BufferedImage coloredImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        BufferedImage emissiveImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < templateImage.getWidth(); x++) {
            for (int y = 0; y < templateImage.getHeight(); y++) {
                int pixel = templateImage.getRGB(x, y);
                if (pixel == -1) {
                    coloredImage.setRGB(x, y, applyAlpha(color.getTextColor(), 0xFF));
                    emissiveImage.setRGB(x, y, applyAlpha(color.getTextColor(), 0xFF));
                } else {
                    coloredImage.setRGB(x, y, applyAlpha(pixel, 0xFF));
                    emissiveImage.setRGB(x, y, 0x00000000);
                }
            }
        }

        // Write the image to the path
        String coloredPatternName = String.format("%s_%s", templateName, color.getSerializedName().toLowerCase());
        writeBufferedImageToPath(writer, coloredImage, coloredPatternName);
        
        String emissivePatternName = String.format("%s_%s_glow", templateName, color.getSerializedName().toLowerCase());
        writeBufferedImageToPath(writer, emissiveImage, emissivePatternName);

        // Flickering glows for lightly corrupted ground: lit, then out for a moment (see PatternBlock)
        BufferedImage flickerImage = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
        // Copy pixels directly: drawing through AWT Graphics needs a display, which CI doesn't have
        flickerImage.setRGB(0, 0, 16, 16, emissiveImage.getRGB(0, 0, 16, 16, null, 0, 16), 0, 16);
        for (int i = 0; i < FLICKER_TIMINGS.length; i++) {
            int[] times = FLICKER_TIMINGS[i];
            for (int phase = 0; phase < FLICKER_PHASES; phase++) {
                writeAnimated(writer, flickerImage, emissivePatternName + "_flicker_" + (i + 1) + phaseSuffix(phase), times,
                        phase * times.length / FLICKER_PHASES);
            }
        }
    }

    private int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private void writeBufferedImageToPath(CachedOutput writer, BufferedImage image, String assetName) {
        PackOutput.PathProvider resolver = dataOutput.createPathProvider(Target.RESOURCE_PACK, "textures/block");
        Path newPath = resolver.file(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, assetName), "png");

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            HashingOutputStream hashingOutputStream = new HashingOutputStream(Hashing.sha1(), byteArrayOutputStream);

            ImageIO.write(image, "png", hashingOutputStream);

            writer.writeIfNeeded(newPath, byteArrayOutputStream.toByteArray(), hashingOutputStream.hash());
        } catch (IOException var10) {
            LOGGER.error("Failed to save file to {}", assetName, var10);
        }
    }

    @Override
    public String getName() {
        return "Textures";
    }
}
