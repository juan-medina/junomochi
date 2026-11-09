// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

/**
 * 16-bit RGB565 color palette backed directly by Flash (.rodata) in SpriteAssets.
 */
public final class Palette {

    public int getColor(int index) {
        if (index < 0 || index >= SpriteAssets.PALETTE_COLORS.length) {
            return 0;
        }
        return SpriteAssets.PALETTE_COLORS[index];
    }

    public int size() {
        return SpriteAssets.PALETTE_COLORS.length;
    }
}
