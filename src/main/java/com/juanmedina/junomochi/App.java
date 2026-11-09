// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

import io.github.jabrena.juno.annotations.ArduinoUnoR4WiFiBoard;
import io.github.jabrena.juno.annotations.Board;
import io.github.jabrena.juno.hardware.device.tft.TftTouchShield;
import io.github.jabrena.juno.hardware.display.Display;
import io.github.jabrena.juno.util.Delay;

@Board(ArduinoUnoR4WiFiBoard.class)
public final class App {

    private static final int DARK_BG = 0x0842;

    // Timeout before screen sleeps: 3 minutes
    private static final int SLEEP_TIMEOUT_M = 3;
    private static final int SLEEP_TIMEOUT_S = SLEEP_TIMEOUT_M * 60;
    private static final int SLEEP_TIMEOUT_MS = SLEEP_TIMEOUT_S * 1000;
    private static final int POLL_INTERVAL_MS = 50;

    private static final int SPRITE_SIZE = 16;
    private static final int SPRITE_SCALE = 6;
    private static final byte[] RETRO_SPRITE = {
        0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0,
        0, 0, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
        0, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
        0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 4, 4, 1, 1, 1, 1, 4, 4, 1, 1, 1, 1,
        1, 1, 1, 1, 4, 4, 1, 1, 1, 1, 4, 4, 1, 1, 1, 1,
        1, 1, 3, 3, 1, 1, 1, 4, 4, 1, 1, 1, 3, 3, 1, 1,
        1, 1, 3, 3, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
        0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0,
        0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] SPRITE_PALETTE = {
        DARK_BG,
        Display.WHITE,
        Display.CYAN,
        Display.MAGENTA,
        TftTouchShield.NAVY
    };

    private App() {
    }

    public static void main(String[] args) {
        WaveshareTftShield tft = new WaveshareTftShield();

        renderScene(tft);

        boolean awake = true;
        int idleElapsedMs = 0;

        while (true) {
            Delay.millis(POLL_INTERVAL_MS);

            boolean touched = tft.isTouched();
            if (touched) {
                idleElapsedMs = 0;
                if (!awake) {
                    tft.setBacklight(true);
                    awake = true;
                    // Debounce finger release
                    while (tft.isTouched()) {
                        Delay.millis(20);
                    }
                }
            } else if (awake) {
                idleElapsedMs += POLL_INTERVAL_MS;
                if (idleElapsedMs >= SLEEP_TIMEOUT_MS) {
                    tft.setBacklight(false);
                    awake = false;
                }
            }
        }
    }

    private static void renderScene(WaveshareTftShield tft) {
        // 1. Clear background
        tft.fillScreen(DARK_BG);

        // 2. Title header
        tft.setCursor(64, 10);
        tft.setTextColor(Display.WHITE, Display.BLACK);
        tft.setTextSize(2);
        tft.print("JUNOMOCHI RGB565");

        // 3. Scaled retro sprite
        int spriteDim = SPRITE_SIZE * SPRITE_SCALE;
        int spriteX = (tft.width() - spriteDim) / 2;
        int spriteY = 36;
        tft.drawSprite(spriteX, spriteY, SPRITE_SIZE, SPRITE_SIZE, SPRITE_SCALE, RETRO_SPRITE, SPRITE_PALETTE);

        // 4. Primary RGB test channels with text
        int barY = 148;
        int barHeight = 52;
        int barWidth = 80;

        tft.fillRect(20, barY, barWidth, barHeight, Display.RED);
        tft.setCursor(36, barY + 18);
        tft.setTextColor(Display.WHITE, Display.RED);
        tft.setTextSize(2);
        tft.print("RED");

        tft.fillRect(120, barY, barWidth, barHeight, Display.GREEN);
        tft.setCursor(130, barY + 18);
        tft.setTextColor(Display.BLACK, Display.GREEN);
        tft.print("GRN");

        tft.fillRect(220, barY, barWidth, barHeight, Display.BLUE);
        tft.setCursor(236, barY + 18);
        tft.setTextColor(Display.WHITE, Display.BLUE);
        tft.print("BLU");

        // 5. Secondary color bar reference strip
        int stripY = 210;
        int stripHeight = 20;
        int swatchWidth = 46;

        tft.fillRect(20, stripY, swatchWidth, stripHeight, Display.YELLOW);
        tft.fillRect(67, stripY, swatchWidth, stripHeight, Display.CYAN);
        tft.fillRect(114, stripY, swatchWidth, stripHeight, Display.MAGENTA);
        tft.fillRect(161, stripY, swatchWidth, stripHeight, TftTouchShield.ORANGE);
        tft.fillRect(208, stripY, swatchWidth, stripHeight, Display.WHITE);
        tft.fillRect(255, stripY, 45, stripHeight, TftTouchShield.GRAY);
    }
}
