#!/usr/bin/env python3
"""
In-world test: the Mainframe, the EV facility (design document, section 6.2; M3 and M4).

A Mainframe facing north, its four racks holding tier 1 servers (32 FTE of hardware), a debug energy hatch, an
maintenance hatch (fixed by the harness) and input and output buses at the back, and its Terminal Concentrator wired by OC cable to five
separate tier 1 screens.  Each screen is a workstation: an OC keyboard on the floor south of it and a BiblioCraft
seat south of the keyboard.  The ME network is the Desk
test's: a creative controller, a drive, and an interface whose stone pattern pushes cobblestone into a chest.

  Formed:      the structure forms; with nobody seated it reports no workers.
  Office:      five librarians, hired and seated, are five terminals' workers: five unmanaged roots are worth
               5 / sqrt(5) FTE, planning at that x 4 x 1.5 steps/s while idle.  Each faces its keyboard, north.
  Keyboards:   with the keyboards gone, nobody counts and the Mainframe says why; put back, all five count again.
  A craft:     200 stone dispatch at the office's rate, about 2.2 ops/s (librarians have no dispatch affinity).
  Management:  an Enderman at a desk with a bell, under a roof so the sun does not make it teleport, becomes their
               team lead: 5 x 0.8 = 4 FTE.
"""

import math
import os
import sys
import time

from nhdev import NH_DEV_ROOT as REPO_ROOT  # noqa: F401 (puts the workspace on sys.path)

from tools.agentharness import HarnessClient
from tools.inworld_test_driver import InWorldTestDriver

from abtest import NORTH_YAW, Chat, count_items, desk_status, entities, faces

TAG = "ab_mainframe"
Y = 6
CONTROLLER = (60, Y, 420)
MAINFRAME_ID = 32200
NORTH = 2
SOUTH = 3
AE_CONTROLLER = (60, Y, 419)
DRIVE = (60, Y + 1, 419)
INTERFACE = (60, Y, 418)
PUSHED = (60, Y, 417)
SCREENS_X = (56, 58, 60, 62, 64)
SCREEN_Z = 425
KEYBOARD_Z = 426
SEAT_Z = 427
MANAGER_SEAT = (60, Y, 430)
STANDING_ON = (60.5, Y, 432.5)


def mainframe_blocks():
    """Every block of the Mainframe but its controller and hatches: {pos: (block, meta)}."""
    casing = "appliedbureaucratics:appliedbureaucratics.casing"
    blocks = {}
    for x in range(57, 64):
        for z in (420, 421, 422):
            for y in range(Y - 1, Y + 3):
                blocks[(x, y, z)] = (casing, 0)
    for x in range(58, 63):
        blocks[(x, Y + 1, 420)] = (casing, 1)
    for x in (58, 59, 61, 62):
        blocks[(x, Y, 420)] = ("OpenComputers:rack", 0)
        blocks[(x, Y, 421)] = ("minecraft:air", 0)
    blocks[(60, Y, 422)] = ("appliedbureaucratics:terminal_concentrator", 0)
    del blocks[CONTROLLER]
    return blocks


HATCHES = {(58, Y + 1, 422): 11304, (59, Y + 1, 422): 90, (61, Y + 1, 422): 74, (62, Y + 1, 422): 84}
INPUT_BUS = (61, Y + 1, 422)
OUTPUT_BUS = (62, Y + 1, 422)


def build(client: HarnessClient):
    client.fill_area(50, Y - 2, 410, 70, Y - 2, 435, "minecraft:stone")
    client.fill_area(50, Y - 1, 423, 70, Y - 1, 435, "minecraft:stone")
    for pos, (block, meta) in mainframe_blocks().items():
        client.set_block(*pos, block, meta)
    client.set_gt_machine(*CONTROLLER, MAINFRAME_ID, NORTH)
    for pos, mte in HATCHES.items():
        client.set_gt_machine(*pos, mte, SOUTH)
    client.teleport(*STANDING_ON)
    time.sleep(1)
    for x in (58, 59, 61, 62):
        client.chat("/ab debug ocitem %d %d %d 0 server1" % (x, Y, 420))
    client.set_container_slot(*INPUT_BUS, 0, "appliedbureaucratics:component", 4, damage=0)
    client.set_container_slot(*INPUT_BUS, 1, "minecraft:paper", 64)
    client.set_container_slot(*INPUT_BUS, 2, "minecraft:emerald", 16)
    client.set_container_slot(*INPUT_BUS, 3, "minecraft:ender_pearl", 4)
    client.set_container_slot(*INPUT_BUS, 4, "appliedbureaucratics:component", 4, damage=4)
    # The office: cable from the concentrator to a row of separate screens, each with a keyboard and a seat south.
    client.set_block(60, Y, 423, "OpenComputers:cable")
    for x in range(55, 66):
        client.set_block(x, Y, 424, "OpenComputers:cable")
    for x in SCREENS_X:
        client.set_block(x, Y, SCREEN_Z, "OpenComputers:screen1")
        client.set_block(x, Y, SEAT_Z, "BiblioCraft:BiblioSeats")
    add_keyboards(client)
    # A manager's office: a seat with a desk and a bell beside it.
    client.set_block(*MANAGER_SEAT, "BiblioCraft:BiblioSeats")
    client.set_block(MANAGER_SEAT[0] + 1, Y, MANAGER_SEAT[2], "BiblioCraft:BiblioDesk")
    client.set_block(MANAGER_SEAT[0] - 1, Y, MANAGER_SEAT[2], "BiblioCraft:BiblioBell")
    # Under a roof, lit from the floor: Endermen teleport in daylight when they can see the sky.
    client.fill_area(54, Y + 3, 428, 66, Y + 3, 436, "minecraft:stone")
    for x in range(55, 66, 3):
        for z in range(429, 436, 3):
            client.set_block(x, Y - 1, z, "minecraft:glowstone")
    # The ME network.
    client.set_block(*AE_CONTROLLER, "appliedenergistics2:tile.BlockCreativeEnergyController")
    client.set_block(*DRIVE, "appliedenergistics2:tile.BlockDrive")
    client.set_container_slot(*DRIVE, 0, "appliedenergistics2:item.ItemBasicStorageCell.64k", 1)
    client.set_block(*INTERFACE, "appliedenergistics2:tile.BlockInterface")
    client.set_block(*PUSHED, "minecraft:chest")
    time.sleep(1)
    for slot in range(4):
        client.set_container_slot(*INTERFACE, slot, "minecraft:cobblestone", 64)
    client.chat("/ab debug pattern %d %d %d 0 minecraft:cobblestone 1 minecraft:stone 1" % INTERFACE)
    time.sleep(2)


def status(client: HarnessClient, chat: Chat) -> dict:
    chat.skip()
    client.chat("/ab status %d %d %d" % CONTROLLER)
    head = chat.wait_for(r"^Mainframe at|^.* at \d+ \d+ \d+:")
    assert head, "No /ab status reply"
    body = chat.wait_for(r"planning .* steps/s, dispatch")
    office = chat.wait_for(r"office:", 3)
    s = {"head": head, "body": body, "office": office}
    import re
    m = re.search(r"planning ([\d.]+) steps/s, dispatch ([\d.]+) ops/s.*slots busy (\d+)", body or "")
    if m:
        s.update(planning=float(m.group(1)), dispatch=float(m.group(2)), busy=int(m.group(3)))
    o = re.search(r"(\d+) terminals, (\d+) workers, (\d+) leads, (\d+) directors, (\d+) unmanaged, hardware ([\d.]+) FTE, "
                  r"fte ([\d.]+)", office or "")
    if o:
        s.update(terminals=int(o.group(1)), workers=int(o.group(2)), leads=int(o.group(3)), unmanaged=int(o.group(5)),
                 hardware=float(o.group(6)), fte=float(o.group(7)))
    return s


def wait_status(client: HarnessClient, chat: Chat, test, seconds: float = 30) -> dict:
    deadline = time.time() + seconds
    s = status(client, chat)
    while not test(s) and time.time() < deadline:
        time.sleep(2)
        s = status(client, chat)
    return s


def hire(client: HarnessClient, chat: Chat, entity: dict, contract: str):
    """Hires `entity`, retrying with its new position if it walked off before the click landed, and from another side
    if something (a seat, when it wandered in among the workstations) was in the way."""
    sides = ((0, 2.5), (2.5, 0), (-2.5, 0), (0, -2.5))
    for attempt in range(8):
        if attempt:
            moved = [e for e in client.nearby_entities(radius=40) if e.get("id") == entity["id"]]
            if not moved:
                break
            entity = moved[0]
        client.set_player_slot(0, contract, 1)
        client.select_item(contract)
        dx, dz = sides[attempt % len(sides)]
        client.teleport(entity["x"] + dx, Y, entity["z"] + dz)
        time.sleep(0.3)
        client.look_at(entity["x"], entity["y"] + 1.0, entity["z"])
        client.right_click()
        if chat.wait_for(r"Welcome aboard", 3):
            return
    raise AssertionError(f"Could not hire {entity}")


def add_keyboards(client: HarnessClient, xs=SCREENS_X, by_hand: bool = True):
    """A keyboard on the floor between each screen and its seat.

    By hand, each is placed as a player would, standing on the empty seat and looking north at the floor, so it lies
    flat with its keys towards the seat; `set_block` would stand it on edge against the screen.  Placing by hand needs
    the seats empty: a seated worker would take the click."""
    if not by_hand:
        for x in xs:
            client.set_block(x, Y, KEYBOARD_Z, "OpenComputers:keyboard")
        return
    client.set_player_slot(0, "OpenComputers:keyboard", 64)
    client.select_item("OpenComputers:keyboard")
    for x in xs:
        client.teleport(x + 0.5, Y + 1, SEAT_Z + 0.5)
        time.sleep(0.3)
        client.look_at(x + 0.5, Y, KEYBOARD_Z + 0.4)
        client.right_click()
        time.sleep(0.2)
    client.set_player_slot(0)


def seat(client: HarnessClient, chat: Chat, seat_pos: tuple, side=(1, 0)):
    """Seats the worker of the selected signed contract, standing on `side` (dx, dz) of the seat: by default east,
    away from where the workers wait and clear of the keyboard in front of the seat."""
    client.select_item("appliedbureaucratics:signed_contract")
    client.teleport(seat_pos[0] + 0.5 + side[0], Y, seat_pos[2] + 0.5 + side[1])
    time.sleep(0.3)
    client.look_at(seat_pos[0] + 0.5, seat_pos[1] + 0.4, seat_pos[2] + 0.5)
    time.sleep(0.3)
    client.right_click()
    assert chat.wait_for(r"has been assigned"), f"Could not seat at {seat_pos}"
    # The signed contract stays in the inventory: clear it so the next one is selected.
    inv = client.player_inventory()
    for s in inv:
        if s and s.get("id") == "appliedbureaucratics:signed_contract":
            client.set_player_slot(s["slot"])


def check_formed(client: HarnessClient, chat: Chat):
    print("[*] Formed")
    time.sleep(3)
    client.fix_maintenance(*CONTROLLER)
    s = wait_status(client, chat, lambda s: s.get("hardware", 0) > 0 and "no power" not in s["head"], 60)
    print(f"    {s}")
    assert s.get("hardware") == 32, f"Expected 32 FTE of hardware from four tier 1 servers, got {s}"
    assert s.get("terminals") == len(SCREENS_X), f"Expected {len(SCREENS_X)} terminals, got {s}"
    assert "nobody" in s["head"], s
    print("[✓] Formed: 32 FTE of hardware, five terminals, nobody at them")


def check_office(client: HarnessClient, chat: Chat):
    print("[*] Office")
    for x in SCREENS_X:
        client.command("/summon Villager %d %d %d {Profession:1}" % (x, Y, SEAT_Z + 4))
    time.sleep(2)
    for x in SCREENS_X:
        villager = min(entities(client, "EntityVillager", radius=30),
                       key=lambda e: 1e9 if "," in e["name"] else (e["x"] - (x + 0.5)) ** 2
                       + (e["z"] - (SEAT_Z + 4.5)) ** 2)
        hire(client, chat, villager, "appliedbureaucratics:employment_contract")
        seat(client, chat, (x, Y, SEAT_Z))
    client.teleport(*STANDING_ON)
    fte = len(SCREENS_X) / math.sqrt(len(SCREENS_X))
    s = wait_status(client, chat, lambda s: s.get("workers") == len(SCREENS_X) and s.get("planning", 0) > 0)
    print(f"    {s}")
    assert s.get("workers") == len(SCREENS_X), s
    assert abs(s["fte"] - fte) < 0.01, f"Expected {fte:.3f} FTE from five unmanaged workers, got {s}"
    assert abs(s["planning"] - fte * 4 * 1.5) < 0.3, f"Expected about {fte * 6:.1f} steps/s, got {s}"
    seated = [e for e in entities(client, "EntityVillager", radius=30) if abs(e["z"] - (SEAT_Z + 0.5)) < 0.3]
    assert len(seated) == len(SCREENS_X), f"Expected {len(SCREENS_X)} seated villagers, got {seated}"
    turned = [e for e in seated if not faces(e, NORTH_YAW)]
    assert not turned, f"Expected every worker to face its keyboard, north: {turned}"
    print(f"[✓] Five librarians, facing their keyboards: {s['fte']:.2f} FTE, planning at {s['planning']} steps/s")


def check_keyboards(client: HarnessClient, chat: Chat):
    print("[*] Keyboards")
    for x in SCREENS_X:
        client.set_block(x, Y, KEYBOARD_Z, "minecraft:air")
    s = wait_status(client, chat, lambda s: s.get("workers") == 0)
    print(f"    {s}")
    assert s.get("workers") == 0, f"Expected no workers without keyboards, got {s}"
    assert "no keyboard" in s["head"], f"Expected the Mainframe to blame the keyboards, got {s}"
    add_keyboards(client, by_hand=False)
    client.teleport(*STANDING_ON)
    s = wait_status(client, chat, lambda s: s.get("workers") == len(SCREENS_X))
    assert s.get("workers") == len(SCREENS_X), f"Expected all five back at work, got {s}"
    print("[✓] Without keyboards nobody counts, and the Mainframe says so; with them back, all five do")


def check_craft(client: HarnessClient, chat: Chat):
    print("[*] A craft")
    client.chat("/ab craft minecraft:stone 200")
    assert chat.wait_for(r"Queued"), "Not queued"
    assert chat.wait_for(r"planned and started", 120), "Never started"
    time.sleep(3)
    before = count_items(client, PUSHED, "Cobblestone")
    time.sleep(20)
    pushed = count_items(client, PUSHED, "Cobblestone") - before
    rate = pushed / 20
    print(f"    {pushed} operations in 20 s")
    expected = len(SCREENS_X) / math.sqrt(len(SCREENS_X))
    assert abs(rate - expected) < 0.6, f"Expected about {expected:.2f} ops/s, got {rate:.2f}"
    print(f"[✓] Dispatching at {rate:.2f} ops/s")


def check_management(client: HarnessClient, chat: Chat):
    print("[*] Management")
    # The driver keeps hostile mobs out; an Enderman manager needs them let in.
    client.set_no_mobs(False)
    client.set_difficulty("easy")
    # Special Mobs replaces vanilla Endermen with variants of its own, some of which teleport about, so the hire is
    # retried, aiming low, with a new Enderman when the last one got away.
    hired = False
    for attempt in range(6):
        if attempt % 2 == 0:
            client.command("/summon Enderman %d %d %d" % (MANAGER_SEAT[0], Y, MANAGER_SEAT[2] + 2))
            time.sleep(0.5)
        found = [e for e in client.nearby_entities(radius=30)
                 if "Enderman" in e.get("class", "") and "," not in e.get("name", "")]
        if not found:
            continue
        enderman = min(found, key=lambda e: e.get("distance", 99))
        client.set_player_slot(0, "appliedbureaucratics:management_contract", 1)
        client.select_item("appliedbureaucratics:management_contract")
        client.teleport(enderman["x"], Y, enderman["z"] + 2.0)
        client.look_at(enderman["x"], enderman["y"] + 0.5, enderman["z"])
        client.right_click()
        if chat.wait_for(r"Welcome aboard", 3):
            hired = True
            break
    assert hired, "Could not hire an Enderman"
    # From the north: the desk is east of the seat, and the Enderman waits south of it.
    seat(client, chat, MANAGER_SEAT, side=(0, -1))
    client.teleport(*STANDING_ON)
    s = wait_status(client, chat, lambda s: s.get("leads") == 1)
    print(f"    {s}")
    assert s.get("leads") == 1, f"Expected the Enderman to lead, got {s}"
    assert abs(s["fte"] - len(SCREENS_X) * 0.8) < 0.01, f"Expected {len(SCREENS_X) * 0.8} FTE, got {s}"
    time.sleep(5)
    assert any("," in e.get("name", "") and "Enderman" in e.get("class", "")
               for e in client.nearby_entities(radius=30)), "The manager teleported away or despawned"
    print(f"[✓] An Enderman leads the team: {s['fte']:.2f} FTE")


def main(port: int) -> bool:
    driver = InWorldTestDriver(tag=TAG, port=port)

    def test(client: HarnessClient):
        client.teleport(*STANDING_ON)
        chat = Chat(client)
        check_formed(client, chat)
        check_office(client, chat)
        check_keyboards(client, chat)
        check_craft(client, chat)
        check_management(client, chat)

    return driver.run(build_fn=build, test_fn=test)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="The Mainframe: structure, office, a craft, a manager")
    parser.add_argument("--port", type=int, default=int(os.environ.get("MCFLEET_HARNESS_PORT", 25571)))
    args = parser.parse_args()
    sys.exit(0 if main(args.port) else 1)
