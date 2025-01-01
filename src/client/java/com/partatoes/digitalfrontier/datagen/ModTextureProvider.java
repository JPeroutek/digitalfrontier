package com.partatoes.digitalfrontier.datagen;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
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

        // Combine all futures into a single future that completes when all variants are done
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
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
        
        String emissivePatternName = String.format("%s_%s_e", templateName, color.asString().toLowerCase());
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
