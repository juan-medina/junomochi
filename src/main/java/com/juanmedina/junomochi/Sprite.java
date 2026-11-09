// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

import java.io.InputStream;

/**
 * High-level span-encoded 2D sprite with native transparency.
 */
public final class Sprite {

    public static final int MAX_DATA_SIZE = 400;
    public static final int MAX_STATIC_DATA_SIZE = 560;

    private int width;
    private int height;
    private final byte[] data;
    private int dataLength;

    public Sprite(int width, int height, byte[] data, int dataLength) {
        this.width = width;
        this.height = height;
        this.data = data;
        this.dataLength = dataLength;
    }

    /**
     * Creates a reusable Sprite with a fixed backing buffer of MAX_STATIC_DATA_SIZE bytes.
     */
    public Sprite() {
        this.width = 0;
        this.height = 0;
        this.data = new byte[MAX_STATIC_DATA_SIZE];
        this.dataLength = 0;
    }

    /**
     * Loads a sprite from an InputStream into this reusable Sprite's buffer.
     */
    public boolean reload(InputStream in, int width, int height) {
        if (in == null) {
            return false;
        }
        var capacity = data.length;
        var count = 0;
        try {
            while (count < capacity) {
                var n = in.read(data, count, capacity - count);
                if (n <= 0) {
                    break;
                }
                count += n;
            }
            in.close();
        } catch (Exception ignored) {
            return false;
        }
        if (count == 0) {
            return false;
        }
        this.width = width;
        this.height = height;
        this.dataLength = count;
        return true;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getData() {
        return data;
    }

    public int getDataLength() {
        return dataLength;
    }

    /**
     * Loads a sprite from an InputStream.
     */
    public static Sprite load(InputStream in, int width, int height) {
        if (in == null) {
            return null;
        }
        var buffer = new byte[MAX_DATA_SIZE];
        var count = 0;
        try {
            while (count < MAX_DATA_SIZE) {
                var n = in.read(buffer, count, MAX_DATA_SIZE - count);
                if (n <= 0) {
                    break;
                }
                count += n;
            }
            in.close();
        } catch (Exception ignored) {
            return null;
        }

        if (count == 0) {
            return null;
        }
        return new Sprite(width, height, buffer, count);
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
