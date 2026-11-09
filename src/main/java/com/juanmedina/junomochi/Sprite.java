// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

/**
 * High-level span-encoded 2D sprite referencing an offset inside SpriteAssets.DATA in Flash (.rodata).
 */
public final class Sprite {

    private final int width;
    private final int height;
    private final int offset;

    public Sprite(int width, int height, int offset) {
        this.width = width;
        this.height = height;
        this.offset = offset;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getOffset() {
        return offset;
    }

    /**
     * Draws this sprite to the given display.
     */
    public void draw(WaveshareTftShield display, int x, int y, int scale, boolean flipX, Palette palette) {
        if (display != null) {
            display.drawSprite(this, x, y, scale, flipX, palette);
        }
    }

    /**
     * Draws this sprite to the given display (unflipped).
     */
    public void draw(WaveshareTftShield display, int x, int y, int scale, Palette palette) {
        draw(display, x, y, scale, false, palette);
    }
}
