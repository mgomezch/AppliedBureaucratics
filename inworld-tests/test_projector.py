#!/usr/bin/env python3
"""
In-world test: the hologram projector builds a whole Mainframe.

StructureLib's projector, autobuild and NEI's structure preview all place blocks through the structure definition's
`construct`.  An element that only checks a block (a bare tile adder) places nothing, and leaves a hole where the
player is shown no block: the Terminal Concentrator in the middle of the Mainframe's back face once did.

  Built:      the projector builds the Mainframe on a bare controller, Terminal Concentrator included.
  Forms:      with its hatches swapped in for casings, what the projector built forms.
"""

import os
import sys
import time

from nhdev import NH_DEV_ROOT as REPO_ROOT  # noqa: F401 (puts the workspace on sys.path)

from tools.agentharness import HarnessClient
from tools.inworld_test_driver import InWorldTestDriver

TAG = "ab_projector"
Y = 6
NORTH = 2
SOUTH = 3
MAINFRAME_ID = 32200
CONTROLLER = (60, Y, 380)
# The back face is two blocks behind the front, where the controller is.
CONCENTRATOR = (60, Y, 382)
HATCHES = {(58, Y + 1, 382): 11304, (59, Y + 1, 382): 90, (61, Y + 1, 382): 74, (62, Y + 1, 382): 84}
STANDING_ON = (60.5, Y, 373.5)


def build(client: HarnessClient):
    client.fill_area(50, Y - 1, 370, 70, Y - 1, 392, "minecraft:stone")
    client.teleport(*STANDING_ON)
    time.sleep(2)


def check_built(client: HarnessClient):
    print("[*] Built")
    client.set_gt_machine(*CONTROLLER, MAINFRAME_ID, NORTH)
    print("    " + str(client.autobuild(*CONTROLLER)))
    time.sleep(2)
    block = client.blockinfo(*CONCENTRATOR).get("block")
    assert block == "appliedbureaucratics:terminal_concentrator", f"Expected the concentrator at the back, got {block}"
    print("[✓] The projector builds the Mainframe, Terminal Concentrator included")


def check_forms(client: HarnessClient):
    print("[*] Forms")
    for pos, mte in HATCHES.items():
        client.set_gt_machine(*pos, mte, SOUTH)
    time.sleep(2)
    client.fix_maintenance(*CONTROLLER)
    deadline = time.time() + 30
    info = {}
    while time.time() < deadline:
        info = client.gt_info(*CONTROLLER)
        if info.get("formed"):
            break
        time.sleep(2)
    assert info.get("formed"), f"Expected what the projector built to form, got {info}"
    print("[✓] With its hatches, what the projector built forms")


def main(port: int) -> bool:
    driver = InWorldTestDriver(tag=TAG, port=port)

    def test(client: HarnessClient):
        client.teleport(*STANDING_ON)
        check_built(client)
        check_forms(client)

    return driver.run(build_fn=build, test_fn=test)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="The hologram projector builds a whole Mainframe")
    parser.add_argument("--port", type=int, default=int(os.environ.get("MCFLEET_HARNESS_PORT", 25571)))
    args = parser.parse_args()
    sys.exit(0 if main(args.port) else 1)
