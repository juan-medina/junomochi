// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomichi;

import io.github.jabrena.juno.annotations.ArduinoUnoR4WiFiBoard;
import io.github.jabrena.juno.annotations.Board;
import io.github.jabrena.juno.hardware.ArduinoUno;
import io.github.jabrena.juno.hardware.gpio.DigitalOutput;
import io.github.jabrena.juno.hardware.led.LedCanvas;
import io.github.jabrena.juno.hardware.led.LedMatrix;
import io.github.jabrena.juno.util.Delay;

/**
 * Junomichi: A Tamagotchi-like virtual pet for Arduino UNO R4 WiFi.
 * Runs natively on the 12x8 LED matrix using JUno AOT Java compiler.
 */
@Board(ArduinoUnoR4WiFiBoard.class)
public final class App {

    // 12-bit row masks for 12x8 LED matrix (Row 0 to 7)

    // IDLE Frame 0: Eyes open looking forward
    private static final int[] POSE_IDLE_A = {
        0x000, // ............
        0x30C, // ..XX....XX.. (ears)
        0x7FE, // .XXXXXXXXXX. (head)
        0x6DE, // .XX.XXXX.XX. (open eyes)
        0x7FE, // .XXXXXXXXXX. (cheeks)
        0x73E, // .XXXX..XXXX. (small mouth)
        0x3FC, // ..XXXXXXXX.. (body)
        0x294  // ..X.X..X.X.. (feet)
    };

    // IDLE Frame 1: Blink (eyes closed)
    private static final int[] POSE_IDLE_BLINK = {
        0x000, // ............
        0x30C, // ..XX....XX..
        0x7FE, // .XXXXXXXXXX.
        0x7FE, // .XXXXXXXXXX. (blink: eyes closed)
        0x6DE, // .XX.XXXX.XX. (slit)
        0x73E, // .XXXX..XXXX.
        0x3FC, // ..XXXXXXXX..
        0x294  // ..X.X..X.X..
    };

    // HAPPY Frame 0: Jump up!
    private static final int[] POSE_HAPPY_UP = {
        0x30C, // ..XX....XX.. (ears jumped to row 0)
        0x7FE, // .XXXXXXXXXX.
        0x5BE, // .X.XX.XX.XX. (happy squint eyes ^_^)
        0x70E, // .XXX....XXX. (open smile :D)
        0x7FE, // .XXXXXXXXXX.
        0x3FC, // ..XXXXXXXX..
        0x294, // ..X.X..X.X.. (feet off ground)
        0x000  // ............
    };

    // HAPPY Frame 1: Ground cheer with heart sparkle
    private static final int[] POSE_HAPPY_DOWN = {
        0x020, // ......X..... (sparkle)
        0x30C, // ..XX....XX..
        0x7FE, // .XXXXXXXXXX.
        0x5BE, // .X.XX.XX.XX. (happy squint)
        0x73E, // .XXXX..XXXX.
        0x7FE, // .XXXXXXXXXX.
        0x3FC, // ..XXXXXXXX..
        0x33C  // ..XX....XX.. (feet planted)
    };

    // EATING Frame 0: Snack floating toward mouth
    private static final int[] POSE_EATING_APPROACH = {
        0x000, // ............
        0x30C, // ..XX....XX..
        0x7FE, // .XXXXXXXXXX.
        0x6DE, // .XX.XXXX.XX. (eyes watching snack)
        0x7FE, // .XXXXXXXXXX.
        0x70E, // .XXX....XXX. (mouth wide open)
        0x3FC, // ..XXXXXXXX..
        0x294  // ..X.X..X.X..
    };

    // EATING Frame 1: Nom nom nom! (mouth chew)
    private static final int[] POSE_EATING_CHEW = {
        0x000, // ............
        0x30C, // ..XX....XX..
        0x7FE, // .XXXXXXXXXX.
        0x5BE, // .X.XX.XX.XX. (content eyes)
        0x7FE, // .XXXXXXXXXX.
        0x7BE, // .XXXXX.XXXX. (chewing mouth)
        0x3FC, // ..XXXXXXXX..
        0x294  // ..X.X..X.X..
    };

    // SLEEP Frame 0: Sleeping with small 'z'
    private static final int[] POSE_SLEEP_A = {
        0x000, // ............
        0x006, // .........XX. ('z' start)
        0x30C, // ..XX....XX..
        0x7FE, // .XXXXXXXXXX.
        0x5BE, // .X.XX.XX.XX. (closed resting eyes)
        0x7FE, // .XXXXXXXXXX.
        0x3FC, // ..XXXXXXXX..
        0x198  // ...XX..XX... (feet tucked in)
    };

    // SLEEP Frame 1: Sleeping with floating 'Z'
    private static final int[] POSE_SLEEP_B = {
        0x00E, // ........XXX. ('Z' floating up)
        0x004, // .........X..
        0x30E, // ..XX....XXX.
        0x7FE, // .XXXXXXXXXX.
        0x5BE, // .X.XX.XX.XX.
        0x7FE, // .XXXXXXXXXX.
        0x3FC, // ..XXXXXXXX..
        0x198  // ...XX..XX...
    };

    private static void renderPose(boolean[][] frame, int[] pose, int snackX, int snackY) {
        LedCanvas.clear(frame);
        for (int y = 0; y < LedCanvas.HEIGHT; y++) {
            int rowMask = pose[y];
            for (int x = 0; x < LedCanvas.MAX_WIDTH; x++) {
                if ((rowMask & (1 << (11 - x))) != 0) {
                    LedCanvas.setPixel(frame, x, y);
                }
            }
        }
        if (snackX >= 0 && snackX < LedCanvas.MAX_WIDTH && snackY >= 0 && snackY < LedCanvas.HEIGHT) {
            LedCanvas.setPixel(frame, snackX, snackY);
        }
        LedCanvas.show(frame);
    }

    public static void main(String[] args) {
        DigitalOutput led = DigitalOutput.of(ArduinoUno.getGpio().getBuiltinLed());
        LedMatrix matrix = ArduinoUno.getLedMatrix();
        matrix.begin();

        boolean[][] frame = new boolean[LedCanvas.HEIGHT][LedCanvas.MAX_WIDTH];

        while (true) {
            // State 1: IDLE & BLINK (4 cycles of breathing & blinking)
            for (int i = 0; i < 4; i++) {
                led.high();
                renderPose(frame, POSE_IDLE_A, -1, -1);
                Delay.millis(700);

                led.low();
                renderPose(frame, POSE_IDLE_BLINK, -1, -1);
                Delay.millis(120);

                renderPose(frame, POSE_IDLE_A, -1, -1);
                Delay.millis(500);
            }

            // State 2: HAPPY BOUNCE (jump up and down excitedly)
            for (int i = 0; i < 5; i++) {
                led.high();
                renderPose(frame, POSE_HAPPY_UP, -1, -1);
                Delay.millis(180);

                led.low();
                renderPose(frame, POSE_HAPPY_DOWN, -1, -1);
                Delay.millis(180);
            }

            // State 3: SNACK TIME! (food dot approaches mouth, then gets eaten)
            for (int x = 11; x >= 6; x--) {
                led.high();
                renderPose(frame, POSE_EATING_APPROACH, x, 5);
                Delay.millis(150);
                led.low();
                Delay.millis(50);
            }

            // Chew 4 times
            for (int i = 0; i < 4; i++) {
                led.high();
                renderPose(frame, POSE_EATING_CHEW, -1, -1);
                Delay.millis(160);
                led.low();
                renderPose(frame, POSE_EATING_APPROACH, -1, -1);
                Delay.millis(160);
            }

            // Happy reaction after meal
            for (int i = 0; i < 3; i++) {
                renderPose(frame, POSE_HAPPY_UP, -1, -1);
                Delay.millis(150);
                renderPose(frame, POSE_HAPPY_DOWN, -1, -1);
                Delay.millis(150);
            }

            // State 4: SLEEP & SNOOZE (pet naps with floating Zs)
            for (int i = 0; i < 4; i++) {
                led.high();
                renderPose(frame, POSE_SLEEP_A, -1, -1);
                Delay.millis(600);

                led.low();
                renderPose(frame, POSE_SLEEP_B, -1, -1);
                Delay.millis(600);
            }

            // Waking up stretch
            renderPose(frame, POSE_IDLE_BLINK, -1, -1);
            Delay.millis(300);
        }
    }
}
