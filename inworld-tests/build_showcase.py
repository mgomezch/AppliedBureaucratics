#!/usr/bin/env python3
"""
Builds the Applied Bureaucratics showcase world: a bit of everything, along the X axis of a superflat world.

  x 24..40    The Desk (HV): a librarian at a BiblioCraft desk, bookshelves, a Break Room Counter with coffee.
              South of it (z ~440), a legacy crafting CPU on a network of its own, which plans unmetered.
  x 50..90    The Mainframe (EV): five terminals with librarians and an Enderman team lead under a roof, linked by a
              Router Port to the Internet (IV) at z 440, with a second, one-terminal Mainframe at x 80, and a Gig
              Kiosk with four villagers in a pen.
  x 103..117  The AI Data Center (LuV): a short hall with 135 Accelerator Cards, water, and 512 Work Logs (T = 0.5).
  x 142..158  The Singularity (UV).

Every area has its own ME network with a creative controller, a drive of cobblestone, a crafting terminal, and an
interface with processing patterns whose pushes land in a chest: try `/ab craft minecraft:stone 64`, or the terminal,
and watch the crafting status screen.  Every GregTech machine runs off a debug energy hatch.

It is built with the in-world test builders, on a fleet instance, then copied into the daily -test instance's saves
as `applied_bureaucratics_showcase` (beside, never over, an existing one), unless a game is running from it.

    mc-fleet test --instance <fleet instance> -- python3 build_showcase.py
"""

import os
import shutil
import sys
import time
from pathlib import Path

from nhdev import NH_DEV_ROOT as REPO_ROOT  # noqa: F401 (puts the workspace on sys.path)

from tools.agentharness import HarnessClient
from tools.inworld_test_driver import InWorldTestDriver
from tools.mcfleet.gtnh import latest_saves

import test_datacenter
import test_desk
import test_internet
import test_mainframe
import test_singularity
from abtest import Chat, entities

TAG = "ab_showcase"
SAVE_NAME = "applied_bureaucratics_showcase"
SPAWN = (32, 4, 426)

# The Data Center and the Singularity were tested at x 70; here they get their own ground.
test_datacenter.XC = 110
test_datacenter.CONTROLLER = (110, test_datacenter.YC, test_datacenter.ZC)
test_datacenter.INPUT_HATCH = (108, test_datacenter.YC, test_datacenter.ZC)
test_datacenter.INPUT_BUS = (112, test_datacenter.YC, test_datacenter.ZC)
test_singularity.XC = 150
test_singularity.CONTROLLER = (150, test_singularity.YC, test_singularity.ZC)
test_singularity.CENTER = (150, test_singularity.YC + 8, test_singularity.ZC + 8)


def instance_in_use(game_dir: Path) -> bool:
    game_dir = game_dir.resolve()
    for proc in Path("/proc").iterdir():
        try:
            cwd = (proc / "cwd").resolve()
        except (OSError, RuntimeError):
            continue
        if cwd == game_dir or game_dir in cwd.parents:
            return True
    return False


def me_station(client: HarnessClient, controller: tuple, pattern_slots: list):
    """A drive of cobblestone and an interface with patterns, north of an ME controller; the pushes land in a chest."""
    x, y, z = controller
    client.set_block(x, y + 1, z, "appliedenergistics2:tile.BlockDrive")
    client.set_container_slot(x, y + 1, z, 0, "appliedenergistics2:item.ItemBasicStorageCell.64k", 1)
    client.set_block(x, y, z - 1, "appliedenergistics2:tile.BlockInterface")
    client.set_block(x, y, z - 2, "minecraft:chest")
    client.teleport(x + 0.5, y, z - 4.5)
    time.sleep(1)
    for slot in range(4):
        client.set_container_slot(x, y, z - 1, slot, "minecraft:cobblestone", 64)
    for slot, (src, dst) in enumerate(pattern_slots):
        client.chat("/ab debug pattern %d %d %d %d minecraft:%s 1 minecraft:%s 1" % (x, y, z - 1, slot, src, dst))
    time.sleep(1)


PATTERNS = [("cobblestone", "stone"), ("cobblestone", "gravel"), ("gravel", "sand"), ("sand", "glass"),
            ("stone", "stonebrick")]


def build_legacy(client: HarnessClient):
    """A legacy crafting CPU: blocks set without a player placing them, as an old base's are."""
    y = 4
    client.fill_area(24, y - 1, 434, 40, y - 1, 446, "minecraft:stone")
    client.set_block(30, y, 440, "appliedenergistics2:tile.BlockCreativeEnergyController")
    client.set_block(29, y, 440, "appliedenergistics2:tile.BlockCraftingStorage", 0)
    client.set_block(28, y, 440, "appliedenergistics2:tile.BlockCraftingUnit", 0)
    client.set_block(31, y, 440, "appliedbureaucratics:requisition_tray")
    me_station(client, (30, y, 440), PATTERNS)


def build(client: HarnessClient):
    # Order matters: the Singularity's and the Data Center's builders lay floors over x 55..85.
    client.fill_area(95, test_datacenter.YC - 5, 405, 125, test_datacenter.YC - 4, 440, "minecraft:stone")
    client.fill_area(135, test_singularity.YC - 1, 405, 165, test_singularity.YC - 1, 445, "minecraft:stone")
    test_singularity.build(client)
    me_station(client, (150, test_singularity.YC, 419), PATTERNS)
    client.set_container_slot(152, test_singularity.YC, 420, 0, "appliedbureaucratics:component", 16, damage=8)
    test_datacenter.build(client)
    me_station(client, (110, test_datacenter.YC, 419), PATTERNS)
    test_desk.build(client)
    build_legacy(client)
    test_mainframe.build(client)
    client.fill_area(64, test_internet.Y - 2, 410, 90, test_internet.Y - 2, 450, "minecraft:stone")
    client.fill_area(64, test_internet.Y - 1, 436, 90, test_internet.Y - 1, 450, "minecraft:stone")
    client.fill_area(71, test_internet.Y - 1, 410, 90, test_internet.Y - 1, 435, "minecraft:stone")
    test_internet.build_mainframe(client, 80)
    test_internet.build_internet(client)
    y = test_internet.Y
    for (px, py, pz), cx in zip(test_internet.PORTS, test_internet.MAINFRAMES):
        for z in range(425, pz):
            client.set_block(px, y, z, "OpenComputers:cable")
        lo, hi = sorted((px, cx))
        for x in range(lo, hi + 1):
            client.set_block(x, y, 424, "OpenComputers:cable")
    for x in range(61, 81):
        client.teleport(x + 0.5, y, 417.5)
        client.place_multipart(x, y, 419, "ae2:56")
    for z in range(420, 440):
        client.teleport(72.5, y, z + 0.5)
        client.place_multipart(70, y, z, "ae2:56")
    client.chat("/ab debug pattern 60 %d 418 1 minecraft:cobblestone 1 minecraft:gravel 1" % y)
    # The gig economy: a kiosk and a pen for its workers.
    client.set_block(76, y, 446, "appliedbureaucratics:gig_kiosk")
    for x in range(77, 83):
        for z in (444, 449):
            client.set_block(x, y, z, "minecraft:fence")
    for z in range(444, 450):
        for x in (77, 82):
            client.set_block(x, y, z, "minecraft:fence")
    # A Docking Pad on Mainframe B's terminal network, waiting for a robot.
    client.set_block(81, y, 425, "OpenComputers:cable")
    client.set_block(82, y, 425, "appliedbureaucratics:docking_pad")
    client.command("/gamerule doMobSpawning false")
    client.command("/setworldspawn %d %d %d" % SPAWN)
    client.teleport(*SPAWN)
    time.sleep(2)


def add_terminal(client: HarnessClient, cable: tuple, stand: tuple):
    """A crafting terminal on the face of `cable` that faces `stand`, placed as a player would."""
    client.teleport(*stand)
    time.sleep(0.5)
    client.set_player_slot(0, "appliedenergistics2:item.ItemMultiPart", 1, damage=360)
    client.select_item("appliedenergistics2:item.ItemMultiPart")
    client.look_at(cable[0] + 0.5, cable[1] + 0.5, cable[2] + 0.5)
    client.right_click()
    time.sleep(0.5)
    client.set_player_slot(0)


def staff(client: HarnessClient):
    chat = Chat(client)
    # The Desk: a librarian, coffee.
    client.teleport(*test_desk.STANDING_ON)
    test_desk.check_hiring(client, chat)
    test_desk.check_seating(client, chat)
    # The clerk's signed contract stays in the inventory; the office's staffing would reassign her with it.
    for slot in client.player_inventory():
        if slot and slot.get("id") == "appliedbureaucratics:signed_contract":
            client.set_player_slot(slot["slot"])
    client.set_block(*test_desk.COUNTER, "appliedbureaucratics:break_room_counter")
    client.set_container_slot(*test_desk.COUNTER, 0, "minecraft:dye", 64, damage=3)
    client.set_container_slot(*test_desk.COUNTER, 1, "minecraft:cake", 1)
    # The Mainframe's office and its Enderman lead.
    client.teleport(*test_mainframe.STANDING_ON)
    client.fix_maintenance(*test_mainframe.CONTROLLER)
    test_mainframe.check_office(client, chat)
    test_mainframe.check_management(client, chat)
    # Mainframe B, the Internet, and the gig workers.
    y = test_internet.Y
    client.fix_maintenance(80, y, 420)
    client.fix_maintenance(*test_internet.INTERNET)
    client.command("/summon Villager 80 %d 431 {Profession:0}" % y)
    time.sleep(1)
    villager = min((e for e in entities(client, "EntityVillager", radius=40) if "," not in e["name"]),
                   key=lambda e: (e["x"] - 80.5) ** 2 + (e["z"] - 431.5) ** 2)
    client.set_player_slot(0, "appliedbureaucratics:employment_contract", 1)
    client.select_item("appliedbureaucratics:employment_contract")
    client.teleport(villager["x"], y, villager["z"] + 2.5)
    client.look_at(villager["x"], villager["y"] + 1.0, villager["z"])
    client.right_click()
    assert chat.wait_for(r"Welcome aboard"), "Could not hire Mainframe B's worker"
    test_mainframe.seat(client, chat, (80, y, 427))
    for i in range(4):
        client.command("/summon Villager %d %d %d" % (78 + i, y, 446))
    # The Data Center: water and Work Logs; the Singularity and the legacy network: maintenance.
    client.teleport(110.5, test_datacenter.YC - 3, 416.5)
    client.fix_maintenance(*test_datacenter.CONTROLLER)
    client.fill_fluid(*test_datacenter.INPUT_HATCH, "water", 64000)
    for slot in range(8):
        client.set_container_slot(*test_datacenter.INPUT_BUS, slot, "appliedbureaucratics:component", 64, damage=5)
    client.teleport(150.5, test_singularity.YC, 415.5)
    client.fix_maintenance(*test_singularity.CONTROLLER)
    # Crafting terminals, one per network, on a cable beside each network's interface, placed from the east.
    dc_y = test_datacenter.YC
    client.fill_area(111, dc_y - 1, 418, 115, dc_y - 1, 418, "minecraft:stone")
    for cable in ((30, 4, 421), (31, 4, 439), (61, test_internet.Y, 418), (111, dc_y, 418),
                  (151, test_singularity.YC, 418)):
        stand = (cable[0] + 3.5, cable[1], cable[2] + 0.5)
        client.teleport(*stand)
        time.sleep(0.5)
        client.place_multipart(*cable, "ae2:56")
        add_terminal(client, cable, stand)
    client.teleport(*SPAWN)
    time.sleep(5)
    for pos in (test_desk.TRAY, test_mainframe.CONTROLLER, test_internet.INTERNET, test_datacenter.CONTROLLER,
                test_singularity.CONTROLLER, (31, 4, 440)):
        chat.skip()
        client.chat("/ab status %d %d %d" % pos)
        print("    " + str(chat.wait_for(r" at \d+ \d+ \d+:")))


def install(driver: InWorldTestDriver):
    built = driver._resolve_saves() / f"{TAG}_post"
    saves = latest_saves("test", project="AppliedBureaucratics")
    if instance_in_use(saves.parent):
        print(f"[!] Not installing into {saves}: a game is running from that instance")
        return
    saves.mkdir(exist_ok=True)
    dest = saves / SAVE_NAME
    if dest.exists():
        dest = saves / f"{SAVE_NAME}-{time.strftime('%Y%m%d-%H%M')}"
    shutil.copytree(built, dest)
    (dest / "session.lock").unlink(missing_ok=True)
    print(f"[✓] Showcase world installed at {dest}")


def main(port: int) -> bool:
    driver = InWorldTestDriver(tag=TAG, port=port)
    ok = driver.run(build_fn=build, test_fn=staff)
    if ok:
        install(driver)
    return ok


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Build the Applied Bureaucratics showcase world")
    parser.add_argument("--port", type=int, default=int(os.environ.get("MCFLEET_HARNESS_PORT", 25571)))
    args = parser.parse_args()
    sys.exit(0 if main(args.port) else 1)
