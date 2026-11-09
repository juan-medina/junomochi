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
- **MicroSD Card:** Standard FAT32/FAT16 MicroSD card inserted into the shield's onboard slot
- **Shield Pinout:**
  - `LCD_CS`: Pin 10
  - `LCD_DC`: Pin 7
  - `LCD_BL`: Pin 9 (Backlight control)
  - `TP_CS`: Pin 4 (XPT2046 Touch Chip Select)
  - `TP_IRQ`: Pin 3 (XPT2046 Touch Interrupt, active-low)
  - `SD_CS`: Pin 5 (MicroSD Chip Select)

---

## Architecture & What It Demonstrates with JUno

Everything is written in modern Java and compiled ahead-of-time to native ARM Thumb-2 assembly via **JUno**, fitting comfortably inside JUno's **8 KB Arena** (~6.3 KB used) with zero allocations inside the main loop:

- [`App.java`](src/main/java/com/juanmedina/junomochi/App.java): Clean entry point that initializes the display, SD card, palette, static room scene, and non-blocking game loop.
- [`RoomScene.java`](src/main/java/com/juanmedina/junomochi/RoomScene.java): Composes the static office background, wall gallery (portraits, landscape painting, clock, hanging plant), floor plants, and workstation by streaming `.bin` assets from the SD card through a single reusable `Sprite` buffer (zero extra heap overhead).
- [`CharacterActor.java`](src/main/java/com/juanmedina/junomochi/CharacterActor.java): Manages the character's animations, movement, chair compositing, and state machine (`STATE_CODING`, `STATE_WALK_LEFT`, `STATE_TIRED`, `STATE_BACK_MSG`, `STATE_WALK_RIGHT`).
- [`Phrases.java`](src/main/java/com/juanmedina/junomochi/Phrases.java): Zero-allocation catalog of playful Java and embedded-developer quips for coding, taking breaks, taunting for a tap, and returning to work.
- [`TileBuffer.java`](src/main/java/com/juanmedina/junomochi/TileBuffer.java): A 512-byte (`16×32`) software compositing buffer in RAM that blends the character and chair over the floor color and blits in a single SPI window transaction for 100% flicker-free animation.
- [`SpeechBubble.java`](src/main/java/com/juanmedina/junomochi/SpeechBubble.java): Renders and auto-dismisses pixel-art speech bubbles at dynamic screen positions.
- [`Sprite.java`](src/main/java/com/juanmedina/junomochi/Sprite.java), [`SpriteAnim.java`](src/main/java/com/juanmedina/junomochi/SpriteAnim.java), & [`Palette.java`](src/main/java/com/juanmedina/junomochi/Palette.java): Span/RLE-compressed sprite loader, tick-based animation controller, and 16-bit RGB565 palette lookup.
- [`WaveshareTftShield.java`](src/main/java/com/juanmedina/junomochi/WaveshareTftShield.java): Zero-allocation SPI display and XPT2046 touch driver implementing JUno's `Display` interface.

---

## Step-by-Step Instructions

### 1. Prerequisites

- **Java Development Kit (JDK) 27+**
- **Arduino CLI** (with the `arduino:renesas_uno` core installed)
- **Python 3** (optional, only needed if re-converting PNG sprites in `graphics/`)
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

### 3. Prepare the MicroSD Card

All sprites and the color palette are loaded from the MicroSD card at boot:

1. Format a MicroSD card as **FAT32**.
2. Copy **all `.bin` files** from the [`sdcard/`](sdcard/) directory directly into the **root** folder of the MicroSD card (`palette.bin`, character animations, furniture, paintings, plants, clock, and bin).
   *(If you modify any PNGs in [`graphics/`](graphics/), run `.\scripts\convert_sprites.ps1` or `./scripts\convert_sprites.sh` to regenerate `sdcard/`).*
3. Insert the MicroSD card into the Waveshare TFT shield slot.

### 4. Compile & Flash to the Board

Connect your Arduino UNO R4 WiFi via USB and run the flash script:

**Windows (PowerShell):**
```powershell
.\scripts\flash.ps1
```

**macOS / Linux:**
```bash
./scripts/flash.sh
```

Once flashed, watch your JUnomochi code at his desk—and whenever he wanders off to slack by the plants, tap the screen to nudge him back to work!

---

## Credits & License

- Office & character pixel art adapted from [Pixel Agents](https://github.com/pixel-agents-hq/pixel-agents).
- Powered by [JUno](https://github.com/jabrena/juno).
- Licensed under the MIT License. See [LICENSE](LICENSE) for details.
