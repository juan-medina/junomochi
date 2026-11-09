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
import java.io.InputStream;

@Board(ArduinoUnoR4WiFiBoard.class)
public final class App {

    private static final int STATE_CODING = 0;
    private static final int STATE_WALK_LEFT = 1;
    private static final int STATE_WALK_RIGHT = 2;

    private static final int FLOOR_PALETTE_INDEX = 1;
    private static final int WALL_COLOR = 0x52AD;
    private static final int BASEBOARD_COLOR = 0x3164;

    private App() {
    }

    private static boolean drawStatic(InputStream in, Sprite temp, WaveshareTftShield tft,
            int w, int h, int x, int y, int scale, Palette palette) {
        if (!temp.reload(in, w, h)) {
            return false;
        }
        temp.draw(tft, x, y, scale, palette);
        return true;
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
        int floorColor = palette.getColor(FLOOR_PALETTE_INDEX);

        // Reusable buffer for static room sprites (desk, PC, paintings, plants, clock, bin)
        Sprite staticSprite = new Sprite();

        // Load dynamic chair sprite (composited with character)
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

        if (palette.size() == 0 || chair == null
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

        int deskX = 256;
        int deskY = 48;
        int chairX = deskX - tileW;
        int charY = deskY + tileW;
        int leftBoundX = 64;
        int stepPx = 4 * scale;

        // 1. Draw top wall, wooden baseboard, and floor background
        tft.fillRect(0, 0, WaveshareTftShield.WIDTH, 66, WALL_COLOR);
        tft.fillRect(0, 66, WaveshareTftShield.WIDTH, 6, BASEBOARD_COLOR);
        tft.fillRect(0, 72, WaveshareTftShield.WIDTH, WaveshareTftShield.HEIGHT - 72, floorColor);

        // 2. Draw top wall decorations (portraits, landscape painting, clock, hanging plant)
        drawStatic(sd.open("hanging_plant.bin"), staticSprite, tft, 16, 32, 8, -24, scale, palette);
        drawStatic(sd.open("small_painting.bin"), staticSprite, tft, 16, 32, 68, -21, scale, palette);
        drawStatic(sd.open("large_painting.bin"), staticSprite, tft, 32, 32, 128, -21, scale, palette);
        drawStatic(sd.open("small_painting_2.bin"), staticSprite, tft, 16, 32, 224, -21, scale, palette);
        drawStatic(sd.open("clock.bin"), staticSprite, tft, 16, 32, 272, -21, scale, palette);

        // 3. Draw floor plants (left side) and trash bin (bottom-right near desk)
        drawStatic(sd.open("plant.bin"), staticSprite, tft, 16, 32, 8, 36, scale, palette);
        drawStatic(sd.open("plant_2.bin"), staticSprite, tft, 16, 32, 8, 140, scale, palette);
        drawStatic(sd.open("bin.bin"), staticSprite, tft, 16, 16, chairX, 192, scale, palette);

        // 4. Draw static desk and PC on the right side of the room
        drawStatic(sd.open("desk_side.bin"), staticSprite, tft, 16, 64, deskX, deskY, scale, palette);
        drawStatic(sd.open("pc_side.bin"), staticSprite, tft, 16, 32, deskX, charY, scale, palette);

        // Start seated at the computer coding
        int state = STATE_CODING;
        int charX = chairX;
        SpriteAnim activeAnim = codingAnim;

        // Initial draw of chair + character over floor color (shifted 3 sprite px up on chair)
        buffer.clear(FLOOR_PALETTE_INDEX);
        buffer.composite(chair, 0);
        buffer.composite(activeAnim.getCurrentFrame(), 0, -3);
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
                        buffer.clear(FLOOR_PALETTE_INDEX);
                        buffer.composite(chair, 0);
                        buffer.composite(activeAnim.getCurrentFrame(), (charX - chairX) / scale);
                        buffer.blit(tft, chairX, charY, scale, palette);
                    } else {
                        // Clear vacated strip to the right of the character with floor color
                        tft.fillRect(charX + tileW, charY, stepPx, charH, floorColor);
                    }

                    if (charX <= leftBoundX) {
                        charX = leftBoundX;
                        state = STATE_WALK_RIGHT;
                        activeAnim = rightAnim;
                    }
                } else if (state == STATE_WALK_RIGHT) {
                    int oldX = charX;
                    charX += stepPx;

                    // Clear vacated strip to the left of the character with floor color
                    tft.fillRect(oldX, charY, stepPx, charH, floorColor);

                    if (charX >= chairX) {
                        charX = chairX;
                        state = STATE_CODING;
                        activeAnim = codingAnim;
                        stateStart = now;
                    }
                }

                // Composite chair (if overlapping) + character at charX and blit
                int charOffsetY = (state == STATE_CODING) ? -3 : 0;
                buffer.clear(FLOOR_PALETTE_INDEX);
                buffer.composite(chair, (chairX - charX) / scale);
                buffer.composite(activeAnim.getCurrentFrame(), 0, charOffsetY);
                buffer.blit(tft, charX, charY, scale, palette);
            }
        }
    }
}
