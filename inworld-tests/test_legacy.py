#!/usr/bin/env python3
"""
In-world test: live worlds (design document, section 9; M9).

Crafting CPU blocks set into the world without a player placing them are what an existing base's blocks look like:
they have no `computeMetered` flag, so they are legacy.

  Legacy:     /ab legacy counts them, and a network with a legacy CPU plans unmetered.
  Gate:       stock CPU recipes were moved to ZPM at load (the log says how many).
"""

import os
import re
import sys
import time
from pathlib import Path

from nhdev import NH_DEV_ROOT as REPO_ROOT  # noqa: F401 (puts the workspace on sys.path)

from tools.agentharness import HarnessClient
from tools.inworld_test_driver import InWorldTestDriver

from abtest import Chat

TAG = "ab_legacy"
Y = 4
TRAY = (31, Y, 420)


def build(client: HarnessClient):
    client.fill_area(24, Y - 1, 414, 40, Y - 1, 428, "minecraft:stone")
    client.set_block(30, Y, 420, "appliedenergistics2:tile.BlockCreativeEnergyController")
    client.set_block(*TRAY, "appliedbureaucratics:requisition_tray")
    client.set_block(29, Y, 420, "appliedenergistics2:tile.BlockCraftingStorage", 0)
    client.set_block(28, Y, 420, "appliedenergistics2:tile.BlockCraftingUnit", 0)
    client.teleport(31.5, Y, 424.5)
    time.sleep(2)


def check_legacy(client: HarnessClient, chat: Chat):
    print("[*] Legacy")
    chat.skip()
    client.chat("/ab legacy")
    line = chat.wait_for(r"Loaded crafting CPU blocks")
    print("    " + str(line))
    assert line and re.search(r"(\d+) legacy", line) and int(re.search(r"(\d+) legacy", line).group(1)) >= 2, line
    deadline = time.time() + 30
    net = None
    while time.time() < deadline:
        chat.skip()
        client.chat("/ab status %d %d %d" % TRAY)
        net = chat.wait_for(r"network:", 5)
        if net and "unmetered" in net:
            break
        time.sleep(2)
    print("    " + str(net))
    assert net and "unmetered" in net, f"Expected a network with a legacy CPU to plan unmetered, got {net}"
    print("[✓] Legacy blocks are found, and their network plans unmetered")


def check_gate(client: HarnessClient):
    print("[*] Gate")
    log = Path(client.game_dir()) / "logs" / "fml-client-latest.log"
    text = log.read_text(errors="replace")
    m = re.search(r"Stock crafting CPU blocks moved to ZPM: (\d+) table recipes removed, (\d+) assembler recipes now ZPM, "
                  r"(\d+) added", text)
    assert m, "The recipe gate did not run"
    print("    " + m.group(0))
    assert int(m.group(1)) + int(m.group(2)) > 0, "The gate found no stock CPU recipes"
    print("[✓] " + m.group(0))


def main(port: int) -> bool:
    driver = InWorldTestDriver(tag=TAG, port=port)

    def test(client: HarnessClient):
        chat = Chat(client)
        client.teleport(31.5, Y, 424.5)
        check_legacy(client, chat)
        check_gate(client)

    return driver.run(build_fn=build, test_fn=test)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Live worlds: legacy CPUs and the recipe gate")
    parser.add_argument("--port", type=int, default=int(os.environ.get("MCFLEET_HARNESS_PORT", 25571)))
    args = parser.parse_args()
    sys.exit(0 if main(args.port) else 1)
