#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Juan Medina
# SPDX-License-Identifier: MIT

import os
import sys
import zlib
import struct

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GRAPHICS_DIR = os.path.join(PROJECT_ROOT, "graphics")
SDCARD_DIR = os.path.join(PROJECT_ROOT, "sdcard")

# Standard 16-bit RGB565 conversion
def to_rgb565(r, g, b):
    r5 = (r >> 3) & 0x1F
    g6 = (g >> 2) & 0x3F
    b5 = (b >> 3) & 0x1F
    return (r5 << 11) | (g6 << 5) | b5

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
            f.read(4) # CRC
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

            # Split span_pixels into literal and repeat sub-segments (run >= 5 saves bytes)
            i = 0
            n = len(span_pixels)
            while i < n:
                # Check length of identical run starting at i
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

    # Binary packing:
    # Header: uint16 total_segments (big-endian)
    # Per segment:
    #   uint8 row
    #   uint8 col
    #   uint8 flag_and_length: (bit 7 = 1 for repeat/memset, bits 0..6 = length)
    #   payload: 1 byte color if repeat, else length bytes of palette indices
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

def main():
    # Always clean the sdcard folder before conversion
    if os.path.exists(SDCARD_DIR):
        for item in os.listdir(SDCARD_DIR):
            p = os.path.join(SDCARD_DIR, item)
            if os.path.isfile(p):
                os.remove(p)
    else:
        os.makedirs(SDCARD_DIR, exist_ok=True)

    targets = [
        "walk_left_stand.png",
        "walk_left_step_left.png",
        "walk_left_step_right.png",
        "walk_right_stand.png",
        "walk_right_step_left.png",
        "walk_right_step_right.png",
        "walk_up_stand.png",
        "walk_up_step_left.png",
        "walk_up_step_right.png",
        "walk_down_stand.png",
        "walk_down_step_left.png",
        "walk_down_step_right.png",
        "typing_right_frame0.png",
        "typing_right_frame1.png",
        "typing_left_frame0.png",
        "typing_left_frame1.png",
        "desk_side.png",
        "pc_side.png",
        "chair_side.png",
        "small_painting.png",
        "small_painting_2.png",
        "large_painting.png",
        "clock.png",
        "hanging_plant.png",
        "plant.png",
        "plant_2.png",
        "large_plant.png",
        "bin.png",
    ]

    print(f"=== Converting {len(targets)} Sprites to Segment Binary Format ===")
    print(f"Graphics source: {GRAPHICS_DIR}")
    print(f"SD card target:  {SDCARD_DIR}\n")

    for png_name in targets:
        bin_name = os.path.splitext(png_name)[0] + ".bin"
        w, h, segments, data = encode_segments(png_name)
        out_path = os.path.join(SDCARD_DIR, bin_name)
        with open(out_path, 'wb') as f:
            f.write(data)

        raw_size = w * h
        print(f"[{bin_name}]")
        print(f"  Dimensions: {w}x{h} ({raw_size} bytes uncompressed)")
        print(f"  Segments:   {len(segments)}")
        print(f"  File size:  {len(data)} bytes ({len(data)*100/raw_size:.1f}% of raw)\n")

    # Export palette.bin
    pal_bytes = bytearray()
    for c in PALETTE:
        pal_bytes.extend(struct.pack('>H', c))
    pal_path = os.path.join(SDCARD_DIR, "palette.bin")
    with open(pal_path, 'wb') as f:
        f.write(pal_bytes)
    print(f"[palette.bin] Exported {len(PALETTE)} colors ({len(pal_bytes)} bytes)\n")

    print("Conversion complete!")

if __name__ == '__main__':
    main()

