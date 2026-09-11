#!/usr/bin/env python3
# Copyright (c) 2026 TikTok Pte. Ltd.
# Licensed under the Apache License Version 2.0 that can be found in the
# LICENSE file in the root directory of this source tree.
"""Render the template's placeholder app icon and splash marks.

These are placeholders - an app replaces them - but they have to be valid PNGs,
which the committed app_icon.png was not: it began with EF BF BD 50 4E 47, the
UTF-8 replacement character followed by "PNG", so the file had been read as text
and re-encoded at some point and every non-ASCII byte was destroyed.

Rendering them from a script rather than committing opaque binaries keeps them
reproducible and keeps the repository free of an asset nobody can regenerate.
Pure standard library: zlib and struct are all a PNG needs.

    python3 resource/make-assets.py
"""

from __future__ import annotations

import os
import struct
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))

# Ink on a near-black tile, with one warm accent so the mark reads at 48 px.
TILE = (0x1B, 0x1D, 0x21)
TILE_EDGE = (0x26, 0x2A, 0x30)
MARK = (0xFF, 0xFF, 0xFF)
MARK_DIM = (0xDC, 0xE0, 0xE6)
ACCENT = (0x4F, 0x9D, 0xFF)

SUPERSAMPLE = 4


def write_png(path: str, size: int, pixels: list[tuple[int, int, int, int]]) -> None:
    """Write 8-bit RGBA, non-interlaced."""
    raw = bytearray()
    for y in range(size):
        raw.append(0)  # filter: none
        row = pixels[y * size:(y + 1) * size]
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))

    def chunk(tag: bytes, payload: bytes) -> bytes:
        return (
            struct.pack(">I", len(payload))
            + tag
            + payload
            + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF)
        )

    header = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    with open(path, "wb") as handle:
        handle.write(b"\x89PNG\r\n\x1a\n")
        handle.write(chunk(b"IHDR", header))
        handle.write(chunk(b"IDAT", zlib.compress(bytes(raw), 9))) 
        handle.write(chunk(b"IEND", b""))


def rounded_tile(x: float, y: float, size: float, radius: float) -> bool:
    """Inside a rounded square centred on the canvas."""
    half = size / 2.0
    dx = abs(x) - (half - radius)
    dy = abs(y) - (half - radius)
    if dx <= 0 or dy <= 0:
        return abs(x) <= half and abs(y) <= half
    return dx * dx + dy * dy <= radius * radius


def spark(x: float, y: float, reach: float) -> bool:
    """A four-point spark: the unit astroid |x|^(2/3) + |y|^(2/3) <= 1."""
    if reach <= 0:
        return False
    ax = abs(x) / reach
    ay = abs(y) / reach
    if ax > 1.0 or ay > 1.0:
        return False
    return (ax ** (2.0 / 3.0) + ay ** (2.0 / 3.0)) <= 1.0


def render(size: int, tile: bool, ink: tuple[int, int, int] = MARK) -> list[tuple[int, int, int, int]]:
    """One image, supersampled so the curves do not stair-step."""
    out: list[tuple[int, int, int, int]] = []
    steps = SUPERSAMPLE
    step = 1.0 / steps
    centre = size / 2.0
    tile_size = size * 0.88
    tile_radius = size * 0.22
    big = size * 0.30
    small = size * 0.13
    offset = size * 0.20

    for py in range(size):
        for px in range(size):
            acc = [0.0, 0.0, 0.0, 0.0]
            for sy in range(steps):
                for sx in range(steps):
                    x = px + (sx + 0.5) * step - centre
                    y = py + (sy + 0.5) * step - centre
                    r, g, b, a = (0, 0, 0, 0)
                    if tile and rounded_tile(x, y, tile_size, tile_radius):
                        edge = max(abs(x), abs(y)) / (tile_size / 2.0)
                        mix = min(1.0, max(0.0, edge))
                        r = round(TILE[0] + (TILE_EDGE[0] - TILE[0]) * mix)
                        g = round(TILE[1] + (TILE_EDGE[1] - TILE[1]) * mix)
                        b = round(TILE[2] + (TILE_EDGE[2] - TILE[2]) * mix)
                        a = 255
                    # The small spark sits up and to the right of the big one.
                    if spark(x + offset * 0.35, y + offset * 0.35, big):
                        r, g, b, a = (*ink, 255)
                    elif spark(x - offset, y - offset * 0.9, small):
                        r, g, b, a = (*ACCENT, 255)
                    acc[0] += r
                    acc[1] += g
                    acc[2] += b
                    acc[3] += a
            n = float(steps * steps)
            out.append((
                round(acc[0] / n),
                round(acc[1] / n),
                round(acc[2] / n),
                round(acc[3] / n),
            ))
    return out


def main() -> int:
    # The splash marks are drawn without the tile: the splash already has a
    # ground of its own. The dark-mode one is a shade softer so it does not
    # glare against pure black.
    jobs = [
        ("app_icon.png", 1024, True, MARK),
        ("splash_icon.png", 512, False, MARK),
        ("splash_icon_dark.png", 512, False, MARK_DIM),
    ]
    for name, size, tile, ink in jobs:
        path = os.path.join(HERE, name)
        write_png(path, size, render(size, tile, ink))
        print(f"{name}: {size}x{size}, {os.path.getsize(path)} bytes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
