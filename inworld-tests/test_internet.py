#!/usr/bin/env python3
"""
In-world test: the Internet, the IV facility (design document, section 6.3; M5).

Two Mainframes, each with one terminal and one librarian, and an Internet tower whose two Router Ports are wired by OC
cable to one Mainframe's terminal network each.  All three are on one ME network with the Desk test's stone pattern.

  Linking:   both Mainframes link: the Internet has two job slots, and the Mainframes none of their own.
  Pooling:   the network plans at the sum of the two Mainframes' rates, with no committee penalty between them.
  Two jobs:  two crafts run at once, one on each of the Internet's slots.
  Sign-off:  with no executive, a job may use only one Mainframe's Filing.
"""

import os
import re
import sys
import time

from nhdev import NH_DEV_ROOT as REPO_ROOT  # noqa: F401 (puts the workspace on sys.path)

from tools.agentharness import HarnessClient
from tools.inworld_test_driver import InWorldTestDriver

import test_mainframe
from abtest import Chat, entities

TAG = "ab_internet"
Y = 6
NORTH = 2
SOUTH = 3
CASING = "appliedbureaucratics:appliedbureaucratics.casing"
MAINFRAMES = (60, 80)
INTERNET = (70, Y, 440)
PORTS = ((69, Y, 440), (71, Y, 440))
AE_CONTROLLER = (60, Y, 419)
STANDING_ON = (70.5, Y, 432.5)


def build_mainframe(client: HarnessClient, cx: int):
    """A Mainframe facing north with its controller at (cx, Y, 420), and one workstation south of it:
    a screen, a keyboard and a seat."""
    for x in range(cx - 3, cx + 4):
        for z in (420, 421, 422):
            for y in range(Y - 1, Y + 3):
                client.set_block(x, y, z, CASING, 0)
    for x in range(cx - 2, cx + 3):
        client.set_block(x, Y + 1, 420, CASING, 1)
    for x in (cx - 2, cx - 1, cx + 1, cx + 2):
        client.set_block(x, Y, 420, "OpenComputers:rack")
        client.set_block(x, Y, 421, "minecraft:air")
    client.set_block(cx, Y, 422, "appliedbureaucratics:terminal_concentrator")
    client.set_gt_machine(cx, Y, 420, 32200, NORTH)
    hatches = {(cx - 2, Y + 1, 422): 11304, (cx - 1, Y + 1, 422): 90, (cx + 1, Y + 1, 422): 74}
    for pos, mte in hatches.items():
        client.set_gt_machine(*pos, mte, SOUTH)
    client.teleport(cx + 0.5, Y, 432.5)
    time.sleep(1)
    for x in (cx - 2, cx - 1, cx + 1, cx + 2):
        client.chat("/ab debug ocitem %d %d %d 0 server1" % (x, Y, 420))
    client.set_container_slot(cx + 1, Y + 1, 422, 0, "appliedbureaucratics:component", 4, damage=0)
    client.set_container_slot(cx + 1, Y + 1, 422, 1, "minecraft:paper", 64)
    client.set_container_slot(cx + 1, Y + 1, 422, 2, "minecraft:emerald", 16)
    client.set_block(cx, Y, 423, "OpenComputers:cable")
    client.set_block(cx, Y, 424, "OpenComputers:cable")
    client.set_block(cx, Y, 425, "OpenComputers:screen1")
    client.set_block(cx, Y, 427, "BiblioCraft:BiblioSeats")
    test_mainframe.add_keyboards(client, xs=(cx,))


def build_internet(client: HarnessClient):
    x0, y0, z0 = INTERNET
    for dz in range(5):
        for dy in range(9):
            for dx in range(-2, 3):
                corner = dx in (-2, 2) and dz in (0, 4)
                wall = dx in (-2, 2) or dz in (0, 4) or dy in (0, 8)
                pos = (x0 + dx, y0 + dy, z0 + dz)
                if corner:
                    client.set_block(*pos, "gregtech:gt.blockframes", 305)
                elif dx == 0 and dz == 2 and dy == 8:
                    client.set_block(*pos, CASING, 7)
                elif dx == 0 and dz == 2 and 1 <= dy <= 7:
                    client.set_block(*pos, CASING, 3)
                elif wall:
                    client.set_block(*pos, CASING, 2)
                else:
                    client.set_block(*pos, "minecraft:air")
    for port in PORTS:
        client.set_block(*port, "appliedbureaucratics:router_port")
    client.set_gt_machine(*INTERNET, 32201, NORTH)
    client.set_gt_machine(x0 - 1, y0 + 1, z0, 11304, NORTH)
    client.set_gt_machine(x0, y0 + 1, z0, 90, NORTH)
    client.set_gt_machine(x0 + 1, y0 + 1, z0, 74, NORTH)


def build(client: HarnessClient):
    client.fill_area(50, Y - 2, 410, 90, Y - 2, 450, "minecraft:stone")
    client.fill_area(50, Y - 1, 410, 90, Y - 1, 450, "minecraft:stone")
    for cx in MAINFRAMES:
        build_mainframe(client, cx)
    build_internet(client)
    # Each port's own OC network: from the port north to its Mainframe's terminal cable, never touching the other.
    for (px, py, pz), cx in zip(PORTS, MAINFRAMES):
        for z in range(425, pz):
            client.set_block(px, Y, z, "OpenComputers:cable")
        lo, hi = sorted((px, cx))
        for x in range(lo, hi + 1):
            client.set_block(x, Y, 424, "OpenComputers:cable")
    # One ME network: a creative controller, a drive, the stone pattern, and ME cable to each machine.
    client.set_block(*AE_CONTROLLER, "appliedenergistics2:tile.BlockCreativeEnergyController")
    client.set_block(60, Y + 1, 419, "appliedenergistics2:tile.BlockDrive")
    client.set_container_slot(60, Y + 1, 419, 0, "appliedenergistics2:item.ItemBasicStorageCell.64k", 1)
    client.set_block(60, Y, 418, "appliedenergistics2:tile.BlockInterface")
    client.set_block(60, Y, 417, "minecraft:chest")
    # AE2 places parts only within the player's reach.
    for x in range(61, 81):
        client.teleport(x + 0.5, Y, 417.5)
        client.place_multipart(x, Y, 419, "ae2:56")
    for z in range(420, 440):
        client.teleport(72.5, Y, z + 0.5)
        client.place_multipart(70, Y, z, "ae2:56")
    client.teleport(*STANDING_ON)
    time.sleep(1)
    for slot in range(4):
        client.set_container_slot(60, Y, 418, slot, "minecraft:cobblestone", 64)
    client.chat("/ab debug pattern 60 %d 418 0 minecraft:cobblestone 1 minecraft:stone 1" % Y)
    client.chat("/ab debug pattern 60 %d 418 1 minecraft:cobblestone 1 minecraft:gravel 1" % Y)
    time.sleep(2)


def status(client: HarnessClient, chat: Chat, pos: tuple) -> dict:
    chat.skip()
    client.chat("/ab status %d %d %d" % pos)
    head = chat.wait_for(r" at \d+ \d+ \d+:")
    lines = [head]
    for _ in range(3):
        line = chat.wait_for(r"^  ", 2)
        if line:
            lines.append(line)
    return {"lines": lines, "text": " | ".join(l for l in lines if l)}


def number(pattern: str, text: str) -> float:
    m = re.search(pattern, text)
    return float(m.group(1)) if m else float("nan")


def wait_for_text(client, chat, pos, pattern, seconds=60):
    deadline = time.time() + seconds
    s = status(client, chat, pos)
    while not re.search(pattern, s["text"]) and time.time() < deadline:
        time.sleep(2)
        s = status(client, chat, pos)
    return s


def staff(client: HarnessClient, chat: Chat):
    for cx in MAINFRAMES:
        client.fix_maintenance(cx, Y, 420)
        client.command("/summon Villager %d %d %d {Profession:1}" % (cx, Y, 431))
    client.fix_maintenance(*INTERNET)
    time.sleep(2)
    for cx in MAINFRAMES:
        villager = min((e for e in entities(client, "EntityVillager", radius=40) if "," not in e["name"]),
                       key=lambda e: (e["x"] - (cx + 0.5)) ** 2 + (e["z"] - 431.5) ** 2)
        test_mainframe.hire(client, chat, villager, "appliedbureaucratics:employment_contract")
        test_mainframe.seat(client, chat, (cx, Y, 427))
    client.teleport(*STANDING_ON)


def check_linking(client: HarnessClient, chat: Chat):
    print("[*] Linking")
    s = wait_for_text(client, chat, INTERNET, r"2 mainframes.*sky yes")
    print("    " + s["text"])
    assert re.search(r"2 mainframes", s["text"]), s
    assert re.search(r"slots 2,", s["text"]), s
    print("[✓] Two Mainframes linked: two job slots")


def check_pooling(client: HarnessClient, chat: Chat):
    print("[*] Pooling")
    # The network's total catches up with the Internet's a moment later: wait for both.
    s = wait_for_text(client, chat, INTERNET,
                      r"planning [1-9][\d.]* steps/s.*network: \d+ facilities, planning [1-9]\d\.\d+")
    planning = number(r"planning ([\d.]+) steps/s", s["text"])
    network = number(r"network: \d+ facilities, planning ([\d.]+) steps/s", s["text"])
    print("    " + s["text"])
    # Each librarian alone: 1 FTE x 4 x 1.5 = 6 steps/s; the Internet adds them without a committee penalty.
    assert abs(planning - 12) < 0.5, f"Expected about 12 steps/s from two Mainframes, got {planning}"
    assert abs(network - planning) < 0.5, f"Expected the network to plan at the Internet's rate, got {network}"
    print(f"[✓] The Internet plans at {planning} steps/s, the sum of its Mainframes")


def check_two_jobs(client: HarnessClient, chat: Chat):
    print("[*] Two jobs")
    client.chat("/ab craft minecraft:stone 100")
    assert chat.wait_for(r"planned and started", 120), "The first job did not start"
    # A different item: AE2 merges a player's job into a busy CPU making the same one.
    client.chat("/ab craft minecraft:gravel 100")
    assert chat.wait_for(r"planned and started", 120), "The second job did not start"
    s = wait_for_text(client, chat, INTERNET, r"slots busy 2", 20)
    print("    " + s["text"])
    assert "slots busy 2" in s["text"], s
    cap = number(r"job cap (\d+) B", s["text"])
    assert cap == 65536, f"Expected one Mainframe's Filing as the job cap without an executive, got {cap}"
    print("[✓] Two jobs run at once; without an executive a job may use one Mainframe's Filing")


def main(port: int) -> bool:
    driver = InWorldTestDriver(tag=TAG, port=port)

    def test(client: HarnessClient):
        client.teleport(*STANDING_ON)
        chat = Chat(client)
        staff(client, chat)
        check_linking(client, chat)
        check_pooling(client, chat)
        check_two_jobs(client, chat)

    return driver.run(build_fn=build, test_fn=test)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="The Internet: linking, pooling, two jobs")
    parser.add_argument("--port", type=int, default=int(os.environ.get("MCFLEET_HARNESS_PORT", 25571)))
    args = parser.parse_args()
    sys.exit(0 if main(args.port) else 1)
