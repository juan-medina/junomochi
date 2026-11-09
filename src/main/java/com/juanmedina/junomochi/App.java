// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

import io.github.jabrena.juno.annotations.ArduinoUnoR4WiFiBoard;
import io.github.jabrena.juno.annotations.Board;
import io.github.jabrena.juno.hardware.ArduinoUno;
import io.github.jabrena.juno.hardware.display.Display;

@Board(ArduinoUnoR4WiFiBoard.class)
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        var tft = new WaveshareTftShield();
        tft.fillScreen(Display.BLACK);

        var palette = new Palette();
        var actor = CharacterActor.create(tft, palette);

        RoomScene.draw(tft, palette);

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
