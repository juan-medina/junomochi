// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi.sim;

import com.juanmedina.junomochi.Palette;
import com.juanmedina.junomochi.SpriteAssets;
import com.juanmedina.junomochi.TouchDisplay;
import io.github.jabrena.juno.hardware.led.LedMatrixFontAscii;

/**
 * Software 320x240 RGB565 framebuffer implementation of {@link TouchDisplay} for desktop simulation
 * and pixel-perfect video capture.
 */
public final class DesktopTouchDisplay implements TouchDisplay {

    public static final int CHAR_WIDTH = 6;
    public static final int CHAR_HEIGHT = 8;

    private final int[] framebuffer = new int[WIDTH * HEIGHT];
    private volatile boolean touched;

    private int cursorX;
    private int cursorY;
    private int textColor = WHITE;
    private int textBackground = BLACK;
    private int textSize = 1;

    public void setTouched(boolean touched) {
        this.touched = touched;
    }

    @Override
    public boolean isTouched() {
        return touched;
    }

    public int[] getFramebuffer() {
        return framebuffer;
    }

    public static int rgb565ToRgb888(int color565) {
        var r5 = (color565 >> 11) & 0x1F;
        var g6 = (color565 >> 5) & 0x3F;
        var b5 = color565 & 0x1F;
        var r8 = (r5 << 3) | (r5 >> 2);
        var g8 = (g6 << 2) | (g6 >> 4);
        var b8 = (b5 << 3) | (b5 >> 2);
        return (r8 << 16) | (g8 << 8) | b8;
    }

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public void fillScreen(int color) {
        fillRect(0, 0, WIDTH, HEIGHT, color);
    }

    @Override
    public void drawPixel(int x, int y, int color) {
        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
            framebuffer[y * WIDTH + x] = rgb565ToRgb888(color);
        }
    }

    @Override
    public void fillRect(int x, int y, int width, int height, int color) {
        if (x >= WIDTH || y >= HEIGHT || width <= 0 || height <= 0) {
            return;
        }
        var x0 = Math.max(0, x);
        var y0 = Math.max(0, y);
        var x1 = Math.min(x + width - 1, WIDTH - 1);
        var y1 = Math.min(y + height - 1, HEIGHT - 1);
        var rgb = rgb565ToRgb888(color);

        for (var py = y0; py <= y1; py++) {
            var rowOffset = py * WIDTH;
            for (var px = x0; px <= x1; px++) {
                framebuffer[rowOffset + px] = rgb;
            }
        }
    }

    @Override
    public void setCursor(int x, int y) {
        this.cursorX = x;
        this.cursorY = y;
    }

    @Override
    public void setTextColor(int foreground, int background) {
        this.textColor = foreground;
        this.textBackground = background;
    }

    @Override
    public void setTextSize(int size) {
        this.textSize = Math.max(size, 1);
    }

    @Override
    public void print(char character) {
        if (character == '\n') {
            newLine();
            return;
        }
        var cellWidth = CHAR_WIDTH * textSize;
        if (cursorX + cellWidth > WIDTH) {
            newLine();
        }
        drawChar(cursorX, cursorY, character);
        cursorX += cellWidth;
    }

    @Override
    public void print(String text) {
        for (var i = 0; i < text.length(); i++) {
            print(text.charAt(i));
        }
    }

    @Override
    public void println(String text) {
        print(text);
        newLine();
    }

    private void newLine() {
        cursorX = 0;
        cursorY += CHAR_HEIGHT * textSize;
    }

    private void drawChar(int x, int y, int character) {
        var code = character;
        if (code < 32 || code > 126) {
            code = 32;
        }
        var cellWidth = CHAR_WIDTH * textSize;
        var cellHeight = CHAR_HEIGHT * textSize;
        if (x < 0 || y < 0 || x + cellWidth > WIDTH || y + cellHeight > HEIGHT) {
            return;
        }

        var fgRgb = rgb565ToRgb888(textColor);
        var bgRgb = rgb565ToRgb888(textBackground);

        for (var row = 0; row < CHAR_HEIGHT; row++) {
            var rowBits = glyphRowBits(code, row);
            for (var sy = 0; sy < textSize; sy++) {
                var py = y + row * textSize + sy;
                var rowOffset = py * WIDTH;
                for (var col = 0; col < CHAR_WIDTH; col++) {
                    var lit = ((rowBits >> col) & 1) != 0;
                    var rgb = lit ? fgRgb : bgRgb;
                    for (var sx = 0; sx < textSize; sx++) {
                        var px = x + col * textSize + sx;
                        framebuffer[rowOffset + px] = rgb;
                    }
                }
            }
        }
    }

    private static int glyphRowBits(int code, int row) {
        if (row >= LedMatrixFontAscii.GLYPH_HEIGHT) {
            return 0;
        }
        var bits = 0;
        for (var col = 0; col < LedMatrixFontAscii.GLYPH_WIDTH; col++) {
            if (LedMatrixFontAscii.charPixel(code, col, row)) {
                bits |= (1 << col);
            }
        }
        return bits;
    }

    @Override
    public void drawSpanSprite(
            int startX,
            int startY,
            int spriteWidth,
            int scale,
            boolean flipX,
            int offset,
            Palette palette) {
        if (palette == null) {
            return;
        }
        var numSpans = ((SpriteAssets.DATA[offset] & 0xFF) << 8) | (SpriteAssets.DATA[offset + 1] & 0xFF);
        var ptr = offset + 2;

        for (var i = 0; i < numSpans; i++) {
            var row = SpriteAssets.DATA[ptr++] & 0xFF;
            var col = SpriteAssets.DATA[ptr++] & 0xFF;
            var flagLen = SpriteAssets.DATA[ptr++] & 0xFF;
            var isRepeat = (flagLen & 0x80) != 0;
            var spanLen = flagLen & 0x7F;

            var drawCol = flipX ? (spriteWidth - col - spanLen) : col;
            var x0 = startX + drawCol * scale;
            var y0 = startY + row * scale;
            var spanWidthPx = spanLen * scale;
            var x1 = x0 + spanWidthPx - 1;
            var y1 = y0 + scale - 1;

            if (x0 < 0 || x1 >= WIDTH || y0 < 0 || y1 >= HEIGHT) {
                if (isRepeat) {
                    ptr++;
                } else {
                    ptr += spanLen;
                }
                continue;
            }

            if (isRepeat) {
                var colorIdx = SpriteAssets.DATA[ptr++] & 0xFF;
                var rgb = rgb565ToRgb888(palette.getColor(colorIdx));
                for (var py = y0; py <= y1; py++) {
                    var rowOffset = py * WIDTH;
                    for (var px = x0; px <= x1; px++) {
                        framebuffer[rowOffset + px] = rgb;
                    }
                }
            } else {
                var dataStart = ptr;
                ptr += spanLen;
                for (var sy = 0; sy < scale; sy++) {
                    var py = y0 + sy;
                    var rowOffset = py * WIDTH;
                    for (var c = 0; c < spanLen; c++) {
                        var srcCol = flipX ? (spanLen - 1 - c) : c;
                        var colorIdx = SpriteAssets.DATA[dataStart + srcCol] & 0xFF;
                        var rgb = rgb565ToRgb888(palette.getColor(colorIdx));
                        for (var sx = 0; sx < scale; sx++) {
                            framebuffer[rowOffset + x0 + c * scale + sx] = rgb;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void drawBuffer(
            int startX,
            int startY,
            int bufferWidth,
            int bufferHeight,
            int scale,
            byte[] buffer,
            Palette palette) {
        if (buffer == null || palette == null) {
            return;
        }
        var scaledW = bufferWidth * scale;
        var scaledH = bufferHeight * scale;
        var x1 = startX + scaledW - 1;
        var y1 = startY + scaledH - 1;

        if (startX < 0 || x1 >= WIDTH || startY < 0 || y1 >= HEIGHT) {
            return;
        }

        for (var row = 0; row < bufferHeight; row++) {
            var rowOffset = row * bufferWidth;
            for (var sy = 0; sy < scale; sy++) {
                var py = startY + row * scale + sy;
                var dstRowOffset = py * WIDTH;
                for (var col = 0; col < bufferWidth; col++) {
                    var colorIdx = buffer[rowOffset + col] & 0xFF;
                    var rgb = rgb565ToRgb888(palette.getColor(colorIdx));
                    for (var sx = 0; sx < scale; sx++) {
                        framebuffer[dstRowOffset + startX + col * scale + sx] = rgb;
                    }
                }
            }
        }
    }
}

