// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

import java.io.InputStream;

/**
 * 16-bit RGB565 color palette loaded from a binary stream (e.g. palette.bin).
 */
public final class Palette {

    public static final int MAX_COLORS = 64;

    private final int[] colors;
    private final int size;

    public Palette(int[] colors, int size) {
        this.colors = colors;
        this.size = size;
    }

    /**
     * Loads a palette from binary big-endian RGB565 pairs.
     */
    public static Palette load(InputStream in) {
        if (in == null) {
            return new Palette(new int[MAX_COLORS], 0);
        }
        var buf = new byte[128];
        var count = 0;
        try {
            while (count < 128) {
                var n = in.read(buf, count, 128 - count);
                if (n <= 0) {
                    break;
                }
                count += n;
            }
            in.close();
        } catch (Exception ignored) {
        }

        var numColors = count / 2;
        var paletteColors = new int[MAX_COLORS];
        for (var i = 0; i < numColors; i++) {
            var hi = buf[i * 2] & 0xFF;
            var lo = buf[i * 2 + 1] & 0xFF;
            paletteColors[i] = (hi << 8) | lo;
        }
        return new Palette(paletteColors, numColors);
    }

    public int getColor(int index) {
        if (index < 0 || index >= size) {
            return 0;
        }
        return colors[index];
    }

    public int[] getColors() {
        return colors;
    }

    public int size() {
        return size;
    }
}
