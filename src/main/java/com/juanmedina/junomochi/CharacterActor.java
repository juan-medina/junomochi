// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

/**
 * Manages the developer character's animations, state machine, movement, and speech bubbles.
 */
public final class CharacterActor {

    private static final int STATE_CODING = 0;
    private static final int STATE_WALK_LEFT = 1;
    private static final int STATE_TIRED = 2;
    private static final int STATE_BACK_MSG = 3;
    private static final int STATE_WALK_RIGHT = 4;

    private static final int WALK_FRAME_MS = 180;
    private static final int CODING_FRAME_MS = 300;
    private static final int INITIAL_CODING_MS = 20000;
    private static final int REPEAT_CODING_MS = 40000;

    private static final int BUBBLE_DURATION_MS = 2600;
    private static final int CODING_CHAT_INTERVAL_MS = 8500;
    private static final int TAUNT_INTERVAL_MS = 6500;
    private static final int BACK_MSG_MS = 1800;

    private static final int BUBBLE_W = 104;
    private static final int BUBBLE_H = 34;
    private static final int BUBBLE_Y = 74;
    private static final int LEFT_BUBBLE_X = 60;
    private static final int LEFT_TAIL_X = RoomScene.LEFT_BOUND_X + 20;
    private static final int DESK_BUBBLE_X = 112;
    private static final int DESK_TAIL_X = RoomScene.CHAIR_X - 4;

    private static final int STEP_PX = 4 * RoomScene.SCALE;
    private static final int CHAIR_OFFSET_Y = -3;

    private final WaveshareTftShield tft;
    private final Palette palette;
    private final int floorColor;
    private final TileBuffer buffer;
    private final SpeechBubble bubble;

    private final Sprite chair;
    private final Sprite downStand;
    private final SpriteAnim leftAnim;
    private final SpriteAnim rightAnim;
    private final SpriteAnim codingAnim;

    private int state;
    private int codingDurationMs;
    private int charX;
    private int stateStart;
    private int bubbleStart;
    private int nextChatTime;
    private int rngState;
    private SpriteAnim activeAnim;

    private CharacterActor(
            WaveshareTftShield tft,
            Palette palette,
            Sprite chair,
            Sprite downStand,
            SpriteAnim leftAnim,
            SpriteAnim rightAnim,
            SpriteAnim codingAnim) {
        this.tft = tft;
        this.palette = palette;
        this.floorColor = palette.getColor(RoomScene.FLOOR_PALETTE_INDEX);
        this.buffer = new TileBuffer(16, 32);
        this.bubble = new SpeechBubble(BUBBLE_W, BUBBLE_H);
        this.chair = chair;
        this.downStand = downStand;
        this.leftAnim = leftAnim;
        this.rightAnim = rightAnim;
        this.codingAnim = codingAnim;
        this.state = STATE_CODING;
        this.codingDurationMs = INITIAL_CODING_MS;
        this.charX = RoomScene.CHAIR_X;
        this.rngState = 0x5A3C9E17;
        this.activeAnim = codingAnim;
    }

    /**
     * Creates the character actor backed by embedded Flash sprite assets.
     */
    public static CharacterActor create(WaveshareTftShield tft, Palette palette) {
        var chair = new Sprite(16, 32, SpriteAssets.CHAIR_SIDE);
        var downStand = new Sprite(16, 32, SpriteAssets.WALK_DOWN_STAND);

        var leftStand = new Sprite(16, 32, SpriteAssets.WALK_LEFT_STAND);
        var leftStepLeft = new Sprite(16, 32, SpriteAssets.WALK_LEFT_STEP_LEFT);
        var leftStepRight = new Sprite(16, 32, SpriteAssets.WALK_LEFT_STEP_RIGHT);

        var rightStand = new Sprite(16, 32, SpriteAssets.WALK_RIGHT_STAND);
        var rightStepLeft = new Sprite(16, 32, SpriteAssets.WALK_RIGHT_STEP_LEFT);
        var rightStepRight = new Sprite(16, 32, SpriteAssets.WALK_RIGHT_STEP_RIGHT);

        var typing0 = new Sprite(16, 32, SpriteAssets.TYPING_RIGHT_FRAME0);
        var typing1 = new Sprite(16, 32, SpriteAssets.TYPING_RIGHT_FRAME1);

        var leftAnim = new SpriteAnim(
                new Sprite[] { leftStand, leftStepLeft, leftStand, leftStepRight }, WALK_FRAME_MS);
        var rightAnim = new SpriteAnim(
                new Sprite[] { rightStand, rightStepLeft, rightStand, rightStepRight }, WALK_FRAME_MS);
        var codingAnim = new SpriteAnim(
                new Sprite[] { typing0, typing1 }, CODING_FRAME_MS);

        return new CharacterActor(tft, palette, chair, downStand, leftAnim, rightAnim, codingAnim);
    }

    /**
     * Draws the initial seated state and starts the state timer.
     */
    public void start(int nowMs) {
        this.stateStart = nowMs;
        this.nextChatTime = nowMs + 4000;
        drawAtChair(activeAnim.getCurrentFrame(), CHAIR_OFFSET_Y);
    }

    /**
     * Advances the character state machine on each tick of the main loop.
     */
    public void update(int now, boolean touchedEdge) {
        switch (state) {
            case STATE_CODING -> updateCoding(now);
            case STATE_WALK_LEFT -> updateWalkLeft(now);
            case STATE_TIRED -> updateTired(now, touchedEdge);
            case STATE_BACK_MSG -> updateBackMsg(now);
            case STATE_WALK_RIGHT -> updateWalkRight(now);
            default -> {
            }
        }
    }

    private int nextRandom(int bound, int now) {
        rngState = rngState * 1664525 + 1013904223 + now;
        var val = (rngState >>> 8) & 0x7FFF;
        return val % bound;
    }

    private void updateCoding(int now) {
        if (bubble.isVisible() && now - bubbleStart >= BUBBLE_DURATION_MS) {
            bubble.hide(tft, floorColor);
        }

        if (now - stateStart >= codingDurationMs) {
            bubble.hide(tft, floorColor);
            state = STATE_WALK_LEFT;
            activeAnim = leftAnim;
            return;
        }

        if (!bubble.isVisible() && now - nextChatTime >= 0
                && (stateStart + codingDurationMs) - now > BUBBLE_DURATION_MS + 1000) {
            var idx = nextRandom(Phrases.CODING_COUNT, now);
            Phrases.showCoding(bubble, tft, DESK_BUBBLE_X, BUBBLE_Y, DESK_TAIL_X, idx, floorColor);
            bubbleStart = now;
            nextChatTime = now + CODING_CHAT_INTERVAL_MS;
        }

        if (activeAnim.update(now)) {
            drawAtChair(activeAnim.getCurrentFrame(), CHAIR_OFFSET_Y);
        }
    }

    private void updateWalkLeft(int now) {
        if (!activeAnim.update(now)) {
            return;
        }
        var oldX = charX;
        charX -= STEP_PX;
        var frame = activeAnim.getCurrentFrame();

        if (oldX > RoomScene.CHAIR_X - RoomScene.TILE_W) {
            // Restore uncovered portion of the chair seamlessly
            buffer.clear(RoomScene.FLOOR_PALETTE_INDEX);
            buffer.composite(chair, 0);
            buffer.composite(frame, (charX - RoomScene.CHAIR_X) / RoomScene.SCALE);
            buffer.blit(tft, RoomScene.CHAIR_X, RoomScene.CHAR_Y, RoomScene.SCALE, palette);
        } else {
            tft.fillRect(charX + RoomScene.TILE_W, RoomScene.CHAR_Y, STEP_PX, RoomScene.CHAR_H, floorColor);
        }

        if (charX <= RoomScene.LEFT_BOUND_X) {
            charX = RoomScene.LEFT_BOUND_X;
            state = STATE_TIRED;
            stateStart = now;
            bubbleStart = now;
            nextChatTime = now + TAUNT_INTERVAL_MS;
            drawAtCurrentPos(downStand, 0);
            var idx = nextRandom(Phrases.TIRED_COUNT, now);
            Phrases.showTired(bubble, tft, LEFT_BUBBLE_X, BUBBLE_Y, LEFT_TAIL_X, idx, floorColor);
        } else {
            drawAtCurrentPos(frame, 0);
        }
    }

    private void updateTired(int now, boolean touchedEdge) {
        if (bubble.isVisible() && now - bubbleStart >= BUBBLE_DURATION_MS) {
            bubble.hide(tft, floorColor);
        } else if (!bubble.isVisible() && now - nextChatTime >= 0) {
            var idx = nextRandom(Phrases.TAUNT_COUNT, now);
            Phrases.showTaunt(bubble, tft, LEFT_BUBBLE_X, BUBBLE_Y, LEFT_TAIL_X, idx, floorColor);
            bubbleStart = now;
            nextChatTime = now + TAUNT_INTERVAL_MS;
        }

        if (touchedEdge) {
            state = STATE_BACK_MSG;
            stateStart = now;
            bubbleStart = now;
            var idx = nextRandom(Phrases.BACK_COUNT, now);
            Phrases.showBack(bubble, tft, LEFT_BUBBLE_X, BUBBLE_Y, LEFT_TAIL_X, idx, floorColor);
        }
    }

    private void updateBackMsg(int now) {
        if (now - stateStart >= BACK_MSG_MS) {
            bubble.hide(tft, floorColor);
            state = STATE_WALK_RIGHT;
            activeAnim = rightAnim;
        }
    }

    private void updateWalkRight(int now) {
        if (!activeAnim.update(now)) {
            return;
        }
        var oldX = charX;
        charX += STEP_PX;

        tft.fillRect(oldX, RoomScene.CHAR_Y, STEP_PX, RoomScene.CHAR_H, floorColor);

        if (charX >= RoomScene.CHAIR_X) {
            charX = RoomScene.CHAIR_X;
            state = STATE_CODING;
            codingDurationMs = REPEAT_CODING_MS;
            activeAnim = codingAnim;
            stateStart = now;
            nextChatTime = now + 5000;
            drawAtChair(activeAnim.getCurrentFrame(), CHAIR_OFFSET_Y);
        } else {
            drawAtCurrentPos(activeAnim.getCurrentFrame(), 0);
        }
    }

    private void drawAtChair(Sprite frame, int offsetY) {
        buffer.clear(RoomScene.FLOOR_PALETTE_INDEX);
        buffer.composite(chair, 0);
        buffer.composite(frame, 0, offsetY);
        buffer.blit(tft, RoomScene.CHAIR_X, RoomScene.CHAR_Y, RoomScene.SCALE, palette);
    }

    private void drawAtCurrentPos(Sprite frame, int offsetY) {
        buffer.clear(RoomScene.FLOOR_PALETTE_INDEX);
        buffer.composite(chair, (RoomScene.CHAIR_X - charX) / RoomScene.SCALE);
        buffer.composite(frame, 0, offsetY);
        buffer.blit(tft, charX, RoomScene.CHAR_Y, RoomScene.SCALE, palette);
    }
}
