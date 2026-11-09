// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi;

/**
 * Manages frame sequencing and tick-based timing for sprite animations.
 */
public final class SpriteAnim {

    private final Sprite[] frames;
    private final int frameDurationMs;
    private int currentFrame;
    private int lastTick;

    public SpriteAnim(Sprite[] frames, int frameDurationMs) {
        this.frames = frames;
        this.frameDurationMs = frameDurationMs;
        this.currentFrame = 0;
        this.lastTick = 0;
    }

    /**
     * Updates the animation based on the current clock ticks in milliseconds.
     * Returns true if the frame changed on this update.
     */
    public boolean update(int currentTickMs) {
        if (frames == null || frames.length == 0) {
            return false;
        }
        if (currentTickMs - lastTick >= frameDurationMs) {
            lastTick = currentTickMs;
            currentFrame = (currentFrame + 1) % frames.length;
            return true;
        }
        return false;
    }

    public Sprite getCurrentFrame() {
        if (frames == null || frames.length == 0) {
            return null;
        }
        return frames[currentFrame];
    }

    public int getCurrentFrameIndex() {
        return currentFrame;
    }

    public int getWidth() {
        Sprite frame = getCurrentFrame();
        return frame != null ? frame.getWidth() : 0;
    }

    public int getHeight() {
        Sprite frame = getCurrentFrame();
        return frame != null ? frame.getHeight() : 0;
    }

    public void draw(WaveshareTftShield display, int x, int y, int scale, boolean flipX, Palette palette) {
        Sprite frame = getCurrentFrame();
        if (frame != null && display != null) {
            frame.draw(display, x, y, scale, flipX, palette);
        }
    }

    public void draw(WaveshareTftShield display, int x, int y, int scale, Palette palette) {
        draw(display, x, y, scale, false, palette);
    }
}

