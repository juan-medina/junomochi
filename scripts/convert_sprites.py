#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Juan Medina
# SPDX-License-Identifier: MIT

import os
import zlib
import struct

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GRAPHICS_DIR = os.path.join(PROJECT_ROOT, "graphics")
JAVA_OUT_PATH = os.path.join(
    PROJECT_ROOT, "src", "main", "java", "com", "juanmedina", "junomochi", "SpriteAssets.java"
)

# Master palette (index 0 is transparent, index 1 is room floor color)
FLOOR_COLOR_565 = 0x3186
PALETTE = [0x0000, FLOOR_COLOR_565]
COLOR_TO_INDEX = {FLOOR_COLOR_565: 1}

def get_palette_index(r, g, b, a):
    if a <= 50:
        return 0
    # Quantize to RGB332-aligned RGB565 (max 255 colors) so all sprites fit in a 256-entry 8-bit palette
    r5 = ((r >> 5) * 31 + 3) // 7
    g6 = ((g >> 5) * 63 + 3) // 7
    b5 = ((b >> 6) * 31 + 1) // 3
    c565 = (r5 << 11) | (g6 << 5) | b5
    if c565 not in COLOR_TO_INDEX:
        idx = len(PALETTE)
        PALETTE.append(c565)
        COLOR_TO_INDEX[c565] = idx
    return COLOR_TO_INDEX[c565]

def read_png(filepath):
    with open(filepath, 'rb') as f:
        sig = f.read(8)
        assert sig == b'\x89PNG\r\n\x1a\n', f"Invalid PNG signature in {filepath}"
        idat = []
        width = height = bit_depth = color_type = 0
        while True:
            chunk_len_bytes = f.read(4)
            if not chunk_len_bytes:
                break
            chunk_len = struct.unpack('>I', chunk_len_bytes)[0]
            chunk_type = f.read(4)
            chunk_data = f.read(chunk_len)
            f.read(4)
            if chunk_type == b'IHDR':
                width, height, bit_depth, color_type, comp, filt, interlace = struct.unpack('>IIBBBBB', chunk_data)
            elif chunk_type == b'IDAT':
                idat.append(chunk_data)
            elif chunk_type == b'IEND':
                break

    decompressed = zlib.decompress(b''.join(idat))
    bytes_per_pixel = 4 if color_type == 6 else (3 if color_type == 2 else 1)
    stride = width * bytes_per_pixel
    pixels = []
    offset = 0
    prev_row = bytearray(stride)

    for y in range(height):
        filter_type = decompressed[offset]
        offset += 1
        raw_row = bytearray(decompressed[offset:offset + stride])
        offset += stride
        row = bytearray(stride)

        for i in range(stride):
            filt = filter_type
            x = raw_row[i]
            a = row[i - bytes_per_pixel] if i >= bytes_per_pixel else 0
            b = prev_row[i]
            c = prev_row[i - bytes_per_pixel] if i >= bytes_per_pixel else 0
            if filt == 0: val = x
            elif filt == 1: val = (x + a) & 0xff
            elif filt == 2: val = (x + b) & 0xff
            elif filt == 3: val = (x + ((a + b) >> 1)) & 0xff
            elif filt == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                val = (x + pr) & 0xff
            else:
                raise ValueError(f"Unknown filter: {filt}")
            row[i] = val

        prev_row = row
        row_pixels = []
        for x in range(width):
            px_idx = x * bytes_per_pixel
            if color_type == 6:
                r, g, b, alpha = row[px_idx:px_idx+4]
            elif color_type == 2:
                r, g, b = row[px_idx:px_idx+3]
                alpha = 255
            else:
                raise ValueError(f"Unsupported color type: {color_type}")
            row_pixels.append((r, g, b, alpha))
        pixels.append(row_pixels)

    return width, height, pixels

def encode_segments(png_filename):
    png_path = os.path.join(GRAPHICS_DIR, png_filename)
    w, h, pixels = read_png(png_path)
    segments = []

    for row in range(h):
        col = 0
        while col < w:
            r, g, b, a = pixels[row][col]
            idx = get_palette_index(r, g, b, a)
            if idx == 0:
                col += 1
                continue

            start_col = col
            span_pixels = [idx]
            col += 1
            while col < w:
                r, g, b, a = pixels[row][col]
                p_idx = get_palette_index(r, g, b, a)
                if p_idx == 0:
                    break
                span_pixels.append(p_idx)
                col += 1

            i = 0
            n = len(span_pixels)
            while i < n:
                run_color = span_pixels[i]
                run_len = 1
                while i + run_len < n and span_pixels[i + run_len] == run_color:
                    run_len += 1

                if run_len >= 5 or (i == 0 and run_len == n and run_len >= 2):
                    segments.append({
                        'row': row,
                        'col': start_col + i,
                        'is_repeat': True,
                        'len': run_len,
                        'color': run_color
                    })
                    i += run_len
                else:
                    lit_start = i
                    lit_pixels = []
                    while i < n:
                        rc = span_pixels[i]
                        rl = 1
                        while i + rl < n and span_pixels[i + rl] == rc:
                            rl += 1
                        if rl >= 5:
                            break
                        for _ in range(rl):
                            lit_pixels.append(span_pixels[i])
                            i += 1
                    if len(lit_pixels) >= 2 and all(p == lit_pixels[0] for p in lit_pixels):
                        segments.append({
                            'row': row,
                            'col': start_col + lit_start,
                            'is_repeat': True,
                            'len': len(lit_pixels),
                            'color': lit_pixels[0]
                        })
                    else:
                        segments.append({
                            'row': row,
                            'col': start_col + lit_start,
                            'is_repeat': False,
                            'len': len(lit_pixels),
                            'pixels': lit_pixels
                        })

    data = bytearray(struct.pack('>H', len(segments)))
    for s in segments:
        data.append(s['row'])
        data.append(s['col'])
        if s['is_repeat']:
            data.append(0x80 | (s['len'] & 0x7F))
            data.append(s['color'])
        else:
            data.append(s['len'] & 0x7F)
            for p in s['pixels']:
                data.append(p)

    return w, h, segments, data

def to_signed_byte(b):
    return b - 256 if b >= 128 else b

def format_byte_array(name, data):
    signed_vals = [str(to_signed_byte(b)) for b in data]
    lines = [f"    public static final byte[] {name} = {{"]
    for i in range(0, len(signed_vals), 16):
        chunk = ", ".join(signed_vals[i:i + 16])
        comma = "," if i + 16 < len(signed_vals) else ""
        lines.append(f"        {chunk}{comma}")
    lines.append("    };")
    return "\n".join(lines)

def format_int_array(name, ints):
    hex_vals = [f"0x{c:04X}" for c in ints]
    lines = [f"    public static final int[] {name} = {{"]
    for i in range(0, len(hex_vals), 8):
        chunk = ", ".join(hex_vals[i:i + 8])
        comma = "," if i + 8 < len(hex_vals) else ""
        lines.append(f"        {chunk}{comma}")
    lines.append("    };")
    return "\n".join(lines)

def main():
    targets = [
        ("WALK_LEFT_STAND", "walk_left_stand.png"),
        ("WALK_LEFT_STEP_LEFT", "walk_left_step_left.png"),
        ("WALK_LEFT_STEP_RIGHT", "walk_left_step_right.png"),
        ("WALK_RIGHT_STAND", "walk_right_stand.png"),
        ("WALK_RIGHT_STEP_LEFT", "walk_right_step_left.png"),
        ("WALK_RIGHT_STEP_RIGHT", "walk_right_step_right.png"),
        ("WALK_DOWN_STAND", "walk_down_stand.png"),
        ("TYPING_RIGHT_FRAME0", "typing_right_frame0.png"),
        ("TYPING_RIGHT_FRAME1", "typing_right_frame1.png"),
        ("DESK_SIDE", "desk_side.png"),
        ("PC_SIDE", "pc_side.png"),
        ("CHAIR_SIDE", "chair_side.png"),
        ("SMALL_PAINTING", "small_painting.png"),
        ("SMALL_PAINTING_2", "small_painting_2.png"),
        ("LARGE_PAINTING", "large_painting.png"),
        ("CLOCK", "clock.png"),
        ("HANGING_PLANT", "hanging_plant.png"),
        ("PLANT", "plant.png"),
        ("PLANT_2", "plant_2.png"),
        ("BIN", "bin.png"),
    ]

    print(f"=== Packing {len(targets)} Sprites into Single Flash Table in {JAVA_OUT_PATH} ===")
    all_data = bytearray()
    offsets = []

    for const_name, png_name in targets:
        offset = len(all_data)
        w, h, segments, data = encode_segments(png_name)
        all_data.extend(data)
        offsets.append((const_name, offset, w, h, len(data), len(segments)))
        print(f"  {const_name:24s} ({w}x{h}) @ offset {offset:4d}: {len(data):3d} bytes ({len(segments)} spans)")

    pal_bytes = len(PALETTE) * 4
    total_flash = len(all_data) + pal_bytes
    print(f"  {'PALETTE_COLORS':24s} ({len(PALETTE)} colors): {pal_bytes} bytes")
    print(f"Total embedded Flash (.rodata): {total_flash} bytes (0 bytes Arena!)\n")

    java_lines = [
        "// SPDX-FileCopyrightText: 2026 Juan Medina",
        "// SPDX-License-Identifier: MIT",
        "",
        "package com.juanmedina.junomochi;",
        "",
        "/**",
        " * Auto-generated embedded sprite and palette assets stored directly in Flash (.rodata).",
        " * Generated by scripts/convert_sprites.py.",
        " */",
        "public final class SpriteAssets {",
        "",
    ]

    for const_name, offset, w, h, length, _ in offsets:
        java_lines.append(f"    public static final int {const_name} = {offset};")

    java_lines.extend([
        "",
        format_int_array("PALETTE_COLORS", PALETTE),
        "",
        format_byte_array("DATA", all_data),
        "",
        "    private SpriteAssets() {",
        "    }",
        "}",
        ""
    ])

    with open(JAVA_OUT_PATH, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(java_lines))

    print("Successfully generated SpriteAssets.java!")

if __name__ == '__main__':
    main()
