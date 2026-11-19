package com.partatoes.digitalfrontier.datagen;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Placeholder program skins, painted in code on the 64×64 player layout: a dark suit with glowing seams, a visor
 * across the helmet, and an emblem on the chest. Each {@link Look} picks the glow color, the emblem and the
 * helmet. They're stand-ins until real art exists; the overlay layers are left empty.
 */
final class ProgramSkins {
    enum Emblem { DIAMOND, RING, BARS, CHEVRON, CROSS, HORN }

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
    static final Look RECRUITER = new Look(0xFF2A2A, Emblem.HORN, false, true);
    // A program near the end of its drift into decay
    static final Look FRAYING = new Look(0xFF8A1E, Emblem.DIAMOND, false, false);
    // Job programs
    static final Look MECHANIC = new Look(0x9BFF4F, Emblem.BARS, false, true);
    static final Look PORTAL_KEEPER = new Look(0xE2CCFF, Emblem.RING, true, false);
    // Redeemed lieutenants: the same suits, their circuitry gone white
    static final Look SOVEREIGN_RESTORED = new Look(0xF4FBFF, Emblem.CHEVRON, false, true);
    static final Look WARDEN_RESTORED = new Look(0xF4FBFF, Emblem.CROSS, true, false);

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

    /**
     * A Bool's frame (see BoolModel, whose cubes are 5 and 7 pixels a side, at V 0 and 12), drawn cut out: every
     * face's rim and a point at its centre in its glow, the rest open. Its panes are a separate texture.
     */
    static BufferedImage paintBool(int glow) {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        int rim = 0xFF000000 | glow;
        boolFaces((x, y, size) -> {
            fill(image, x, y, size, size, rim);
            fill(image, x + 1, y + 1, size - 2, size - 2, 0);
            fill(image, x + size / 2, y + size / 2, 1, 1, rim);
        });
        return image;
    }

    /**
     * The panes filling a Bool's faces, drawn over its frame by a pass that doesn't write depth: a faint, mostly
     * see-through tint of its glow inside each rim, so every face shows through every other whatever order they're
     * drawn in.
     */
    static BufferedImage paintBoolPanes(int glow) {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        boolFaces((x, y, size) -> fill(image, x + 1, y + 1, size - 2, size - 2, 0x48000000 | glow));
        return image;
    }

    /**
     * A Bool's halo, drawn around it the same way: a faint wash of its glow, strongest at each face's centre and
     * fading toward the edges, so the enlarged copy reads as light rather than a second solid.
     */
    static BufferedImage paintBoolHalo(int glow) {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        boolFaces((x, y, size) -> {
            fill(image, x, y, size, size, 0x0C000000 | glow);
            fill(image, x + 1, y + 1, size - 2, size - 2, 0x1C000000 | glow);
            fill(image, x + 2, y + 2, size - 4, size - 4, 0x30000000 | glow);
        });
        return image;
    }

    @FunctionalInterface
    private interface FacePainter {
        void paint(int x, int y, int size);
    }

    // Every face of the Bool's two cubes (size, V), in the standard box layout: top and bottom, then the four sides
    private static void boolFaces(FacePainter painter) {
        for (int[] cube : new int[][]{{5, 0}, {7, 12}}) {
            int size = cube[0];
            int v = cube[1];
            painter.paint(size, v, size);
            painter.paint(2 * size, v, size);
            for (int side = 0; side < 4; side++) {
                painter.paint(side * size, v + size, size);
            }
        }
    }

    /**
     * The mask a derezzing body dissolves through (see the derez mixin): 2×2-texel cells, each with its own alpha. A
     * cell vanishes once the body's fading tint drops below its alpha, so the skin comes apart in random squares.
     */
    static BufferedImage paintDerezMask() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        java.util.Random random = new java.util.Random(0xDE7E2L);
        for (int x = 0; x < 64; x += 2) {
            for (int y = 0; y < 64; y += 2) {
                int alpha = 13 + random.nextInt(230);
                fill(image, x, y, 2, 2, alpha << 24 | 0xFFFFFF);
            }
        }
        return image;
    }

    /** A derez cube's faces: white (tinted by each cube's color), with a brighter rim so the edges read as a cube. */
    static BufferedImage paintDerezCube() {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        fill(image, 0, 0, 8, 8, 0xFFFFFFFF);
        fill(image, 1, 1, 6, 6, 0xFFB8B8B8);
        return image;
    }

    /** The Disinfection Pad's block texture: a dark tile, a bright rim, and a glowing cross. */
    static BufferedImage paintPad() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int glow = 0xFF7FFFD4;
        fill(image, 0, 0, 16, 16, 0xFF0B1418);
        hLine(image, 0, 0, 16, glow);
        hLine(image, 0, 15, 16, glow);
        vLine(image, 0, 0, 16, glow);
        vLine(image, 15, 0, 16, glow);
        fill(image, 6, 3, 4, 10, glow);
        fill(image, 3, 6, 10, 4, glow);
        fill(image, 7, 4, 2, 8, 0xFFFFFFFF);
        fill(image, 4, 7, 8, 2, 0xFFFFFFFF);
        return image;
    }

    /**
     * The System Core's texture (64×128, see SystemCoreModel): dark cells with red rims, a few lit through, laid over the
     * whole sheet so every face of its three blocks shows the same circuitry.
     */
    static BufferedImage paintCore() {
        BufferedImage image = new BufferedImage(64, 128, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(0xC0AE);
        for (int cellX = 0; cellX < 64; cellX += 4) {
            for (int cellY = 0; cellY < 128; cellY += 4) {
                boolean lit = random.nextInt(6) == 0;
                fill(image, cellX, cellY, 4, 4, 0xFFB01818);
                fill(image, cellX + 1, cellY + 1, 2, 2, lit ? 0xFFFF6040 : 0xFF140606);
            }
        }
        return image;
    }

    /** The core seal's block texture: a dark plate crossed by red bars, its rim lit. */
    static BufferedImage paintSeal() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(image, 0, 0, 16, 16, 0xFF120606);
        hLine(image, 0, 0, 16, 0xFFFF3030);
        hLine(image, 0, 15, 16, 0xFFFF3030);
        vLine(image, 0, 0, 16, 0xFFFF3030);
        vLine(image, 15, 0, 16, 0xFFFF3030);
        for (int i = 2; i < 14; i++) {
            set(image, i, i, 0xFFD02020);
            set(image, 15 - i, i, 0xFFD02020);
        }
        fill(image, 6, 6, 4, 4, 0xFFFF6040);
        return image;
    }

    private static int scale(int rgb, double factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        return r << 16 | g << 8 | b;
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
            // A loudhailer: a mouthpiece widening into a flared bell
            case HORN -> {
                vLine(image, x + 1, y + 2, 2, glow);
                vLine(image, x + 2, y + 2, 2, glow);
                vLine(image, x + 3, y + 1, 4, glow);
                vLine(image, x + 4, y + 1, 4, glow);
                vLine(image, x + 5, y, 6, glow);
                vLine(image, x + 6, y, 6, glow);
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
