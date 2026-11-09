// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

import io.github.jabrena.juno.hardware.display.Display;

/**
 * Renders and clears a pixel-art speech bubble at dynamic screen coordinates.
 */
public final class SpeechBubble {

    private static final int BORDER_COLOR = 0x3164;
    private static final int TAIL_HEIGHT = 8;

    private final int width;
    private final int height;
    private int x;
    private int y;
    private boolean visible;

    public SpeechBubble(int width, int height) {
        this.width = width;
        this.height = height;
        this.visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * Draws the speech bubble at (x, y) with a downward pointer tail at tailX.
     */
    public void showAt(WaveshareTftShield tft, int x, int y, int tailX,
            String line1, String line2, int bgColor) {
        if (visible) {
            hide(tft, bgColor);
        }
        this.x = x;
        this.y = y;

        // Outer dark border and inner white fill
        tft.fillRect(x, y, width, height, BORDER_COLOR);
        tft.fillRect(x + 2, y + 2, width - 4, height - 4, Display.WHITE);

        // Downward pointer tail toward character's head
        tft.fillRect(tailX - 2, y + height, 10, 4, BORDER_COLOR);
        tft.fillRect(tailX, y + height - 2, 6, 4, Display.WHITE);
        tft.fillRect(tailX, y + height + 4, 6, 4, BORDER_COLOR);
        tft.fillRect(tailX + 2, y + height + 2, 2, 4, Display.WHITE);

        tft.setTextColor(Display.BLACK, Display.WHITE);
        tft.setTextSize(1);
        tft.setCursor(x + 6, y + 7);
        tft.print(line1);
        tft.setCursor(x + 6, y + 19);
        tft.print(line2);
        visible = true;
    }

    /**
     * Erases the speech bubble and tail with the given background color.
     */
    public void hide(WaveshareTftShield tft, int bgColor) {
        if (visible) {
            tft.fillRect(x, y, width, height + TAIL_HEIGHT, bgColor);
            visible = false;
        }
    }
}
