// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

import io.github.jabrena.juno.annotations.ArduinoUnoR4WiFiBoard;
import io.github.jabrena.juno.annotations.Board;
import io.github.jabrena.juno.hardware.ArduinoUno;
import io.github.jabrena.juno.hardware.device.sd.SdCard;
import io.github.jabrena.juno.hardware.display.Display;
import io.github.jabrena.juno.hardware.time.Clock;
import io.github.jabrena.juno.util.Delay;

@Board(ArduinoUnoR4WiFiBoard.class)
public final class App {

    private static final int STATE_CODING = 0;
    private static final int STATE_WALK_LEFT = 1;
    private static final int STATE_WALK_RIGHT = 2;

    private App() {
    }

    public static void main(String[] args) {
        WaveshareTftShield tft = new WaveshareTftShield();
        tft.fillScreen(Display.BLACK);

        SdCard sd = tft.getSdCard();
        if (!sd.begin(WaveshareTftShield.DEFAULT_SD_CS)) {
            tft.setCursor(20, 100);
            tft.setTextColor(Display.RED, Display.BLACK);
            tft.setTextSize(2);
            tft.print("SD INIT FAILED");
            while (true) {
                Delay.millis(500);
            }
        }

        // Load shared palette
        Palette palette = Palette.load(sd.open("palette.bin"));

        // Load room furniture sprites (side orientation)
        Sprite desk = Sprite.load(sd.open("desk_side.bin"), 16, 64);
        Sprite pc = Sprite.load(sd.open("pc_side.bin"), 16, 32);
        Sprite chair = Sprite.load(sd.open("chair_side.bin"), 16, 32);

        // Load walk left frames
        Sprite leftStand = Sprite.load(sd.open("walk_left_stand.bin"), 16, 32);
        Sprite leftStepLeft = Sprite.load(sd.open("walk_left_step_left.bin"), 16, 32);
        Sprite leftStepRight = Sprite.load(sd.open("walk_left_step_right.bin"), 16, 32);

        // Load walk right frames
        Sprite rightStand = Sprite.load(sd.open("walk_right_stand.bin"), 16, 32);
        Sprite rightStepLeft = Sprite.load(sd.open("walk_right_step_left.bin"), 16, 32);
        Sprite rightStepRight = Sprite.load(sd.open("walk_right_step_right.bin"), 16, 32);

        // Load coding (typing right) frames
        Sprite typing0 = Sprite.load(sd.open("typing_right_frame0.bin"), 16, 32);
        Sprite typing1 = Sprite.load(sd.open("typing_right_frame1.bin"), 16, 32);

        if (palette.size() == 0 || desk == null || pc == null || chair == null
                || leftStand == null || rightStand == null || typing0 == null || typing1 == null) {
            tft.setCursor(20, 100);
            tft.setTextColor(Display.RED, Display.BLACK);
            tft.setTextSize(2);
            tft.print("SPRITE LOAD FAILED");
            while (true) {
                Delay.millis(500);
            }
        }

        // Build animations
        SpriteAnim leftAnim = new SpriteAnim(
                new Sprite[] { leftStand, leftStepLeft, leftStand, leftStepRight }, 180);
        SpriteAnim rightAnim = new SpriteAnim(
                new Sprite[] { rightStand, rightStepLeft, rightStand, rightStepRight }, 180);
        SpriteAnim codingAnim = new SpriteAnim(
                new Sprite[] { typing0, typing1 }, 300);

        // 16x32 local composite buffer (512 bytes RAM)
        TileBuffer buffer = new TileBuffer(16, 32);

        // Room layout on 320x240 screen at scale 3 (16x32 = 48x96 px, 16x64 desk = 48x192 px)
        int scale = 3;
        int tileW = 16 * scale;
        int charH = 32 * scale;
        int deskH = 64 * scale;

        int deskX = 240;
        int deskY = (WaveshareTftShield.HEIGHT - deskH) / 2;
        int chairX = deskX - tileW;
        int charY = deskY + tileW;
        int leftBoundX = 36;
        int stepPx = 4 * scale;

        // Draw static desk and PC on the right side of the room
        desk.draw(tft, deskX, deskY, scale, palette);
        pc.draw(tft, deskX, charY, scale, palette);

        // Start seated at the computer coding
        int state = STATE_CODING;
        int charX = chairX;
        SpriteAnim activeAnim = codingAnim;

        // Initial draw of chair + character
        buffer.clear(0);
        buffer.composite(chair, 0);
        buffer.composite(activeAnim.getCurrentFrame(), 0);
        buffer.blit(tft, charX, charY, scale, palette);

        Clock clock = ArduinoUno.getClock();
        int stateStart = clock.millis();

        while (true) {
            int now = clock.millis();

            // After 4 seconds of coding, stand up and walk left across the room
            if (state == STATE_CODING && now - stateStart >= 4000) {
                state = STATE_WALK_LEFT;
                activeAnim = leftAnim;
            }

            if (activeAnim.update(now)) {
                if (state == STATE_WALK_LEFT) {
                    int oldX = charX;
                    charX -= stepPx;

                    if (oldX > chairX - tileW) {
                        // Restore uncovered portion of the chair seamlessly
                        buffer.clear(0);
                        buffer.composite(chair, 0);
                        buffer.composite(activeAnim.getCurrentFrame(), (charX - chairX) / scale);
                        buffer.blit(tft, chairX, charY, scale, palette);
                    } else {
                        // Clear vacated strip to the right of the character
                        tft.fillRect(charX + tileW, charY, stepPx, charH, Display.BLACK);
                    }

                    if (charX <= leftBoundX) {
                        charX = leftBoundX;
                        state = STATE_WALK_RIGHT;
                        activeAnim = rightAnim;
                    }
                } else if (state == STATE_WALK_RIGHT) {
                    int oldX = charX;
                    charX += stepPx;

                    // Clear vacated strip to the left of the character
                    tft.fillRect(oldX, charY, stepPx, charH, Display.BLACK);

                    if (charX >= chairX) {
                        charX = chairX;
                        state = STATE_CODING;
                        activeAnim = codingAnim;
                        stateStart = now;
                    }
                }

                // Composite chair (if overlapping) + character at charX and blit
                buffer.clear(0);
                buffer.composite(chair, (chairX - charX) / scale);
                buffer.composite(activeAnim.getCurrentFrame(), 0);
                buffer.blit(tft, charX, charY, scale, palette);
            }
        }
    }
}
