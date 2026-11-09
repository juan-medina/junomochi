// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

import io.github.jabrena.juno.hardware.display.Display;

/**
 * Hardware-agnostic 320x240 RGB565 touch display interface.
 * Implemented by {@link WaveshareTftShield} on Arduino UNO R4 WiFi and by desktop simulators.
 */
public interface TouchDisplay extends Display {

    int WIDTH = 320;
    int HEIGHT = 240;

    /**
     * Returns whether the touch panel is currently being pressed.
     */
    boolean isTouched();

    /**
     * Draws a span-encoded sprite from {@link SpriteAssets#DATA} with transparency.
     */
    void drawSpanSprite(
            int startX,
            int startY,
            int spriteWidth,
            int scale,
            boolean flipX,
            int offset,
            Palette palette);

    /**
     * Blits a 2D composite buffer scaled uniformly onto the display.
     */
    void drawBuffer(
            int startX,
            int startY,
            int bufferWidth,
            int bufferHeight,
            int scale,
            byte[] buffer,
            Palette palette);
}

