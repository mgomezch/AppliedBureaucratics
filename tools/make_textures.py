#!/usr/bin/env python3
"""Draws the mod's 16x16 textures, with no imaging library.

Run from the repository root: python3 tools/make_textures.py
"""
import os
import struct
import zlib

OUT = "src/main/resources/assets/appliedbureaucratics/textures"


def png(path, pixels):
    h = len(pixels)
    w = len(pixels[0])
    raw = b"".join(b"\x00" + b"".join(bytes(p) for p in row) for row in pixels)

    def chunk(kind, data):
        c = struct.pack(">I", len(data)) + kind + data
        return c + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(data)


def canvas(fill=(0, 0, 0, 0)):
    return [[list(fill) for _ in range(16)] for _ in range(16)]


def rect(c, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            c[y][x] = list(color)


def frame(c, color):
    rect(c, 0, 0, 15, 0, color)
    rect(c, 0, 15, 15, 15, color)
    rect(c, 0, 0, 0, 15, color)
    rect(c, 15, 0, 15, 15, color)


STEEL = (120, 124, 132, 255)
STEEL_DARK = (78, 80, 88, 255)
STEEL_LIGHT = (160, 164, 172, 255)
PAPER = (236, 232, 214, 255)
PAPER_SHADE = (205, 200, 180, 255)
INK = (40, 40, 52, 255)
FLUIX = (122, 70, 200, 255)


def tray_side():
    c = canvas(STEEL)
    frame(c, STEEL_DARK)
    rect(c, 2, 5, 13, 10, STEEL_DARK)
    for x in range(3, 13, 2):
        c[7][x] = list(FLUIX)
        c[8][x + 1] = list(FLUIX)
    rect(c, 1, 1, 14, 1, STEEL_LIGHT)
    return c


def tray_top():
    c = canvas(STEEL)
    frame(c, STEEL_DARK)
    rect(c, 2, 2, 13, 13, STEEL_DARK)
    rect(c, 3, 3, 12, 12, PAPER_SHADE)
    rect(c, 4, 4, 11, 11, PAPER)
    for y in (6, 8, 10):
        rect(c, 5, y, 10, y, INK)
    return c


def tray_bottom():
    c = canvas(STEEL_DARK)
    frame(c, (50, 52, 58, 255))
    rect(c, 6, 6, 9, 9, FLUIX)
    return c


def document(seal=None, stripe=None, signed=False, base=PAPER):
    c = canvas()
    rect(c, 3, 1, 12, 14, PAPER_SHADE)
    rect(c, 3, 1, 11, 13, base)
    for y in (3, 5, 7, 9):
        rect(c, 5, y, 10, y, INK if y != 9 else PAPER_SHADE)
    if stripe:
        rect(c, 3, 1, 11, 2, stripe)
    if signed:
        for i, x in enumerate(range(5, 11)):
            c[11 + (i % 2)][x] = list((30, 60, 160, 255))
    if seal:
        rect(c, 9, 10, 11, 12, seal)
    return c


def main():
    png(f"{OUT}/blocks/requisition_tray_side.png", tray_side())
    png(f"{OUT}/blocks/requisition_tray_top.png", tray_top())
    png(f"{OUT}/blocks/requisition_tray_bottom.png", tray_bottom())
    png(f"{OUT}/items/employment_contract.png", document(seal=(40, 170, 70, 255)))
    png(f"{OUT}/items/signed_contract.png", document(seal=(40, 170, 70, 255), signed=True))
    png(f"{OUT}/items/management_contract.png", document(seal=(200, 160, 40, 255), stripe=(120, 30, 30, 255)))
    png(f"{OUT}/items/pink_slip.png", document(base=(240, 170, 200, 255)))


if __name__ == "__main__":
    main()
