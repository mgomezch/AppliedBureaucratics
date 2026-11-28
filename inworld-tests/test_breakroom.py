#!/usr/bin/env python3
"""
In-world test: break rooms (Note [Break rooms]).

The Desk test's Desk, with a librarian at it, and a break room in the Desk's chunk, east of the seat: a microkitchen
of a Cooking for Blockheads sink and fridge and a potted plant (2 x 4 + 1 = 9 points) around the machine's spot.

  Cooler:     a Water Cooler under an Iron Tanks tank of water binds the worker: morale +5, and +9 for the kitchen.
  Break:      sent on its break, the worker leaves its seat (the Desk is "on a break"), walks to the cooler, which
              pours it a cup (a visit), and comes back and sits down facing its typewriter.
  Coffee:     a Coffee Machine with beans and sugar in the cooler's place takes the worker over.  Black coffee is
              worth 4; after a cup with sugar, the Desk's coffee is worth 6.  The cup took a bean and a sugar.
  Snacks:     a Snack Vending Machine with three kinds of sandwich: +4, and +2 a kind, 10 with the kitchen's 9.  A
              break sells a sandwich.
"""

import os
import re
import sys
import time

from nhdev import NH_DEV_ROOT as REPO_ROOT  # noqa: F401 (puts the workspace on sys.path)

from tools.agentharness import HarnessClient
from tools.inworld_test_driver import InWorldTestDriver

import test_desk
from abtest import EAST_YAW, Chat, desk_status, entities, faces

TAG = "ab_breakroom"
Y = test_desk.Y
SEAT = test_desk.SEAT
TRAY = test_desk.TRAY
# In the seat's chunk (x 32..47, z 416..431), four blocks from it.
ROOM = (36, Y, 416)
TANK = (ROOM[0], Y + 1, ROOM[2])
KITCHEN = {(35, Y, 415): "cookingforblockheads:sink", (37, Y, 415): "cookingforblockheads:fridge",
           (38, Y, 416): "minecraft:flower_pot"}
KITCHEN_POINTS = 9
COOLER, COFFEE, SNACKS = 32210, 32211, 32212
SOUTH = 3
SUPPLY = 3  # The first supply slot.


def build(client: HarnessClient):
    test_desk.build(client)
    for pos, block in KITCHEN.items():
        client.set_block(*pos, block)


def perk(status: dict) -> int:
    for p in status.get("perks", []):
        if p.startswith("break room +"):
            return int(p.split("+")[1])
    return 0


def wait_perk(client: HarnessClient, chat: Chat, value: int, seconds: float = 20) -> dict:
    deadline = time.time() + seconds
    s = desk_status(client, chat, TRAY)
    while perk(s) != value and time.time() < deadline:
        time.sleep(2)
        s = desk_status(client, chat, TRAY)
    return s


def wait_working(client: HarnessClient, chat: Chat, seconds: float = 15) -> dict:
    deadline = time.time() + seconds
    s = desk_status(client, chat, TRAY)
    while s["bottleneck"] != "Working." and time.time() < deadline:
        time.sleep(2)
        s = desk_status(client, chat, TRAY)
    return s


def place(client: HarnessClient, machine: int):
    client.set_block(*ROOM, "minecraft:air")
    time.sleep(0.5)
    client.set_gt_machine(*ROOM, machine, SOUTH)
    time.sleep(1)


def scan(client: HarnessClient, chat: Chat):
    chat.skip()
    client.chat("/ab debug breakroom %d %d %d" % ROOM)
    assert chat.wait_for(r"Scanning"), "The break room did not scan"


def villager(client: HarnessClient) -> dict:
    return [e for e in entities(client, "EntityVillager", radius=30) if "," in e["name"]][0]


def take_break(client: HarnessClient, chat: Chat):
    """Sends the worker on its break, and waits for it to sit down again."""
    chat.skip()
    client.chat("/ab debug break %d %d %d" % SEAT)
    assert chat.wait_for(r"Break due"), "No break"
    time.sleep(3)
    away = desk_status(client, chat, TRAY)
    if "on a break" not in away["bottleneck"]:
        time.sleep(5)
        away = desk_status(client, chat, TRAY)
    print(f"    away: {away['bottleneck']}")
    assert "on a break" in away["bottleneck"], f"Expected the Desk to say its worker is on a break, got {away}"
    deadline = time.time() + 120
    while time.time() < deadline and not entities(client, "EntityWorkstationMount", radius=30):
        time.sleep(1)
    assert entities(client, "EntityWorkstationMount", radius=30), "The worker never sat down again"


def check_cooler(client: HarnessClient, chat: Chat):
    print("[*] Cooler")
    place(client, COOLER)
    client.set_block(*TANK, "irontank:ironTank")
    time.sleep(0.5)
    print("    " + str(client.fill_fluid(*TANK, "water", 8000)))
    scan(client, chat)
    s = wait_perk(client, chat, 5 + KITCHEN_POINTS)
    print(f"    {s}")
    assert perk(s) == 5 + KITCHEN_POINTS, f"Expected the cooler's 5 and the kitchen's 9, got {s}"
    print(f"[✓] A Water Cooler under a tank of water: morale +{perk(s)}")


def room_status(client: HarnessClient, chat: Chat) -> dict:
    """/ab status of the break room: its head line, and its visits."""
    chat.skip()
    client.chat("/ab status %d %d %d" % ROOM)
    head = chat.wait_for(r" at %d %d %d: " % ROOM)
    body = chat.wait_for(r"serving \d+ workers")
    m = re.search(r"serving (\d+) workers, (\d+) visits", body or "")
    return {"head": head, "body": body, "workers": int(m.group(1)) if m else -1, "visits": int(m.group(2)) if m else -1}


def check_break(client: HarnessClient, chat: Chat):
    print("[*] Break")
    before = room_status(client, chat)
    take_break(client, chat)
    after = room_status(client, chat)
    print(f"    {after['head']} | {after['body']}")
    assert after["visits"] == before["visits"] + 1, f"Expected the worker to visit the cooler for a cup: {before} -> {after}"
    time.sleep(2)
    assert faces(villager(client), EAST_YAW), f"Expected the worker back facing its typewriter: {villager(client)}"
    s = wait_working(client, chat)
    assert s["bottleneck"] == "Working.", f"Expected the Desk back at work, got {s}"
    print("[✓] The worker went to the cooler for a cup and came back to its typewriter")


def items(client: HarnessClient, pos: tuple) -> dict:
    out = {}
    for i in client.blockinfo(*pos).get("items", []):
        out[i.get("name", "")] = out.get(i.get("name", ""), 0) + i.get("count", 0)
    return out


def check_coffee(client: HarnessClient, chat: Chat):
    print("[*] Coffee")
    client.set_block(*TANK, "minecraft:air")
    place(client, COFFEE)
    client.charge_energy(*ROOM, 500000)
    client.set_container_slot(*ROOM, SUPPLY, "harvestcraft:coffeebeanItem", 8)
    client.set_container_slot(*ROOM, SUPPLY + 1, "minecraft:sugar", 8)
    time.sleep(2)
    scan(client, chat)
    s = wait_perk(client, chat, 4 + KITCHEN_POINTS)
    print(f"    {s}")
    room = room_status(client, chat)
    print(f"    {room['head']} | {room['body']}")
    assert perk(s) == 4 + KITCHEN_POINTS, f"Expected black coffee's 4 and the kitchen's 9, got {s}"
    before = items(client, ROOM)
    take_break(client, chat)
    after = items(client, ROOM)
    print(f"    {before} -> {after}")
    s = wait_perk(client, chat, 6 + KITCHEN_POINTS)
    print(f"    {s}")
    assert perk(s) == 6 + KITCHEN_POINTS, f"Expected coffee with sugar's 6 and the kitchen's 9, got {s}"
    assert sum(before.values()) - sum(after.values()) == 2, f"Expected a bean and a sugar used: {before} -> {after}"
    print(f"[✓] A cup with sugar: the Desk's coffee is worth 6, morale +{perk(s)}")


def check_snacks(client: HarnessClient, chat: Chat):
    print("[*] Snacks")
    place(client, SNACKS)
    client.charge_energy(*ROOM, 5000000)
    for i, name in enumerate(("harvestcraft:blueberryjellysandwichItem", "harvestcraft:honeysandwichItem",
                              "harvestcraft:chickensandwichItem")):
        client.set_container_slot(*ROOM, SUPPLY + i, name, 4)
    time.sleep(2)
    scan(client, chat)
    s = wait_perk(client, chat, 10 + KITCHEN_POINTS)
    print(f"    {s}")
    assert perk(s) == 10 + KITCHEN_POINTS, f"Expected three kinds of sandwich's 10 and the kitchen's 9, got {s}"
    before = sum(items(client, ROOM).values())
    take_break(client, chat)
    after = sum(items(client, ROOM).values())
    assert before - after == 1, f"Expected a sandwich sold: {before} -> {after}"
    print(f"[✓] Three kinds of sandwich: morale +{perk(s)}, and a break sells one")


def main(port: int) -> bool:
    driver = InWorldTestDriver(tag=TAG, port=port)

    def test(client: HarnessClient):
        client.teleport(*test_desk.STANDING_ON)
        chat = Chat(client)
        test_desk.check_hiring(client, chat)
        test_desk.check_seating(client, chat)
        check_cooler(client, chat)
        check_break(client, chat)
        check_coffee(client, chat)
        check_snacks(client, chat)

    return driver.run(build_fn=build, test_fn=test)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Break rooms: a cooler, coffee, snacks, and a break")
    parser.add_argument("--port", type=int, default=int(os.environ.get("MCFLEET_HARNESS_PORT", 25571)))
    args = parser.parse_args()
    sys.exit(0 if main(args.port) else 1)
