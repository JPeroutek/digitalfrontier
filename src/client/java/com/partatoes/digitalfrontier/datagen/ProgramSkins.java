package com.partatoes.digitalfrontier.datagen;

import java.awt.image.BufferedImage;

/**
 * Placeholder program skins, painted in code on the 64×64 player layout: a dark suit with glowing seams, a visor
 * across the helmet, and an emblem on the chest. Each {@link Look} picks the glow color, the emblem and the
 * helmet. They're stand-ins until real art exists; the overlay layers are left empty.
 */
final class ProgramSkins {
    enum Emblem { DIAMOND, RING, BARS, CHEVRON, CROSS }

    /**
     * @param faceplate a closed helmet with a vertical slit, instead of a visor band
     * @param shoulders lit shoulder tops
     */
    record Look(int glow, Emblem emblem, boolean faceplate, boolean shoulders) {
    }

    static final Look CYAN = new Look(0x4FE3FF, Emblem.DIAMOND, false, false);
    static final Look WHITE = new Look(0xE8F4FF, Emblem.RING, false, false);
    static final Look BLUE = new Look(0x7FB6FF, Emblem.BARS, false, false);
    static final Look SOVEREIGN = new Look(0xFFB000, Emblem.CHEVRON, false, true);
    static final Look WARDEN = new Look(0xFF6A00, Emblem.CROSS, true, false);

    private static final int SUIT = 0xFF1A1E24;
    private static final int SUIT_LIGHT = 0xFF242A32;
    private static final int SUIT_DARK = 0xFF121519;

    private ProgramSkins() {
    }

    static BufferedImage paint(Look look) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        int glow = 0xFF000000 | look.glow();
        // Head, body, then right and left arm and leg, at the player layout's base-layer offsets
        box(image, 0, 0, 8, 8, 8, glow, Part.HEAD, look);
        box(image, 16, 16, 8, 12, 4, glow, Part.BODY, look);
        box(image, 40, 16, 4, 12, 4, glow, Part.ARM, look);
        box(image, 32, 48, 4, 12, 4, glow, Part.ARM, look);
        box(image, 0, 16, 4, 12, 4, glow, Part.LEG, look);
        box(image, 16, 48, 4, 12, 4, glow, Part.LEG, look);
        return image;
    }

    private enum Part { HEAD, BODY, ARM, LEG }

    // A cuboid's six faces, unfolded the way the model maps them: top and bottom above, then right, front, left, back
    private static void box(BufferedImage image, int u, int v, int w, int h, int d, int glow, Part part, Look look) {
        fill(image, u + d, v, w, d, SUIT_LIGHT);
        fill(image, u + d + w, v, w, d, SUIT_DARK);
        fill(image, u, v + d, d, h, SUIT);
        fill(image, u + d, v + d, w, h, SUIT);
        fill(image, u + d + w, v + d, d, h, SUIT);
        fill(image, u + d + d + w, v + d, w, h, SUIT);
        int sideY = v + d;
        switch (part) {
            case HEAD -> {
                if (look.faceplate()) {
                    // A closed plate split by one lit slit, on the front only
                    vLine(image, u + d + w / 2, sideY + 1, h - 2, glow);
                } else {
                    // A visor band all the way round
                    hLine(image, u, sideY + 3, 2 * (d + w), glow);
                }
                // A ridge over the crown
                vLine(image, u + d + w / 2, v, d, glow);
            }
            case BODY -> {
                // Seams down both sides, a belt, and the emblem on the chest
                vLine(image, u + d / 2, sideY, h, glow);
                vLine(image, u + d + w + d / 2, sideY, h, glow);
                hLine(image, u, sideY + 8, 2 * (d + w), glow);
                emblem(image, u + d, sideY + 1, look.emblem(), glow);
            }
            case ARM, LEG -> {
                // A seam down the outside and front, and a lit cuff
                vLine(image, u + d / 2, sideY, h, glow);
                vLine(image, u + d + w / 2, sideY, h, glow);
                hLine(image, u, sideY + h - 1, 2 * (d + w), glow);
                if (part == Part.ARM && look.shoulders()) {
                    fill(image, u + d, v, w, d, glow);
                }
            }
        }
    }

    // On the 8-wide, 6-tall chest area starting at (x, y)
    private static void emblem(BufferedImage image, int x, int y, Emblem emblem, int glow) {
        switch (emblem) {
            case DIAMOND -> {
                set(image, x + 3, y + 1, glow);
                set(image, x + 4, y + 1, glow);
                set(image, x + 2, y + 2, glow);
                set(image, x + 5, y + 2, glow);
                set(image, x + 2, y + 3, glow);
                set(image, x + 5, y + 3, glow);
                set(image, x + 3, y + 4, glow);
                set(image, x + 4, y + 4, glow);
            }
            case RING -> {
                hLine(image, x + 2, y + 1, 4, glow);
                hLine(image, x + 2, y + 4, 4, glow);
                vLine(image, x + 2, y + 1, 4, glow);
                vLine(image, x + 5, y + 1, 4, glow);
            }
            case BARS -> {
                hLine(image, x + 1, y + 1, 6, glow);
                hLine(image, x + 1, y + 3, 6, glow);
            }
            case CHEVRON -> {
                for (int i = 0; i < 4; i++) {
                    set(image, x + i, y + 1 + i, glow);
                    set(image, x + 7 - i, y + 1 + i, glow);
                }
            }
            case CROSS -> {
                for (int i = 0; i < 5; i++) {
                    set(image, x + 1 + i, y + i, glow);
                    set(image, x + 6 - i, y + i, glow);
                }
            }
        }
    }

    private static void fill(BufferedImage image, int x, int y, int w, int h, int color) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                image.setRGB(x + dx, y + dy, color);
            }
        }
    }

    private static void hLine(BufferedImage image, int x, int y, int length, int color) {
        fill(image, x, y, length, 1, color);
    }

    private static void vLine(BufferedImage image, int x, int y, int length, int color) {
        fill(image, x, y, 1, length, color);
    }

    private static void set(BufferedImage image, int x, int y, int color) {
        image.setRGB(x, y, color);
    }
}
