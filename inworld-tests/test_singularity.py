#!/usr/bin/env python3
"""
In-world test: the Singularity, the UV facility (design document, section 6.6; M8).

The frame of MTESingularity.shape (an octagonal ring at the base and the top, four pylons, the empty cube in the
middle), with a debug energy hatch, a maintenance hatch and an input bus in the base ring, on an ME network.

  Formed:     the structure forms and the controller puts the sphere in the middle of the frame.
  Capacity:   at Alignment 50 it dispatches 1024 x 20 x 0.75 = 15360 ops/s, and the network plans unmetered.
  Looks:      a screenshot of the sphere, for a person to look at (output/singularity.png).
  Broken:     taking out a pylon block unforms it, and the sphere goes.
"""

import os
import re
import shutil
import sys
import time
from pathlib import Path

from nhdev import NH_DEV_ROOT as REPO_ROOT  # noqa: F401 (puts the workspace on sys.path)

from tools.agentharness import HarnessClient
from tools.inworld_test_driver import InWorldTestDriver

from abtest import Chat

TAG = "ab_singularity"
CASING = "appliedbureaucratics:appliedbureaucratics.casing"
XC, YC, ZC = 70, 6, 420
NORTH = 2
CONTROLLER = (XC, YC, ZC)
CENTER = (XC, YC + 8, ZC + 8)
PYLON = (XC + 8, YC + 7, ZC + 8)
SCREENSHOT = Path(__file__).resolve().parent / "output" / "singularity.png"


def frame():
    """The casing blocks of the structure, as MTESingularity.shape lays them out."""
    blocks = []
    for zi in range(17):
        for y in range(17):
            for xi in range(17):
                dx, dz = xi - 8, zi - 8
                outer = max(abs(dx), abs(dz)) <= 8 and abs(dx) + abs(dz) <= 12
                inner = max(abs(dx), abs(dz)) <= 7 and abs(dx) + abs(dz) <= 11
                ring = outer and not inner
                pylon = (dx == 0 and abs(dz) == 8) or (dz == 0 and abs(dx) == 8)
                if (y in (0, 16) and ring) or (pylon and 0 < y < 16):
                    if not (y == 0 and dx == 0 and dz == -8):
                        blocks.append((XC + dx, YC + y, ZC + 8 + dz))
    return blocks


def build(client: HarnessClient):
    client.fill_area(55, YC - 1, 405, 85, YC - 1, 440, "minecraft:stone")
    for pos in frame():
        client.set_block(*pos, CASING, 6)
    client.set_gt_machine(*CONTROLLER, 32203, NORTH)
    client.set_gt_machine(XC - 1, YC, ZC, 11304, NORTH)
    client.set_gt_machine(XC + 1, YC, ZC, 90, NORTH)
    client.set_gt_machine(XC + 2, YC, ZC, 74, NORTH)
    client.set_block(XC, YC, ZC - 1, "appliedenergistics2:tile.BlockCreativeEnergyController")
    client.teleport(XC + 0.5, YC, ZC - 4.5)
    time.sleep(1)


def status(client: HarnessClient, chat: Chat) -> str:
    chat.skip()
    client.chat("/ab status %d %d %d" % CONTROLLER)
    lines = [chat.wait_for(r" at \d+ \d+ \d+:")]
    for _ in range(3):
        lines.append(chat.wait_for(r"^  ", 2))
    return " | ".join(l for l in lines if l)


def wait(client, chat, test, seconds=60) -> str:
    deadline = time.time() + seconds
    s = status(client, chat)
    while not test(s) and time.time() < deadline:
        time.sleep(2)
        s = status(client, chat)
    return s


def check_formed(client: HarnessClient, chat: Chat):
    print("[*] Formed")
    client.fix_maintenance(*CONTROLLER)
    s = wait(client, chat, lambda s: "dispatch 15360" in s)
    print("    " + s)
    assert "dispatch 15360.00 ops/s" in s, s
    assert "unmetered" in s, f"Expected the network to plan unmetered, got {s}"
    core = client.blockinfo(*CENTER)
    assert core["block"] == "appliedbureaucratics:singularity_core", f"No sphere at the centre: {core}"
    print("[✓] Formed: 15360 ops/s at Alignment 50, unmetered planning, and the sphere is in place")


def check_looks(client: HarnessClient):
    print("[*] Looks")
    client.teleport(XC + 0.5, YC + 4, ZC - 14.5)
    client.look_at(CENTER[0] + 0.5, CENTER[1] + 0.5, CENTER[2] + 0.5)
    time.sleep(3)
    result = client.screenshot()
    path = result.get("path") if isinstance(result, dict) else str(result).split(": ", 1)[-1].strip()
    print(f"    screenshot: {path}")
    if path and Path(str(path)).exists():
        SCREENSHOT.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(str(path), SCREENSHOT)
        print(f"[✓] Screenshot saved to {SCREENSHOT}")


def check_broken(client: HarnessClient, chat: Chat):
    print("[*] Broken")
    client.set_block(*PYLON, "minecraft:air")
    deadline = time.time() + 60
    while client.blockinfo(*CENTER)["block"] == "appliedbureaucratics:singularity_core" and time.time() < deadline:
        time.sleep(2)
    assert client.blockinfo(*CENTER)["block"] == "minecraft:air", "The sphere stayed after the frame broke"
    print("[✓] A broken frame takes the sphere away")


def main(port: int) -> bool:
    driver = InWorldTestDriver(tag=TAG, port=port)

    def test(client: HarnessClient):
        client.teleport(XC + 0.5, YC, ZC - 4.5)
        chat = Chat(client)
        check_formed(client, chat)
        check_looks(client)
        check_broken(client, chat)

    return driver.run(build_fn=build, test_fn=test)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="The Singularity: structure, capacity, the sphere")
    parser.add_argument("--port", type=int, default=int(os.environ.get("MCFLEET_HARNESS_PORT", 25571)))
    args = parser.parse_args()
    sys.exit(0 if main(args.port) else 1)
