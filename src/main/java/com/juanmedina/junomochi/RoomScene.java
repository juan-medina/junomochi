// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

import io.github.jabrena.juno.hardware.device.sd.SdCard;
import java.io.InputStream;

/**
 * Renders the static office room background, wall gallery, floor plants, and workstation.
 * Reuses a single sprite buffer so static assets consume zero additional heap after startup.
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
    public static void draw(WaveshareTftShield tft, SdCard sd, Palette palette) {
        var floorColor = palette.getColor(FLOOR_PALETTE_INDEX);
        var staticSprite = new Sprite();

        // 1. Top wall, wooden baseboard, and floor background
        tft.fillRect(0, 0, WaveshareTftShield.WIDTH, 66, WALL_COLOR);
        tft.fillRect(0, 66, WaveshareTftShield.WIDTH, 6, BASEBOARD_COLOR);
        tft.fillRect(0, 72, WaveshareTftShield.WIDTH, WaveshareTftShield.HEIGHT - 72, floorColor);

        // 2. Top wall gallery (hanging plant, portraits, landscape painting, clock)
        drawStatic(sd.open("hanging_plant.bin"), staticSprite, tft, 16, 32, 8, -24, palette);
        drawStatic(sd.open("small_painting.bin"), staticSprite, tft, 16, 32, 68, -21, palette);
        drawStatic(sd.open("large_painting.bin"), staticSprite, tft, 32, 32, 128, -21, palette);
        drawStatic(sd.open("small_painting_2.bin"), staticSprite, tft, 16, 32, 224, -21, palette);
        drawStatic(sd.open("clock.bin"), staticSprite, tft, 16, 32, 272, -21, palette);

        // 3. Floor plants (left side) and trash bin (bottom-right near desk)
        drawStatic(sd.open("plant.bin"), staticSprite, tft, 16, 32, 8, 36, palette);
        drawStatic(sd.open("plant_2.bin"), staticSprite, tft, 16, 32, 8, 140, palette);
        drawStatic(sd.open("bin.bin"), staticSprite, tft, 16, 16, CHAIR_X, 192, palette);

        // 4. Right-side workstation (desk and PC)
        drawStatic(sd.open("desk_side.bin"), staticSprite, tft, 16, 64, DESK_X, DESK_Y, palette);
        drawStatic(sd.open("pc_side.bin"), staticSprite, tft, 16, 32, DESK_X, CHAR_Y, palette);
    }

    private static void drawStatic(InputStream in, Sprite temp, WaveshareTftShield tft,
            int w, int h, int x, int y, Palette palette) {
        if (temp.reload(in, w, h)) {
            temp.draw(tft, x, y, SCALE, palette);
        }
    }
}

