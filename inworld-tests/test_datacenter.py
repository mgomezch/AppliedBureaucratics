#!/usr/bin/env python3
"""
In-world test: the AI Data Center, the LuV facility (design document, section 6.4; M7).

The shortest hall: a front wall with the controller, five slices of three rack rows, and a back wall, 15 x 7 x 7.  Its
135 Accelerator Racks each hold one Accelerator Card: 135 x 0.128 = 17.28 synthetic FTE.  The front wall has a debug
energy hatch, a maintenance hatch, an input hatch for water and an input bus; a muffler sits in the roof.

  Coolant:   without water the hall throttles to the floor: 17.28 x 4 x 0.25 steps/s while idle.
  Cooled:    with water and no training, output is 25 %: the same 17.28 steps/s.
  Training:  512 Work Logs take the model to T = 0.5, output 62.5 %: 43.2 steps/s.
"""

import os
import re
import sys
import time

from nhdev import NH_DEV_ROOT as REPO_ROOT  # noqa: F401 (puts the workspace on sys.path)

from tools.agentharness import HarnessClient
from tools.inworld_test_driver import InWorldTestDriver

from abtest import Chat

TAG = "ab_datacenter"
CASING = "appliedbureaucratics:appliedbureaucratics.casing"
XC, YC, ZC = 70, 8, 420
NORTH = 2
CONTROLLER = (XC, YC, ZC)
INPUT_HATCH = (XC - 2, YC, ZC)
INPUT_BUS = (XC + 2, YC, ZC)
ROW = "WAA-AA-A-AA-AAW"
STANDING_ON = (XC + 0.5, YC - 3, ZC - 3.5)


def build(client: HarnessClient):
    client.fill_area(55, YC - 5, 410, 85, YC - 5, 435, "minecraft:stone")
    client.fill_area(55, YC - 4, 410, 85, YC - 4, 435, "minecraft:stone")
    x0 = XC - 7
    for z in range(ZC, ZC + 7):
        for y in range(YC - 3, YC + 4):
            for i in range(15):
                x = x0 + i
                end = z in (ZC, ZC + 6)
                row = YC + 3 - y  # 0 at the top
                if end:
                    client.set_block(x, y, z, CASING, 4)
                elif row == 5:
                    client.set_block(x, y, z, CASING, 5)
                elif row in (0, 6) or i in (0, 14):
                    client.set_block(x, y, z, CASING, 4)
                elif row == 1:
                    client.set_block(x, y, z, "minecraft:air")
                elif ROW[i] == "A":
                    client.set_block(x, y, z, "appliedbureaucratics:accelerator_rack")
                else:
                    client.set_block(x, y, z, "minecraft:air")
    client.set_gt_machine(*CONTROLLER, 32202, NORTH)
    client.set_gt_machine(XC - 1, YC, ZC, 11304, NORTH)
    client.set_gt_machine(XC + 1, YC, ZC, 90, NORTH)
    client.set_gt_machine(*INPUT_HATCH, 54, NORTH)
    client.set_gt_machine(*INPUT_BUS, 74, NORTH)
    client.set_gt_machine(XC, YC + 3, ZC + 3, 91, 1)
    # One card in every rack.
    for z in range(ZC + 1, ZC + 6):
        client.teleport(XC + 0.5, YC + 1, z + 0.5)
        for y in range(YC - 1, YC + 2):
            for i, c in enumerate(ROW):
                if c == "A":
                    client.set_container_slot(x0 + i, y, z, 0, "appliedbureaucratics:component", 1, damage=6)
    client.teleport(*STANDING_ON)
    # The ME network: a creative controller beside the front wall, touching the Data Center's controller.
    client.set_block(XC, YC, ZC - 1, "appliedenergistics2:tile.BlockCreativeEnergyController")
    time.sleep(1)


def status(client: HarnessClient, chat: Chat) -> str:
    chat.skip()
    client.chat("/ab status %d %d %d" % CONTROLLER)
    lines = [chat.wait_for(r" at \d+ \d+ \d+:")]
    for _ in range(3):
        lines.append(chat.wait_for(r"^  ", 2))
    return " | ".join(l for l in lines if l)


def planning(text: str) -> float:
    m = re.search(r"Center at .*?planning ([\d.]+) steps/s", text)
    return float(m.group(1)) if m else -1


def wait(client, chat, test, seconds=40) -> str:
    deadline = time.time() + seconds
    s = status(client, chat)
    while not test(s) and time.time() < deadline:
        time.sleep(2)
        s = status(client, chat)
    return s


FTE = 135 * 0.128


def check_coolant(client: HarnessClient, chat: Chat):
    print("[*] Coolant")
    client.fix_maintenance(*CONTROLLER)
    s = wait(client, chat, lambda s: "cooled no" in s and planning(s) > 0)
    print("    " + s)
    assert "135 racks, 135 cards" in s, s
    assert abs(planning(s) - FTE * 4 * 0.25) < 0.3, f"Expected {FTE:.2f} steps/s at the floor, got {s}"
    print(f"[✓] Without water: {planning(s)} steps/s, the floor")


def check_cooled(client: HarnessClient, chat: Chat):
    print("[*] Cooled, untrained")
    client.fill_fluid(*INPUT_HATCH, "water", 16000)
    s = wait(client, chat, lambda s: "cooled yes" in s)
    print("    " + s)
    assert abs(planning(s) - FTE * 4 * 0.25) < 0.3, f"Expected {FTE:.2f} steps/s untrained, got {s}"
    print(f"[✓] Cooled, untrained: {planning(s)} steps/s")


def check_training(client: HarnessClient, chat: Chat):
    print("[*] Training")
    for slot in range(8):
        client.set_container_slot(*INPUT_BUS, slot, "appliedbureaucratics:component", 64, damage=5)
    client.fill_fluid(*INPUT_HATCH, "water", 16000)
    s = wait(client, chat, lambda s: "training 0.500" in s and "cooled yes" in s, 60)
    print("    " + s)
    assert "training 0.500" in s, s
    assert abs(planning(s) - FTE * 4 * 0.625) < 0.5, f"Expected {FTE * 2.5:.2f} steps/s at T = 0.5, got {s}"
    print(f"[✓] 512 Work Logs: T = 0.5, planning at {planning(s)} steps/s")


def main(port: int) -> bool:
    driver = InWorldTestDriver(tag=TAG, port=port)

    def test(client: HarnessClient):
        client.teleport(*STANDING_ON)
        chat = Chat(client)
        check_coolant(client, chat)
        check_cooled(client, chat)
        check_training(client, chat)

    return driver.run(build_fn=build, test_fn=test)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="The Data Center: coolant, training")
    parser.add_argument("--port", type=int, default=int(os.environ.get("MCFLEET_HARNESS_PORT", 25571)))
    args = parser.parse_args()
    sys.exit(0 if main(args.port) else 1)
