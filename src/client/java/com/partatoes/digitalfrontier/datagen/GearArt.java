package com.partatoes.digitalfrontier.datagen;

import java.awt.image.BufferedImage;

/**
 * Placeholder art for the gear (see the gear items in {@code ModItems}): small glowing pixel sprites, painted in code
 * like the program skins, in the grid's cyan with a dark core.
 */
final class GearArt {
    private static final int CYAN = 0xFF4FE3FF;
    private static final int PALE = 0xFFCFF8FF;
    private static final int DARK = 0xFF14191E;
    private static final int STEEL = 0xFF4A5560;
    private static final int ORANGE = 0xFFFF8A1E;

    private GearArt() {
    }

    /** The Light Staff: a long rod, corner to corner, lit at both ends. */
    static BufferedImage staff() {
        BufferedImage image = blank(16);
        for (int i = 1; i < 15; i++) {
            set(image, i, 15 - i, DARK);
            set(image, i + 1, 15 - i, i < 4 || i > 11 ? CYAN : STEEL);
        }
        set(image, 2, 13, PALE);
        set(image, 14, 1, PALE);
        return image;
    }

    /** The Light Grenade: a dark sphere with a glowing ring and a bright spark. */
    static BufferedImage grenade() {
        BufferedImage image = blank(16);
        disc(image, 8, 9, 5, DARK);
        ring(image, 8, 9, 5, CYAN);
        for (int x = 4; x <= 12; x++) {
            set(image, x, 9, CYAN);
        }
        set(image, 8, 3, PALE);
        set(image, 8, 4, STEEL);
        return image;
    }

    /** The Force Shield: a hexagon of light around a dark face. */
    static BufferedImage shield() {
        BufferedImage image = blank(16);
        for (int y = 2; y < 14; y++) {
            int inset = Math.max(0, Math.abs(y - 8) - 3);
            for (int x = 3 + inset; x <= 12 - inset; x++) {
                boolean edge = x == 3 + inset || x == 12 - inset || y == 2 || y == 13;
                set(image, x, y, edge ? CYAN : DARK);
            }
        }
        set(image, 7, 7, PALE);
        set(image, 8, 8, PALE);
        return image;
    }

    /** The Chute, folded: a pack with straps and a glowing seam. */
    static BufferedImage chute() {
        BufferedImage image = blank(16);
        for (int y = 3; y < 14; y++) {
            for (int x = 4; x < 12; x++) {
                set(image, x, y, x == 4 || x == 11 || y == 3 || y == 13 ? STEEL : DARK);
            }
        }
        for (int y = 4; y < 13; y++) {
            set(image, 8, y, CYAN);
        }
        set(image, 6, 2, STEEL);
        set(image, 9, 2, STEEL);
        return image;
    }

    /** Overclock: a vial of glowing orange code. */
    static BufferedImage overclock() {
        BufferedImage image = blank(16);
        for (int y = 5; y < 14; y++) {
            for (int x = 5; x < 11; x++) {
                boolean edge = x == 5 || x == 10 || y == 13;
                set(image, x, y, edge ? PALE : y > 7 ? ORANGE : DARK);
            }
        }
        for (int x = 6; x < 10; x++) {
            set(image, x, 4, STEEL);
            set(image, x, 3, STEEL);
        }
        set(image, 7, 9, PALE);
        return image;
    }

    /**
     * The Chute's canopy, as the elytra-shaped "wings" equipment layer (64×32, the elytra's layout): a translucent
     * cyan sail with bright ribs.
     */
    static BufferedImage canopy() {
        BufferedImage image = blank(64, 32);
        for (int y = 0; y < 32; y++) {
            for (int x = 22; x < 46; x++) {
                int color = (x - 22) % 6 == 0 || y % 8 == 0 ? CYAN : 0x9F2A7F99;
                set(image, x, y, color);
            }
        }
        return image;
    }

    /** The Recognizer's hull: dark plating with red seams, and a bright eye panel (128×64, the model's layout). */
    static BufferedImage recognizer() {
        BufferedImage image = blank(128, 64);
        int hull = 0xFF1C1F24;
        int seam = 0xFFFF3030;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 128; x++) {
                set(image, x, y, x % 8 == 0 || y % 8 == 0 ? seam : hull);
            }
        }
        // The eye, under the beam
        for (int y = 40; y < 46; y++) {
            for (int x = 28; x < 48; x++) {
                set(image, x, y, 0xFFFF6A6A);
            }
        }
        return image;
    }

    /** A Fading Barrier block: a see-through pane of light with a bright frame. */
    static BufferedImage fadingBarrier() {
        BufferedImage image = blank(16);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean frame = x == 0 || y == 0 || x == 15 || y == 15;
                set(image, x, y, frame ? CYAN : (x + y) % 4 == 0 ? 0x904FE3FF : 0x504FE3FF);
            }
        }
        return image;
    }

    /** An arena duel tile: a dark pane edged in light, cyan, or red when it's about to fall. */
    static BufferedImage arenaTile(boolean warning) {
        BufferedImage image = blank(16);
        int edge = warning ? 0xFFFF3030 : CYAN;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean rim = x == 0 || y == 0 || x == 15 || y == 15;
                boolean inner = x == 3 || y == 3 || x == 12 || y == 12;
                set(image, x, y, rim ? edge : inner ? (warning ? 0xFF7A1818 : 0xFF1E6F7F) : DARK);
            }
        }
        return image;
    }

    /** The arena console: a panel of glowing keys. */
    static BufferedImage arenaConsole() {
        BufferedImage image = blank(16);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean key = x % 3 == 1 && y % 3 == 1 && x > 0 && y > 3;
                set(image, x, y, x == 0 || y == 0 || x == 15 || y == 15 ? STEEL : key ? (x + y) % 2 == 0 ? CYAN : 0xFFFFD24F : DARK);
            }
        }
        for (int x = 2; x < 14; x++) {
            set(image, x, 2, PALE);
        }
        return image;
    }

    /** A trophy block: gold, with a disc of light in the middle. */
    static BufferedImage trophy() {
        BufferedImage image = blank(16);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                set(image, x, y, x == 0 || y == 0 || x == 15 || y == 15 ? 0xFFB8860B : 0xFFFFD24F);
            }
        }
        disc(image, 8, 8, 4, DARK);
        ring(image, 8, 8, 4, CYAN);
        return image;
    }

    private static BufferedImage blank(int size) {
        return blank(size, size);
    }

    private static BufferedImage blank(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private static void set(BufferedImage image, int x, int y, int color) {
        if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
            image.setRGB(x, y, color);
        }
    }

    private static void disc(BufferedImage image, int cx, int cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x * x + y * y <= radius * radius) {
                    set(image, cx + x, cy + y, color);
                }
            }
        }
    }

    private static void ring(BufferedImage image, int cx, int cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                int d = x * x + y * y;
                if (d <= radius * radius && d > (radius - 1) * (radius - 1)) {
                    set(image, cx + x, cy + y, color);
                }
            }
        }
    }
}
