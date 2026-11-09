// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

/**
 * Renders the static office room background, wall gallery, floor plants, and workstation
 * directly from embedded Flash (.rodata) sprite tables with zero heap allocation.
 */
public final class RoomScene {

    public static final int SCALE = 3;
    public static final int TILE_W = 16 * SCALE;
    public static final int CHAR_H = 32 * SCALE;

    public static final int DESK_X = 256;
    public static final int DESK_Y = 48;
    public static final int CHAIR_X = DESK_X - TILE_W;
    public static final int CHAR_Y = DESK_Y + TILE_W;
    public static final int LEFT_BOUND_X = 64;

    public static final int FLOOR_PALETTE_INDEX = 1;
    private static final int WALL_COLOR = 0x52AD;
    private static final int BASEBOARD_COLOR = 0x3164;

    private RoomScene() {
    }

    /**
     * Draws the entire static office room onto the display.
     */
    public static void draw(WaveshareTftShield tft, Palette palette) {
        var floorColor = palette.getColor(FLOOR_PALETTE_INDEX);

        // 1. Top wall, wooden baseboard, and floor background
        tft.fillRect(0, 0, WaveshareTftShield.WIDTH, 66, WALL_COLOR);
        tft.fillRect(0, 66, WaveshareTftShield.WIDTH, 6, BASEBOARD_COLOR);
        tft.fillRect(0, 72, WaveshareTftShield.WIDTH, WaveshareTftShield.HEIGHT - 72, floorColor);

        // 2. Top wall gallery (hanging plant, portraits, landscape painting, clock)
        drawStatic(tft, SpriteAssets.HANGING_PLANT, 16, 8, -24, palette);
        drawStatic(tft, SpriteAssets.SMALL_PAINTING, 16, 68, -21, palette);
        drawStatic(tft, SpriteAssets.LARGE_PAINTING, 32, 128, -21, palette);
        drawStatic(tft, SpriteAssets.SMALL_PAINTING_2, 16, 224, -21, palette);
        drawStatic(tft, SpriteAssets.CLOCK, 16, 272, -21, palette);

        // 3. Floor plants (left side) and trash bin (bottom-right near desk)
        drawStatic(tft, SpriteAssets.PLANT, 16, 8, 36, palette);
        drawStatic(tft, SpriteAssets.PLANT_2, 16, 8, 140, palette);
        drawStatic(tft, SpriteAssets.BIN, 16, CHAIR_X, 192, palette);

        // 4. Right-side workstation (desk and PC)
        drawStatic(tft, SpriteAssets.DESK_SIDE, 16, DESK_X, DESK_Y, palette);
        drawStatic(tft, SpriteAssets.PC_SIDE, 16, DESK_X, CHAR_Y, palette);
    }

    private static void drawStatic(WaveshareTftShield tft, int offset,
            int w, int x, int y, Palette palette) {
        tft.drawSpanSprite(x, y, w, SCALE, false, offset, palette);
    }
}
