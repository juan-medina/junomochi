// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

/**
 * Small 512-byte RAM buffer (16x32) for flicker-free sprite compositing and dirty-region redraw.
 */
public final class TileBuffer {

    public static final int BUFFER_SIZE = 512;

    private final int width;
    private final int height;
    private final byte[] pixels;

    public TileBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new byte[BUFFER_SIZE];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getPixels() {
        return pixels;
    }

    /**
     * Fills the buffer with a background color index.
     */
    public void clear(int colorIndex) {
        byte b = (byte) colorIndex;
        int total = width * height;
        if (total > BUFFER_SIZE) {
            total = BUFFER_SIZE;
        }
        for (int i = 0; i < total; i++) {
            pixels[i] = b;
        }
    }

    /**
     * Composites a sprite's opaque spans on top of whatever is already in the buffer,
     * shifted horizontally by offsetX (in unscaled sprite pixels).
     * Transparent areas of the sprite leave the existing background untouched.
     */
    public void composite(Sprite sprite, int offsetX, boolean flipX) {
        if (sprite == null) {
            return;
        }
        byte[] data = sprite.getData();
        if (data == null || data.length < 2) {
            return;
        }
        int numSpans = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        int ptr = 2;

        for (int i = 0; i < numSpans; i++) {
            int row = data[ptr++] & 0xFF;
            int col = data[ptr++] & 0xFF;
            int flagLen = data[ptr++] & 0xFF;
            boolean isRepeat = (flagLen & 0x80) != 0;
            int spanLen = flagLen & 0x7F;

            int drawCol = (flipX ? (width - col - spanLen) : col) + offsetX;
            int rowOffset = row * width;

            if (isRepeat) {
                byte colorIdx = data[ptr++];
                if (row >= 0 && row < height) {
                    for (int c = 0; c < spanLen; c++) {
                        int dstCol = drawCol + c;
                        if (dstCol >= 0 && dstCol < width) {
                            pixels[rowOffset + dstCol] = colorIdx;
                        }
                    }
                }
            } else {
                if (row >= 0 && row < height) {
                    for (int c = 0; c < spanLen; c++) {
                        int dstCol = drawCol + c;
                        if (dstCol >= 0 && dstCol < width) {
                            int srcCol = flipX ? (spanLen - 1 - c) : c;
                            pixels[rowOffset + dstCol] = data[ptr + srcCol];
                        }
                    }
                }
                ptr += spanLen;
            }
        }
    }

    public void composite(Sprite sprite, int offsetX) {
        composite(sprite, offsetX, false);
    }

    public void composite(Sprite sprite, boolean flipX) {
        composite(sprite, 0, flipX);
    }

    public void composite(Sprite sprite) {
        composite(sprite, 0, false);
    }

    /**
     * Blits the completed composite buffer to the display in a single continuous SPI write.
     */
    public void blit(WaveshareTftShield display, int x, int y, int scale, Palette palette) {
        if (display != null) {
            display.drawBuffer(x, y, width, height, scale, pixels, palette);
        }
    }
}

