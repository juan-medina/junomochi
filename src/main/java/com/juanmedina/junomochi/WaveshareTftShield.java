// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

import io.github.jabrena.juno.hardware.ArduinoUno;
import io.github.jabrena.juno.hardware.display.Display;
import io.github.jabrena.juno.hardware.gpio.DigitalOutput;
import io.github.jabrena.juno.hardware.gpio.Gpio;
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
    private final int touchIrqPin;

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
        this.touchIrqPin = touchIrqPin;

        DigitalOutput touchCs = DigitalOutput.of(touchCsPin);
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
        int scaledW = spriteWidth * scale;
        int scaledH = spriteHeight * scale;

        cs.low();
        setWindow(startX, startY, startX + scaledW - 1, startY + scaledH - 1);
        for (int row = 0; row < spriteHeight; row++) {
            int rowOffset = row * spriteWidth;
            for (int sy = 0; sy < scale; sy++) {
                for (int col = 0; col < spriteWidth; col++) {
                    int color = palette[spriteData[rowOffset + col] & 0xFF];
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
