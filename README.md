<!--
SPDX-FileCopyrightText: 2026 Juan Medina
SPDX-License-Identifier: MIT
-->

# JUnomochi

**JUnomochi** is a playful proof of concept showcasing what you can build on an **Arduino UNO R4 WiFi** using ahead-of-time (AOT) Java compilation with [JUno](https://github.com/jabrena/juno).

Meet your tiny pixel Java developer! He sits at his desk coding away in a cozy pixel-art office complete with wall portraits, a clock, and potted plants, occasionally sharing random thoughts about Java, AOT compilation, and embedded life. After a while, he gets tired of coding, walks across the room, and slacks off by the plants—dropping playful hints and taunts until you **tap the screen to give him a friendly nudge** and send him right back to his keyboard.

---

## How It Works

1. **Coding at the Desk:** On startup, your JUnomochi sits at his computer coding away, periodically popping up random developer thoughts in a speech bubble.
2. **Slacking Off:** When his coding timer runs out, he stands up, walks across the office floor toward the plants, and takes a break—periodically taunting you or dropping hints that he's waiting on your interrupt.
3. **Give Him a Nudge:** Tap anywhere on the resistive touch screen! He acknowledges your nudge with a playful reply, walks back to his desk, hops into his chair, and gets back to work.

---

## Hardware Setup

- **Microcontroller:** Arduino UNO R4 WiFi
- **Shield:** Waveshare 2.8" TFT Touch LCD Shield ($320 \times 240$, ST7789V / ILI9341 over SPI)
- **Shield Pinout:**
  - `LCD_CS`: Pin 10
  - `LCD_DC`: Pin 7
  - `LCD_BL`: Pin 9 (Backlight control)
  - `TP_CS`: Pin 4 (XPT2046 Touch Chip Select)
  - `TP_IRQ`: Pin 3 (XPT2046 Touch Interrupt, active-low)
  - `SD_CS`: Pin 5 (Deselected / held HIGH)

---

## Architecture & What It Demonstrates with JUno

Everything is written in modern Java and compiled ahead-of-time to native ARM Thumb-2 assembly via **JUno**. All 20 sprites and the 62-color RGB565 palette are span/RLE-compressed into **6.4 KB of read-only Flash (`.rodata`)**, keeping total JUno Arena RAM usage at just **1,004 / 8,192 bytes** with zero allocations after startup:

- [`App.java`](src/main/java/com/juanmedina/junomochi/App.java): Clean entry point that initializes the display, palette, static room scene, and non-blocking game loop.
- [`SpriteAssets.java`](src/main/java/com/juanmedina/junomochi/SpriteAssets.java): Auto-generated constant tables (`DATA` and `PALETTE_COLORS`) that JUno places directly into MCU Flash (`.rodata`).
- [`RoomScene.java`](src/main/java/com/juanmedina/junomochi/RoomScene.java): Composes the static office background, wall gallery (portraits, landscape painting, clock, hanging plant), floor plants, and workstation directly from Flash.
- [`CharacterActor.java`](src/main/java/com/juanmedina/junomochi/CharacterActor.java): Manages the character's animations, movement, chair compositing, and state machine (`STATE_CODING`, `STATE_WALK_LEFT`, `STATE_TIRED`, `STATE_BACK_MSG`, `STATE_WALK_RIGHT`).
- [`Phrases.java`](src/main/java/com/juanmedina/junomochi/Phrases.java): Zero-allocation catalog of playful Java and embedded-developer quips for coding, taking breaks, taunting for a tap, and returning to work.
- [`TileBuffer.java`](src/main/java/com/juanmedina/junomochi/TileBuffer.java): A 512-byte (`16×32`) software compositing buffer in RAM that blends the character and chair over the floor color and blits in a single SPI window transaction for 100% flicker-free animation.
- [`SpeechBubble.java`](src/main/java/com/juanmedina/junomochi/SpeechBubble.java): Renders and auto-dismisses pixel-art speech bubbles at dynamic screen positions.
- [`Sprite.java`](src/main/java/com/juanmedina/junomochi/Sprite.java), [`SpriteAnim.java`](src/main/java/com/juanmedina/junomochi/SpriteAnim.java), & [`Palette.java`](src/main/java/com/juanmedina/junomochi/Palette.java): Lightweight Flash-offset sprite wrapper, tick-based animation controller, and 16-bit RGB565 palette lookup.
- [`WaveshareTftShield.java`](src/main/java/com/juanmedina/junomochi/WaveshareTftShield.java): Zero-allocation SPI display and XPT2046 touch driver implementing JUno's `Display` interface.

---

## Step-by-Step Instructions

### 1. Prerequisites

- **Java Development Kit (JDK) 27+**
- **Arduino CLI** (with the `arduino:renesas_uno` core installed)
- **Python 3** (optional, only needed if re-converting PNG sprites in [`graphics/`](graphics/) into [`SpriteAssets.java`](src/main/java/com/juanmedina/junomochi/SpriteAssets.java))
- **Git**

### 2. Initialize Dependencies

Run the setup script once to install the local JUno dependencies and Arduino core:

**Windows (PowerShell):**
```powershell
.\scripts\setup.ps1
```

**macOS / Linux:**
```bash
chmod +x ./scripts/*.sh ./mvnw
./scripts/setup.sh
```

### 3. Compile & Flash to the Board

No MicroSD card is needed—all graphics are embedded in the firmware! Connect your Arduino UNO R4 WiFi via USB and run the flash script:

**Windows (PowerShell):**
```powershell
.\scripts\flash.ps1
```

**macOS / Linux:**
```bash
./scripts/flash.sh
```

*(Optional: If you edit any PNG sprites in [`graphics/`](graphics/), run `.\scripts\convert_sprites.ps1` or `./scripts/convert_sprites.sh` to regenerate [`SpriteAssets.java`](src/main/java/com/juanmedina/junomochi/SpriteAssets.java)).*

Once flashed, watch your JUnomochi code at his desk—and whenever he wanders off to slack by the plants, tap the screen to nudge him back to work!

---

## Credits & License

- **Sprites & Office Pixel Art:** Sourced from [Pixel Agents](https://github.com/pixel-agents-hq/pixel-agents) by [Pablo De Lucca](https://github.com/pablodelucca), featuring character sprites based on *MetroCity* by [JIK-A-4](https://jik-a-4.itch.io/) and office furniture/decorations from the *Office Interior Tileset (16x16)* by [Donarg](https://donarg.itch.io/).
- **Compiler & Hardware SDK:** Powered by [JUno](https://github.com/jabrena/juno).
- **License:** Licensed under the MIT License. See [LICENSE](LICENSE) for details.
