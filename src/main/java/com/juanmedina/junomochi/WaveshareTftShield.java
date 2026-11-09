// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

import io.github.jabrena.juno.hardware.ArduinoUno;
import io.github.jabrena.juno.hardware.device.sd.SdCard;
import io.github.jabrena.juno.hardware.display.Display;
import io.github.jabrena.juno.hardware.gpio.DigitalOutput;
import io.github.jabrena.juno.hardware.gpio.Gpio;
import io.github.jabrena.juno.hardware.internal.NativeSdCard;
import io.github.jabrena.juno.hardware.led.LedMatrixFontAscii;
import io.github.jabrena.juno.hardware.spi.Spi;
import io.github.jabrena.juno.util.Delay;

/**
 * Driver for the Waveshare 2.8" TFT Touch LCD Shield (320x240, ST7789V / ILI9341 over SPI).
 * Implements Juno's {@link Display} interface with zero heap allocation after startup.
 */
public final class WaveshareTftShield implements Display {

    // Shield default pinout
    public static final int DEFAULT_CS = 10;
    public static final int DEFAULT_DC = 7;
    public static final int DEFAULT_BL = 9;
    public static final int DEFAULT_TOUCH_CS = 4;
    public static final int DEFAULT_TOUCH_IRQ = 3;
    public static final int DEFAULT_SD_CS = 5;

    // Display dimensions
    public static final int WIDTH = 320;
    public static final int HEIGHT = 240;

    // MIPI DCS commands
    private static final int CMD_SOFT_RESET = 0x01;
    private static final int CMD_SLEEP_OUT = 0x11;
    private static final int CMD_INVON = 0x21;
    private static final int CMD_DISPLAY_ON = 0x29;
    private static final int CMD_COLUMN_ADDRESS_SET = 0x2A;
    private static final int CMD_PAGE_ADDRESS_SET = 0x2B;
    private static final int CMD_MEMORY_WRITE = 0x2C;
    private static final int CMD_MEMORY_ACCESS_CONTROL = 0x36;
    private static final int CMD_PIXEL_FORMAT = 0x3A;

    private static final int PIXEL_FORMAT_RGB565 = 0x55;
    private static final int MADCTL_LANDSCAPE = 0x60;

    public static final int CHAR_WIDTH = 6;
    public static final int CHAR_HEIGHT = 8;

    private final Spi spi;
    private final Gpio gpio;
    private final DigitalOutput cs;
    private final DigitalOutput dc;
    private final DigitalOutput bl;
    private final DigitalOutput touchCs;
    private final int touchIrqPin;
    private final int sdCsPin;
    private SdCard sdCard;

    private int cursorX;
    private int cursorY;
    private int textColor = WHITE;
    private int textBackground = BLACK;
    private int textSize = 1;

    /** Default shield configuration on Arduino Uno R4 WiFi. */
    public WaveshareTftShield() {
        this(
            ArduinoUno.getSpi(),
            ArduinoUno.getGpio(),
            DEFAULT_CS,
            DEFAULT_DC,
            DEFAULT_BL,
            DEFAULT_TOUCH_CS,
            DEFAULT_TOUCH_IRQ,
            DEFAULT_SD_CS
        );
    }

    /** Custom pin configuration. */
    public WaveshareTftShield(
            Spi spi,
            Gpio gpio,
            int csPin,
            int dcPin,
            int blPin,
            int touchCsPin,
            int touchIrqPin,
            int sdCsPin) {
        this.spi = spi;
        this.gpio = gpio;
        this.cs = DigitalOutput.of(csPin);
        this.dc = DigitalOutput.of(dcPin);
        this.bl = DigitalOutput.of(blPin);
        this.touchCs = DigitalOutput.of(touchCsPin);
        this.touchIrqPin = touchIrqPin;
        this.sdCsPin = sdCsPin;

        DigitalOutput sdCs = DigitalOutput.of(sdCsPin);

        // Configure touch IRQ as input with pullup (active-low when touched)
        gpio.pinMode(touchIrqPin, gpio.getInputPullup());

        // Deselect other devices on the shared SPI bus
        touchCs.high();
        sdCs.high();

        cs.high();
        dc.high();
        bl.high();

        spi.begin();
        init();
        initTouch();
    }

    private void initTouch() {
        touchCs.low();
        spi.transfer(0x80);
        spi.transfer(0x00);
        spi.transfer(0x00);
        touchCs.high();
    }

    private void init() {
        writeCommand(CMD_SOFT_RESET);
        Delay.millis(150);

        writeCommand(CMD_SLEEP_OUT);
        Delay.millis(150);

        writeCommandData8(CMD_PIXEL_FORMAT, PIXEL_FORMAT_RGB565);
        writeCommandData8(CMD_MEMORY_ACCESS_CONTROL, MADCTL_LANDSCAPE);
        writeCommand(CMD_INVON);

        writeCommand(CMD_DISPLAY_ON);
        Delay.millis(50);
    }

    /** Controls backlight power (true = ON, false = OFF / dark). */
    public void setBacklight(boolean on) {
        if (on) {
            bl.high();
        } else {
            bl.low();
        }
    }

    /** Returns whether the touch panel is currently being pressed (active-low IRQ). */
    public boolean isTouched() {
        return !gpio.digitalRead(touchIrqPin);
    }

    /** Returns the SdCard interface for the onboard micro-SD slot. */
    public SdCard getSdCard() {
        if (sdCard == null) {
            sdCard = NativeSdCard.sdCard(sdCsPin);
        }
        return sdCard;
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
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            return;
        }
        cs.low();
        setWindow(x, y, x, y);
        spi.transfer((color >> 8) & 0xFF);
        spi.transfer(color & 0xFF);
        cs.high();
    }

    @Override
    public void fillRect(int x, int y, int width, int height, int color) {
        if (x >= WIDTH || y >= HEIGHT || width <= 0 || height <= 0) {
            return;
        }
        int right = Math.min(x + width - 1, WIDTH - 1);
        int bottom = Math.min(y + height - 1, HEIGHT - 1);
        int totalPixels = (right - x + 1) * (bottom - y + 1);

        int hi = (color >> 8) & 0xFF;
        int lo = color & 0xFF;

        cs.low();
        setWindow(x, y, right, bottom);
        for (int i = 0; i < totalPixels; i++) {
            spi.transfer(hi);
            spi.transfer(lo);
        }
        cs.high();
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
        int cellWidth = CHAR_WIDTH * textSize;
        if (cursorX + cellWidth > WIDTH) {
            newLine();
        }
        drawChar(cursorX, cursorY, character);
        cursorX += cellWidth;
    }

    @Override
    public void print(String text) {
        int length = text.length();
        for (int i = 0; i < length; i++) {
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
        int code = character;
        if (code < 32 || code > 126) {
            code = 32;
        }
        int cellWidth = CHAR_WIDTH * textSize;
        int cellHeight = CHAR_HEIGHT * textSize;
        if (x < 0 || y < 0 || x + cellWidth > WIDTH || y + cellHeight > HEIGHT) {
            return;
        }

        int hiFg = (textColor >> 8) & 0xFF;
        int loFg = textColor & 0xFF;
        int hiBg = (textBackground >> 8) & 0xFF;
        int loBg = textBackground & 0xFF;

        cs.low();
        setWindow(x, y, x + cellWidth - 1, y + cellHeight - 1);
        for (int row = 0; row < CHAR_HEIGHT; row++) {
            int rowBits = glyphRowBits(code, row);
            for (int sy = 0; sy < textSize; sy++) {
                for (int col = 0; col < CHAR_WIDTH; col++) {
                    boolean lit = ((rowBits >> col) & 1) != 0;
                    int hi = lit ? hiFg : hiBg;
                    int lo = lit ? loFg : loBg;
                    for (int sx = 0; sx < textSize; sx++) {
                        spi.transfer(hi);
                        spi.transfer(lo);
                    }
                }
            }
        }
        cs.high();
    }

    private static int glyphRowBits(int code, int row) {
        if (row >= LedMatrixFontAscii.GLYPH_HEIGHT) {
            return 0;
        }
        int bits = 0;
        for (int col = 0; col < LedMatrixFontAscii.GLYPH_WIDTH; col++) {
            if (LedMatrixFontAscii.charPixel(code, col, row)) {
                bits |= (1 << col);
            }
        }
        return bits;
    }

    /** Draws a 2D byte-indexed sprite scaled uniformly. */
    public void drawSprite(
            int startX,
            int startY,
            int spriteWidth,
            int spriteHeight,
            int scale,
            byte[] spriteData,
            int[] palette) {
        drawSprite(startX, startY, spriteWidth, spriteHeight, scale, false, spriteData, palette);
    }

    /** Draws a 2D byte-indexed sprite with optional horizontal flipping. */
    public void drawSprite(
            int startX,
            int startY,
            int spriteWidth,
            int spriteHeight,
            int scale,
            boolean flipX,
            byte[] spriteData,
            int[] palette) {
        int scaledW = spriteWidth * scale;
        int scaledH = spriteHeight * scale;

        cs.low();
        setWindow(startX, startY, startX + scaledW - 1, startY + scaledH - 1);
        for (int row = 0; row < spriteHeight; row++) {
            int rowOffset = row * spriteWidth;
            for (int sy = 0; sy < scale; sy++) {
                for (int col = 0; col < spriteWidth; col++) {
                    int srcCol = flipX ? (spriteWidth - 1 - col) : col;
                    int color = palette[spriteData[rowOffset + srcCol] & 0xFF];
                    int hi = (color >> 8) & 0xFF;
                    int lo = color & 0xFF;
                    for (int sx = 0; sx < scale; sx++) {
                        spi.transfer(hi);
                        spi.transfer(lo);
                    }
                }
            }
        }
        cs.high();
    }

    /** Draws a sprite filling transparent pixels (index 0) with a background color. */
    public void drawSpriteWithBg(
            int startX,
            int startY,
            int spriteWidth,
            int spriteHeight,
            int scale,
            boolean flipX,
            byte[] spriteData,
            int[] palette,
            int bgColor) {
        int scaledW = spriteWidth * scale;
        int scaledH = spriteHeight * scale;

        int bgHi = (bgColor >> 8) & 0xFF;
        int bgLo = bgColor & 0xFF;

        cs.low();
        setWindow(startX, startY, startX + scaledW - 1, startY + scaledH - 1);
        for (int row = 0; row < spriteHeight; row++) {
            int rowOffset = row * spriteWidth;
            for (int sy = 0; sy < scale; sy++) {
                for (int col = 0; col < spriteWidth; col++) {
                    int srcCol = flipX ? (spriteWidth - 1 - col) : col;
                    int colorIdx = spriteData[rowOffset + srcCol] & 0xFF;
                    int hi;
                    int lo;
                    if (colorIdx == 0) {
                        hi = bgHi;
                        lo = bgLo;
                    } else {
                        int color = palette[colorIdx];
                        hi = (color >> 8) & 0xFF;
                        lo = color & 0xFF;
                    }
                    for (int sx = 0; sx < scale; sx++) {
                        spi.transfer(hi);
                        spi.transfer(lo);
                    }
                }
            }
        }
        cs.high();
    }

    /** Draws a sprite skipping transparent pixels (index 0) via horizontal spans. */
    public void drawSpriteTransparent(
            int startX,
            int startY,
            int spriteWidth,
            int spriteHeight,
            int scale,
            boolean flipX,
            byte[] spriteData,
            int[] palette) {
        for (int row = 0; row < spriteHeight; row++) {
            int rowOffset = row * spriteWidth;
            int col = 0;
            while (col < spriteWidth) {
                int srcCol = flipX ? (spriteWidth - 1 - col) : col;
                int colorIndex = spriteData[rowOffset + srcCol] & 0xFF;
                if (colorIndex == 0) {
                    col++;
                    continue;
                }
                int spanStart = col;
                while (col < spriteWidth) {
                    int sc = flipX ? (spriteWidth - 1 - col) : col;
                    if ((spriteData[rowOffset + sc] & 0xFF) == 0) {
                        break;
                    }
                    col++;
                }
                int spanLen = col - spanStart;

                int x0 = startX + spanStart * scale;
                int y0 = startY + row * scale;
                int x1 = x0 + spanLen * scale - 1;
                int y1 = y0 + scale - 1;

                cs.low();
                setWindow(x0, y0, x1, y1);
                for (int sy = 0; sy < scale; sy++) {
                    for (int c = spanStart; c < col; c++) {
                        int sc = flipX ? (spriteWidth - 1 - c) : c;
                        int color = palette[spriteData[rowOffset + sc] & 0xFF];
                        int hi = (color >> 8) & 0xFF;
                        int lo = color & 0xFF;
                        for (int sx = 0; sx < scale; sx++) {
                            spi.transfer(hi);
                            spi.transfer(lo);
                        }
                    }
                }
                cs.high();
            }
        }
    }

    /**
     * Draws a span-encoded sprite with pure transparency.
     * Only opaque pixel spans are transmitted, preserving the background.
     */
    public void drawSpanSprite(
            int startX,
            int startY,
            int spriteWidth,
            int scale,
            boolean flipX,
            byte[] data,
            int offset,
            int[] palette) {
        int numSpans = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        int ptr = offset + 2;

        for (int i = 0; i < numSpans; i++) {
            int row = data[ptr++] & 0xFF;
            int col = data[ptr++] & 0xFF;
            int flagLen = data[ptr++] & 0xFF;
            boolean isRepeat = (flagLen & 0x80) != 0;
            int spanLen = flagLen & 0x7F;

            int drawCol = flipX ? (spriteWidth - col - spanLen) : col;
            int x0 = startX + drawCol * scale;
            int y0 = startY + row * scale;
            int spanWidthPx = spanLen * scale;
            int x1 = x0 + spanWidthPx - 1;
            int y1 = y0 + scale - 1;

            if (x0 < 0 || x1 >= WIDTH || y0 < 0 || y1 >= HEIGHT) {
                if (isRepeat) {
                    ptr++;
                } else {
                    ptr += spanLen;
                }
                continue;
            }

            cs.low();
            setWindow(x0, y0, x1, y1);

            if (isRepeat) {
                int colorIdx = data[ptr++] & 0xFF;
                int color = palette[colorIdx];
                int hi = (color >> 8) & 0xFF;
                int lo = color & 0xFF;
                int totalPixels = spanWidthPx * scale;
                for (int p = 0; p < totalPixels; p++) {
                    spi.transfer(hi);
                    spi.transfer(lo);
                }
            } else {
                int dataStart = ptr;
                ptr += spanLen;
                for (int sy = 0; sy < scale; sy++) {
                    for (int c = 0; c < spanLen; c++) {
                        int srcCol = flipX ? (spanLen - 1 - c) : c;
                        int colorIdx = data[dataStart + srcCol] & 0xFF;
                        int color = palette[colorIdx];
                        int hi = (color >> 8) & 0xFF;
                        int lo = color & 0xFF;
                        for (int sx = 0; sx < scale; sx++) {
                            spi.transfer(hi);
                            spi.transfer(lo);
                        }
                    }
                }
            }
            cs.high();
        }
    }

    /**
     * Draws a high-level Sprite with transparency using a Palette.
     */
    public void drawSprite(
            Sprite sprite,
            int x,
            int y,
            int scale,
            boolean flipX,
            Palette palette) {
        if (sprite == null || sprite.getData() == null || palette == null) {
            return;
        }
        drawSpanSprite(x, y, sprite.getWidth(), scale, flipX, sprite.getData(), 0, palette);
    }

    /**
     * Draws a high-level Sprite (unflipped) with transparency using a Palette.
     */
    public void drawSprite(
            Sprite sprite,
            int x,
            int y,
            int scale,
            Palette palette) {
        drawSprite(sprite, x, y, scale, false, palette);
    }

    /**
     * Draws a span-encoded sprite with pure transparency using a Palette.
     */
    public void drawSpanSprite(
            int startX,
            int startY,
            int spriteWidth,
            int scale,
            boolean flipX,
            byte[] data,
            int offset,
            Palette palette) {
        if (palette == null) {
            return;
        }
        drawSpanSprite(startX, startY, spriteWidth, scale, flipX, data, offset, palette.getColors());
    }

    /**
     * Blits a 2D composite buffer to the display scaled in a single SPI transaction.
     * Overwrites all pixels in the destination window with zero flicker.
     */
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
        int scaledW = bufferWidth * scale;
        int scaledH = bufferHeight * scale;
        int x1 = startX + scaledW - 1;
        int y1 = startY + scaledH - 1;

        if (startX < 0 || x1 >= WIDTH || startY < 0 || y1 >= HEIGHT) {
            return;
        }

        cs.low();
        setWindow(startX, startY, x1, y1);
        int[] colors = palette.getColors();

        for (int row = 0; row < bufferHeight; row++) {
            int rowOffset = row * bufferWidth;
            for (int sy = 0; sy < scale; sy++) {
                for (int col = 0; col < bufferWidth; col++) {
                    int colorIdx = buffer[rowOffset + col] & 0xFF;
                    int color = (colorIdx >= 0 && colorIdx < colors.length) ? colors[colorIdx] : 0;
                    int hi = (color >> 8) & 0xFF;
                    int lo = color & 0xFF;
                    for (int sx = 0; sx < scale; sx++) {
                        spi.transfer(hi);
                        spi.transfer(lo);
                    }
                }
            }
        }
        cs.high();
    }

    private void setWindow(int x0, int y0, int x1, int y1) {
        dc.low();
        spi.transfer(CMD_COLUMN_ADDRESS_SET);
        dc.high();
        spi.transfer((x0 >> 8) & 0xFF);
        spi.transfer(x0 & 0xFF);
        spi.transfer((x1 >> 8) & 0xFF);
        spi.transfer(x1 & 0xFF);

        dc.low();
        spi.transfer(CMD_PAGE_ADDRESS_SET);
        dc.high();
        spi.transfer((y0 >> 8) & 0xFF);
        spi.transfer(y0 & 0xFF);
        spi.transfer((y1 >> 8) & 0xFF);
        spi.transfer(y1 & 0xFF);

        dc.low();
        spi.transfer(CMD_MEMORY_WRITE);
        dc.high();
    }

    private void writeCommand(int command) {
        cs.low();
        dc.low();
        spi.transfer(command);
        dc.high();
        cs.high();
    }

    private void writeCommandData8(int command, int data) {
        cs.low();
        dc.low();
        spi.transfer(command);
        dc.high();
        spi.transfer(data);
        cs.high();
    }
}
