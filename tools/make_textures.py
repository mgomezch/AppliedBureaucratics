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


WOOD = (150, 110, 70, 255)
WOOD_DARK = (105, 75, 45, 255)
STONE = (150, 150, 150, 255)


def counter_side():
    c = canvas(WOOD)
    frame(c, WOOD_DARK)
    rect(c, 0, 0, 15, 2, STONE)
    rect(c, 3, 6, 6, 12, WOOD_DARK)
    rect(c, 9, 6, 12, 12, WOOD_DARK)
    return c


def counter_top():
    c = canvas(STONE)
    frame(c, (120, 120, 120, 255))
    rect(c, 3, 3, 6, 7, (240, 240, 240, 255))
    rect(c, 4, 4, 5, 5, (90, 55, 30, 255))
    rect(c, 9, 8, 13, 12, (230, 190, 120, 255))
    rect(c, 10, 9, 12, 11, (200, 60, 50, 255))
    return c


def casing(base, dark, light, motif=None):
    c = canvas(base)
    frame(c, dark)
    rect(c, 1, 1, 14, 1, light)
    rect(c, 1, 1, 1, 14, light)
    if motif:
        motif(c)
    return c


def rivets(color):
    def draw(c):
        for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
            c[y][x] = list(color)
    return draw


def tape_window(c):
    rect(c, 2, 4, 13, 11, (30, 34, 40, 255))
    for cx in (5, 10):
        rect(c, cx - 2, 6, cx + 2, 9, (90, 70, 40, 255))
        c[7][cx] = list((200, 200, 200, 255))
        c[8][cx] = list((200, 200, 200, 255))


def tubes(c):
    for x in (3, 7, 11):
        rect(c, x, 1, x + 1, 14, (170, 210, 230, 255))
        c[5][x] = list((255, 230, 120, 255))
        c[10][x + 1] = list((255, 230, 120, 255))


def backbone(c):
    rect(c, 6, 0, 9, 15, (60, 200, 120, 255))
    rect(c, 7, 0, 8, 15, (180, 255, 210, 255))


def data_hall(c):
    for y in (4, 8, 12):
        rect(c, 2, y, 13, y, (50, 55, 60, 255))
    for x in (4, 8, 11):
        c[6][x] = list((80, 255, 80, 255))


def raised_floor(c):
    rect(c, 7, 0, 8, 15, (70, 75, 80, 255))
    rect(c, 0, 7, 15, 8, (70, 75, 80, 255))


def pylon(c):
    rect(c, 5, 0, 10, 15, (40, 30, 60, 255))
    for y in range(1, 15, 3):
        rect(c, 6, y, 9, y, (150, 90, 255, 255))


def dish(c):
    rect(c, 3, 3, 12, 12, (210, 210, 215, 255))
    rect(c, 6, 6, 9, 9, (120, 120, 130, 255))


def screen_overlay(active):
    c = canvas()
    rect(c, 2, 2, 13, 13, (20, 25, 20, 255))
    rect(c, 3, 3, 12, 10, (10, 40, 10, 255) if not active else (20, 120, 30, 255))
    if active:
        for y in (4, 6, 8):
            rect(c, 4, y, 4 + (y * 3) % 8, y, (120, 255, 120, 255))
    rect(c, 4, 12, 11, 12, (90, 90, 90, 255))
    return c


def concentrator():
    c = casing(STEEL, STEEL_DARK, STEEL_LIGHT)
    rect(c, 4, 4, 11, 11, (40, 40, 50, 255))
    for x, y in ((5, 5), (10, 5), (5, 10), (10, 10)):
        c[y][x] = list((90, 200, 255, 255))
    rect(c, 7, 7, 8, 8, (255, 200, 60, 255))
    return c


def reel(color, hub):
    c = canvas()
    rect(c, 3, 3, 12, 12, color)
    rect(c, 2, 5, 13, 10, color)
    rect(c, 5, 2, 10, 13, color)
    rect(c, 6, 6, 9, 9, hub)
    return c


def small_item(fill, mark=None):
    c = canvas()
    rect(c, 3, 3, 12, 12, fill)
    if mark:
        mark(c)
    return c


def main():
    for name, art in {
        "mainframe": casing(STEEL, STEEL_DARK, STEEL_LIGHT, rivets(STEEL_DARK)),
        "tape_window": casing(STEEL, STEEL_DARK, STEEL_LIGHT, tape_window),
        "tube": casing((90, 100, 110, 255), (60, 65, 70, 255), (130, 140, 150, 255), tubes),
        "backbone": casing((50, 55, 60, 255), (30, 32, 36, 255), (80, 85, 90, 255), backbone),
        "data_hall": casing((200, 205, 210, 255), (150, 155, 160, 255), (230, 235, 240, 255), data_hall),
        "raised_floor": casing((180, 180, 185, 255), (120, 120, 125, 255), (210, 210, 215, 255), raised_floor),
        "containment": casing((30, 25, 40, 255), (15, 10, 20, 255), (60, 50, 80, 255), pylon),
        "dish": casing((180, 180, 185, 255), (120, 120, 125, 255), (220, 220, 225, 255), dish),
    }.items():
        png(f"{OUT}/blocks/casing_{name}.png", art)
    png(f"{OUT}/blocks/terminal_concentrator.png", concentrator())
    port = casing((90, 100, 110, 255), (60, 65, 70, 255), (130, 140, 150, 255))
    for x in (4, 7, 10):
        rect(port, x, 6, x + 1, 9, (20, 20, 25, 255))
        port[5][x] = [60, 255, 90, 255]
    png(f"{OUT}/blocks/router_port.png", port)
    kiosk = casing((230, 120, 40, 255), (170, 80, 20, 255), (255, 170, 90, 255))
    rect(kiosk, 3, 3, 12, 8, (30, 30, 40, 255))
    rect(kiosk, 4, 4, 11, 5, (255, 255, 255, 255))
    rect(kiosk, 6, 10, 9, 12, (255, 230, 120, 255))
    png(f"{OUT}/blocks/gig_kiosk.png", kiosk)
    png(f"{OUT}/blocks/OVERLAY_FRONT_MAINFRAME.png", screen_overlay(False))
    png(f"{OUT}/blocks/OVERLAY_FRONT_MAINFRAME_ACTIVE.png", screen_overlay(True))
    png(f"{OUT}/items/tape_reel.png", reel((60, 50, 40, 255), (200, 200, 200, 255)))
    png(f"{OUT}/items/high_density_tape_reel.png", reel((40, 40, 80, 255), (230, 200, 90, 255)))
    png(f"{OUT}/items/disk_array.png", small_item((150, 150, 160, 255), lambda c: [rect(c, 4, y, 11, y, (40, 40, 50, 255)) for y in (5, 8, 11)]))
    png(f"{OUT}/items/punch_card.png", small_item((230, 210, 150, 255), lambda c: [c.__setitem__(y, [p if (x + y) % 3 else [60, 50, 30, 255] for x, p in enumerate(c[y])]) for y in range(4, 12)]))
    png(f"{OUT}/items/blank_data_tape.png", small_item((50, 50, 55, 255), lambda c: rect(c, 5, 5, 10, 8, (200, 200, 200, 255))))
    png(f"{OUT}/items/work_log.png", small_item((50, 50, 55, 255), lambda c: rect(c, 5, 5, 10, 8, (90, 200, 90, 255))))
    png(f"{OUT}/items/accelerator_card.png", small_item((20, 90, 40, 255), lambda c: rect(c, 5, 5, 10, 10, (30, 30, 30, 255))))
    png(f"{OUT}/items/converged_model_weights.png", small_item((120, 60, 200, 255), lambda c: rect(c, 6, 6, 9, 9, (255, 255, 255, 255))))
    pc = canvas()
    for y in range(3, 13):
        pc[y][5] = [190, 190, 200, 255]
        pc[y][10] = [190, 190, 200, 255]
    rect(pc, 5, 3, 10, 3, (190, 190, 200, 255))
    rect(pc, 7, 12, 10, 12, (190, 190, 200, 255))
    png(f"{OUT}/items/paperclip.png", pc)
    png(f"{OUT}/blocks/break_room_counter_side.png", counter_side())
    png(f"{OUT}/blocks/break_room_counter_top.png", counter_top())
    png(f"{OUT}/blocks/requisition_tray_side.png", tray_side())
    png(f"{OUT}/blocks/requisition_tray_top.png", tray_top())
    png(f"{OUT}/blocks/requisition_tray_bottom.png", tray_bottom())
    png(f"{OUT}/items/employment_contract.png", document(seal=(40, 170, 70, 255)))
    png(f"{OUT}/items/signed_contract.png", document(seal=(40, 170, 70, 255), signed=True))
    png(f"{OUT}/items/management_contract.png", document(seal=(200, 160, 40, 255), stripe=(120, 30, 30, 255)))
    png(f"{OUT}/items/pink_slip.png", document(base=(240, 170, 200, 255)))


if __name__ == "__main__":
    main()
