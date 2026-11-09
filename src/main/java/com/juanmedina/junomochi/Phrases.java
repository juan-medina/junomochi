// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

/**
 * Playful Java developer phrases for coding, slacking off, and getting nudged back to work.
 */
public final class Phrases {

    public static final int CODING_COUNT = 8;
    public static final int TIRED_COUNT = 6;
    public static final int TAUNT_COUNT = 8;
    public static final int BACK_COUNT = 6;

    private Phrases() {
    }

    public static void showCoding(SpeechBubble bubble, TouchDisplay tft,
            int x, int y, int tailX, int index, int bgColor) {
        switch (index) {
            case 0 -> bubble.showAt(tft, x, y, tailX, "var everything!", "Java 27 rocks!", bgColor);
            case 1 -> bubble.showAt(tft, x, y, tailX, "Zero heap alloc", "in this loop!", bgColor);
            case 2 -> bubble.showAt(tft, x, y, tailX, "Who needs a", "FactoryBean?", bgColor);
            case 3 -> bubble.showAt(tft, x, y, tailX, "Compiling Java", "to ARM ASM!", bgColor);
            case 4 -> bubble.showAt(tft, x, y, tailX, "8KB Arena and", "still plenty!", bgColor);
            case 5 -> bubble.showAt(tft, x, y, tailX, "Wait, is that a", "NullPointer?!", bgColor);
            case 6 -> bubble.showAt(tft, x, y, tailX, "It works on my", "Arduino!", bgColor);
            default -> bubble.showAt(tft, x, y, tailX, "Just one more", "interface...", bgColor);
        }
    }

    public static void showTired(SpeechBubble bubble, TouchDisplay tft,
            int x, int y, int tailX, int index, int bgColor) {
        switch (index) {
            case 0 -> bubble.showAt(tft, x, y, tailX, "My brain hit a", "StackOverflow!", bgColor);
            case 1 -> bubble.showAt(tft, x, y, tailX, "Out of coffee", "Exception!", bgColor);
            case 2 -> bubble.showAt(tft, x, y, tailX, "Too many", "abstractions..", bgColor);
            case 3 -> bubble.showAt(tft, x, y, tailX, "Garbage collect", "my soul pls...", bgColor);
            case 4 -> bubble.showAt(tft, x, y, tailX, "Thread.sleep()", "sounds nice...", bgColor);
            default -> bubble.showAt(tft, x, y, tailX, "I'm tired of", "boilerplate!", bgColor);
        }
    }

    public static void showTaunt(SpeechBubble bubble, TouchDisplay tft,
            int x, int y, int tailX, int index, int bgColor) {
        switch (index) {
            case 0 -> bubble.showAt(tft, x, y, tailX, "Tap the screen", "if you dare!", bgColor);
            case 1 -> bubble.showAt(tft, x, y, tailX, "Still waiting", "on maven clean", bgColor);
            case 2 -> bubble.showAt(tft, x, y, tailX, "No screen tap,", "no commits!", bgColor);
            case 3 -> bubble.showAt(tft, x, y, tailX, "I'm blocked on", "your touch IRQ", bgColor);
            case 4 -> bubble.showAt(tft, x, y, tailX, "Poke the LCD", "Scrum Master!", bgColor);
            case 5 -> bubble.showAt(tft, x, y, tailX, "100% idle...", "tap to wake me", bgColor);
            case 6 -> bubble.showAt(tft, x, y, tailX, "Touch screen to", "deploy hotfix!", bgColor);
            default -> bubble.showAt(tft, x, y, tailX, "Nice plants...", "tap to resume!", bgColor);
        }
    }

    public static void showBack(SpeechBubble bubble, TouchDisplay tft,
            int x, int y, int tailX, int index, int bgColor) {
        switch (index) {
            case 0 -> bubble.showAt(tft, x, y, tailX, "Ok, ok! Back", "to the IDE!", bgColor);
            case 1 -> bubble.showAt(tft, x, y, tailX, "Fine! Opening", "a new PR now!", bgColor);
            case 2 -> bubble.showAt(tft, x, y, tailX, "Interrupt", "received! Omw!", bgColor);
            case 3 -> bubble.showAt(tft, x, y, tailX, "Alright, time", "to ship it!", bgColor);
            case 4 -> bubble.showAt(tft, x, y, tailX, "Resuming main", "thread now!", bgColor);
            default -> bubble.showAt(tft, x, y, tailX, "Back to JUno", "wizardry!", bgColor);
        }
    }
}

