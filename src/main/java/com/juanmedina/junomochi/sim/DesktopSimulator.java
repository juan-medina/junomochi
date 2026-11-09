// SPDX-FileCopyrightText: 2026 Juan Medina
// SPDX-License-Identifier: MIT

package com.juanmedina.junomochi.sim;

import com.juanmedina.junomochi.CharacterActor;
import com.juanmedina.junomochi.Palette;
import com.juanmedina.junomochi.RoomScene;
import com.juanmedina.junomochi.TouchDisplay;
import io.github.jabrena.juno.hardware.display.Display;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Desktop simulator and video recorder for JUnomochi.
 *
 * <p>Run without arguments to open an interactive 960x720 window (click to nudge JUnomochi).
 * Run with {@code --record <mp4-path> <gif-path>} to record a pixel-perfect demo video and GIF.
 */
public final class DesktopSimulator {

    private static final int WINDOW_SCALE = 3;
    private static final int OUT_WIDTH = TouchDisplay.WIDTH * WINDOW_SCALE;
    private static final int OUT_HEIGHT = TouchDisplay.HEIGHT * WINDOW_SCALE;

    private DesktopSimulator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length >= 3 && "--record".equals(args[0])) {
            recordDemo(args[1], args[2]);
        } else {
            runInteractiveWindow();
        }
    }

    private static void runInteractiveWindow() throws Exception {
        var display = new DesktopTouchDisplay();
        display.fillScreen(Display.BLACK);

        var palette = new Palette();
        var actor = CharacterActor.create(display, palette);
        RoomScene.draw(display, palette);

        var startMs = System.currentTimeMillis();
        actor.start(0);

        var image = new BufferedImage(TouchDisplay.WIDTH, TouchDisplay.HEIGHT, BufferedImage.TYPE_INT_RGB);
        var panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                image.setRGB(0, 0, TouchDisplay.WIDTH, TouchDisplay.HEIGHT,
                        display.getFramebuffer(), 0, TouchDisplay.WIDTH);
                g.drawImage(image, 0, 0, OUT_WIDTH, OUT_HEIGHT, null);
            }
        };
        panel.setPreferredSize(new Dimension(OUT_WIDTH, OUT_HEIGHT));
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                display.setTouched(true);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                display.setTouched(false);
            }
        });

        SwingUtilities.invokeAndWait(() -> {
            var frame = new JFrame("JUnomochi Desktop Simulator (Click to nudge!)");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        var wasTouched = false;
        while (true) {
            var now = (int) (System.currentTimeMillis() - startMs);
            var touched = display.isTouched();
            actor.update(now, touched && !wasTouched);
            wasTouched = touched;
            panel.repaint();
            Thread.sleep(16);
        }
    }

    /**
     * Simulates a full 38-second cycle at 30 FPS and streams scaled RGB24 frames to FFmpeg.
     */
    private static void recordDemo(String mp4Path, String gifPath) throws Exception {
        var mp4File = new File(mp4Path);
        if (mp4File.getParentFile() != null) {
            mp4File.getParentFile().mkdirs();
        }

        var display = new DesktopTouchDisplay();
        display.fillScreen(Display.BLACK);

        var palette = new Palette();
        var actor = CharacterActor.create(display, palette);
        RoomScene.draw(display, palette);
        actor.start(0);

        var fps = 30;
        var totalSeconds = 38;
        var totalFrames = totalSeconds * fps;

        // Touch nudge occurs at 31.2s while JUnomochi is slacking by the plants and taunting
        var tapStartMs = 31200;
        var tapEndMs = 31600;
        var tapCenterX = 148;
        var tapCenterY = 152;

        var pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-f", "rawvideo",
                "-pixel_format", "rgb24",
                "-video_size", OUT_WIDTH + "x" + OUT_HEIGHT,
                "-framerate", String.valueOf(fps),
                "-i", "pipe:0",
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-crf", "18",
                "-preset", "fast",
                mp4Path
        );
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        var ffmpeg = pb.start();

        var frameBytes = new byte[OUT_WIDTH * OUT_HEIGHT * 3];
        var wasTouched = false;

        try (var out = new BufferedOutputStream(ffmpeg.getOutputStream(), frameBytes.length)) {
            for (var frameIdx = 0; frameIdx < totalFrames; frameIdx++) {
                var nowMs = (frameIdx * 1000) / fps;
                var touched = nowMs >= tapStartMs && nowMs <= tapEndMs;
                display.setTouched(touched);

                actor.update(nowMs, touched && !wasTouched);
                wasTouched = touched;

                // Upscale 320x240 framebuffer by 3x (960x720) with nearest-neighbor scaling
                var fb = display.getFramebuffer();
                var outIdx = 0;
                var tapRadius = -1;
                if (touched) {
                    tapRadius = 6 + ((nowMs - tapStartMs) * 14) / (tapEndMs - tapStartMs);
                }

                for (var y = 0; y < TouchDisplay.HEIGHT; y++) {
                    var srcRow = y * TouchDisplay.WIDTH;
                    for (var sy = 0; sy < WINDOW_SCALE; sy++) {
                        for (var x = 0; x < TouchDisplay.WIDTH; x++) {
                            var rgb = fb[srcRow + x];
                            if (tapRadius > 0) {
                                var dx = x - tapCenterX;
                                var dy = y - tapCenterY;
                                var distSq = dx * dx + dy * dy;
                                var rMin = (tapRadius - 2) * (tapRadius - 2);
                                var rMax = tapRadius * tapRadius;
                                if (distSq >= rMin && distSq <= rMax) {
                                    rgb = 0xFFFFFF;
                                } else if (distSq <= 4) {
                                    rgb = 0x00FFFF;
                                }
                            }
                            var r = (byte) ((rgb >> 16) & 0xFF);
                            var g = (byte) ((rgb >> 8) & 0xFF);
                            var b = (byte) (rgb & 0xFF);
                            for (var sx = 0; sx < WINDOW_SCALE; sx++) {
                                frameBytes[outIdx++] = r;
                                frameBytes[outIdx++] = g;
                                frameBytes[outIdx++] = b;
                            }
                        }
                    }
                }
                out.write(frameBytes);
            }
        }

        var exitCode = ffmpeg.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("FFmpeg MP4 encoding failed with code " + exitCode);
        }

        // Convert the clean MP4 to a crisp, lightweight pixel-art GIF
        var gifPb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-i", mp4Path,
                "-vf", "fps=12,scale=640:-1:flags=neighbor,split[s0][s1];[s0]palettegen=max_colors=128[p];[s1][p]paletteuse=dither=none",
                gifPath
        );
        gifPb.redirectError(ProcessBuilder.Redirect.INHERIT);
        var gifExit = gifPb.start().waitFor();
        if (gifExit != 0) {
            throw new IllegalStateException("FFmpeg GIF encoding failed with code " + gifExit);
        }
        System.out.println("Recorded " + mp4Path + " and " + gifPath + " successfully!");
    }
}
