// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

import io.github.jabrena.juno.annotations.ArduinoUnoR4WiFiBoard;
import io.github.jabrena.juno.annotations.Board;
import io.github.jabrena.juno.hardware.ArduinoUno;
import io.github.jabrena.juno.hardware.display.Display;
import io.github.jabrena.juno.util.Delay;

@Board(ArduinoUnoR4WiFiBoard.class)
public final class App {

    private static final int ERROR_RETRY_MS = 500;

    private App() {
    }

    private static void haltWithError(WaveshareTftShield tft, String message) {
        tft.setCursor(20, 100);
        tft.setTextColor(Display.RED, Display.BLACK);
        tft.setTextSize(2);
        tft.print(message);
        while (true) {
            Delay.millis(ERROR_RETRY_MS);
        }
    }

    public static void main(String[] args) {
        var tft = new WaveshareTftShield();
        tft.fillScreen(Display.BLACK);

        var sd = tft.getSdCard();
        if (!sd.begin(WaveshareTftShield.DEFAULT_SD_CS)) {
            haltWithError(tft, "SD INIT FAILED");
        }

        var palette = Palette.load(sd.open("palette.bin"));
        if (palette.size() == 0) {
            haltWithError(tft, "PALETTE LOAD FAILED");
        }

        var actor = CharacterActor.load(tft, sd, palette);
        if (actor == null) {
            haltWithError(tft, "SPRITE LOAD FAILED");
        }

        RoomScene.draw(tft, sd, palette);

        var clock = ArduinoUno.getClock();
        actor.start(clock.millis());

        var wasTouched = false;
        while (true) {
            var now = clock.millis();
            var touched = tft.isTouched();
            actor.update(now, touched && !wasTouched);
            wasTouched = touched;
        }
    }
}
