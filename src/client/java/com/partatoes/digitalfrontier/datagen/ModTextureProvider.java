package com.partatoes.digitalfrontier.datagen;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonObject;
import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.block.ModBlocks;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.data.DataOutput.OutputType;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

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
    protected final FabricDataOutput dataOutput;
    private static final Logger LOGGER = LoggerFactory.getLogger(DigitalFrontier.MOD_ID);

    public ModTextureProvider(FabricDataOutput output) {
        this.dataOutput = output;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
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
        ModBlocks.GLITCHED_GRIDSTONE.keySet().forEach(color ->
                futures.add(CompletableFuture.runAsync(() -> createGlitched(writer, color))));

        // Combine all futures into a single future that completes when all variants are done
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private static final int STATIC_FRAMES = 8;
    private static final int GLITCH_FRAMES = 4;
    private static final String GLITCH_TEMPLATE = "gridstone_pattern_thick_ring";

    // TV static: gray noise with brighter scan lines; the brightest specks also go in the glow layer
    private void createStatic(DataWriter writer) {
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
        writeAnimated(writer, image, "static_block", 1);
        writeAnimated(writer, glow, "static_block_glow", 1);
    }

    // A pattern whose rows jump sideways from frame to frame, in the given color
    private void createGlitched(DataWriter writer, DyeColor color) {
        BufferedImage template = readTemplate(GLITCH_TEMPLATE);
        if (template == null) {
            return;
        }
        Random random = new Random(0x611C7L);
        BufferedImage image = new BufferedImage(16, 16 * GLITCH_FRAMES, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(16, 16 * GLITCH_FRAMES, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < GLITCH_FRAMES; frame++) {
            for (int y = 0; y < 16; y++) {
                int shift = random.nextFloat() < 0.4f ? random.nextInt(9) - 4 : 0;
                int sourceY = random.nextFloat() < 0.15f ? random.nextInt(16) : y;
                for (int x = 0; x < 16; x++) {
                    int pixel = template.getRGB(Math.floorMod(x - shift, 16), sourceY);
                    boolean line = pixel == -1;
                    int base = line ? applyAlpha(color.getSignColor(), 0xFF) : applyAlpha(pixel, 0xFF);
                    image.setRGB(x, frame * 16 + y, base);
                    glow.setRGB(x, frame * 16 + y, line ? base : 0);
                }
            }
        }
        String name = "glitched_gridstone_" + color.asString();
        writeAnimated(writer, image, name, 3);
        writeAnimated(writer, glow, name + "_glow", 3);
    }

    private void writeAnimated(DataWriter writer, BufferedImage image, String assetName, int frameTime) {
        writeBufferedImageToPath(writer, image, assetName);
        JsonObject animation = new JsonObject();
        animation.addProperty("frametime", frameTime);
        JsonObject meta = new JsonObject();
        meta.add("animation", animation);
        DataOutput.PathResolver resolver = dataOutput.getResolver(OutputType.RESOURCE_PACK, "textures/block");
        DataProvider.writeToPath(writer, meta, resolver.resolve(Identifier.of(DigitalFrontier.MOD_ID, assetName), "png.mcmeta")).join();
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

    private void createVariant(DataWriter writer, String templateName, DyeColor color) {
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
                    coloredImage.setRGB(x, y, applyAlpha(color.getSignColor(), 0xFF));
                    emissiveImage.setRGB(x, y, applyAlpha(color.getSignColor(), 0xFF));
                } else {
                    coloredImage.setRGB(x, y, applyAlpha(pixel, 0xFF));
                    emissiveImage.setRGB(x, y, 0x00000000);
                }
            }
        }

        // Write the image to the path
        String coloredPatternName = String.format("%s_%s", templateName, color.asString().toLowerCase());
        writeBufferedImageToPath(writer, coloredImage, coloredPatternName);
        
        String emissivePatternName = String.format("%s_%s_glow", templateName, color.asString().toLowerCase());
        writeBufferedImageToPath(writer, emissiveImage, emissivePatternName);
    }

    private int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private void writeBufferedImageToPath(DataWriter writer, BufferedImage image, String assetName) {
        DataOutput.PathResolver resolver = dataOutput.getResolver(OutputType.RESOURCE_PACK, "textures/block");
        Path newPath = resolver.resolve(Identifier.of(DigitalFrontier.MOD_ID, assetName), "png");

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            HashingOutputStream hashingOutputStream = new HashingOutputStream(Hashing.sha1(), byteArrayOutputStream);

            ImageIO.write(image, "png", hashingOutputStream);

            writer.write(newPath, byteArrayOutputStream.toByteArray(), hashingOutputStream.hash());
        } catch (IOException var10) {
            LOGGER.error("Failed to save file to {}", assetName, var10);
        }
    }

    @Override
    public String getName() {
        return "Textures";
    }
}
