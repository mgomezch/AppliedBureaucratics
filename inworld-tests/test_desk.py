#!/usr/bin/env python3
"""
In-world test: the Desk, the HV facility (design document, section 6.1; M1).

A Requisition Tray on a powered ME network, with a BiblioCraft seat, desk, typewriter and bell beside it, six vanilla
bookshelves for Filing, paper and emeralds.  The network can craft stone from cobblestone through a processing
pattern in an ME interface, which pushes the cobblestone into a chest and never gets stone back, so every operation
the Desk dispatches shows up as one cobblestone in that chest.

  Vacancy:     with nobody on the seat the Desk is off ("nobody on the seat"), and a craft cannot even be planned.
  Hiring:      an Employment Contract on a librarian names it and gives a Signed Contract.
  Seating:     the Signed Contract on the seat puts the villager on a workstation mount, where it stays put.
  Working:     the Desk plans at kPlan x 1.5 (librarian) while idle: about 6 steps/s.
  A craft:     /ab craft queues 200 stone; it is planned in the background, then starts on the Desk's CPU and
               dispatches at about 1.25 ops/s (kDisp x the bell).
  Reload:      after saving and reloading, the job is still on the Desk and still dispatching.
  Release:     a Pink Slip dismounts the worker and clears its name.
"""

import os
import sys
import time

from nhdev import NH_DEV_ROOT as REPO_ROOT  # noqa: F401 (puts the workspace on sys.path)

from tools.agentharness import HarnessClient
from tools.inworld_test_driver import InWorldTestDriver

from abtest import Chat, count_items, desk_status, entities

TAG = "ab_desk"
Y = 4
CONTROLLER = (30, Y, 420)
TRAY = (31, Y, 420)
DRIVE = (30, Y, 419)
INTERFACE = (29, Y, 420)
PUSHED = (28, Y, 420)
SEAT = (33, Y, 420)
DESK = (34, Y, 420)
TYPEWRITER = (34, Y + 1, 420)
BELL = (33, Y, 419)
SHELVES = [(x, y, 418) for x in (31, 32, 33) for y in (Y, Y + 1)]
VILLAGER_AT = (33, Y, 423)
STANDING_ON = (32.5, Y, 424.5)


def build(client: HarnessClient):
    client.fill_area(24, Y - 1, 414, 40, Y - 1, 428, "minecraft:stone")
    client.set_block(*CONTROLLER, "appliedenergistics2:tile.BlockCreativeEnergyController")
    client.set_block(*TRAY, "appliedbureaucratics:requisition_tray")
    client.set_block(*DRIVE, "appliedenergistics2:tile.BlockDrive")
    client.set_container_slot(*DRIVE, 0, "appliedenergistics2:item.ItemBasicStorageCell.64k", 1)
    client.set_block(*INTERFACE, "appliedenergistics2:tile.BlockInterface")
    client.set_block(*PUSHED, "minecraft:chest")
    client.set_block(*SEAT, "BiblioCraft:BiblioSeats")
    client.set_block(*DESK, "BiblioCraft:BiblioDesk")
    client.set_block(*TYPEWRITER, "BiblioCraft:BiblioTypewriter")
    client.set_block(*BELL, "BiblioCraft:BiblioBell")
    for shelf in SHELVES:
        client.set_block(*shelf, "minecraft:bookshelf")
    client.set_container_slot(*TRAY, 0, "minecraft:paper", 64)
    client.set_container_slot(*TRAY, 1, "minecraft:emerald", 16)
    client.teleport(*STANDING_ON)
    time.sleep(1)
    # The interface hands what it holds to the network's storage.
    for slot in range(4):
        client.set_container_slot(*INTERFACE, slot, "minecraft:cobblestone", 64)
    client.chat("/ab debug pattern %d %d %d 0 minecraft:cobblestone 1 minecraft:stone 1" % INTERFACE)
    time.sleep(2)


def check_vacancy(client: HarnessClient, chat: Chat):
    print("[*] Vacancy")
    status = desk_status(client, chat, TRAY)
    print(f"    {status}")
    assert "nobody on the seat" in status["bottleneck"], status
    assert status["filing"] == 6 * 192, f"Expected six bookshelves of Filing, got {status}"
    assert status["network"]["facilities"] == 1, status
    print("[✓] An empty seat: the Desk is off, with 1152 B of Filing")


def check_hiring(client: HarnessClient, chat: Chat) -> dict:
    print("[*] Hiring")
    # The driver clears mobs between its phases, so the villager arrives now.
    client.command("/summon Villager %d %d %d {Profession:1}" % VILLAGER_AT)
    time.sleep(2)
    client.set_player_slot(0, "appliedbureaucratics:employment_contract", 1)
    client.select_item("appliedbureaucratics:employment_contract")
    villager = entities(client, "EntityVillager")[0]
    client.look_at(villager["x"], villager["y"] + 1.0, villager["z"])
    time.sleep(0.3)
    client.right_click()
    hired = chat.wait_for(r"Welcome aboard")
    assert hired, "No welcome message"
    villager = entities(client, "EntityVillager")[0]
    assert "," in villager["name"], f"Expected a name and title, got {villager}"
    slots = client.player_inventory()
    assert any(s and s.get("id") == "appliedbureaucratics:signed_contract" for s in slots), slots
    print(f"[✓] Hired: {villager['name']}")
    return villager


def check_seating(client: HarnessClient, chat: Chat):
    print("[*] Seating")
    client.select_item("appliedbureaucratics:signed_contract")
    client.look_at(SEAT[0] + 0.5, SEAT[1] + 0.4, SEAT[2] + 0.5)
    time.sleep(0.3)
    client.right_click()
    assert chat.wait_for(r"has been assigned"), "No assignment message"
    time.sleep(1)
    mounts = entities(client, "EntityWorkstationMount")
    assert len(mounts) == 1, f"Expected one workstation mount, got {mounts}"
    before = entities(client, "EntityVillager")[0]
    assert abs(before["x"] - (SEAT[0] + 0.5)) < 0.3 and abs(before["z"] - (SEAT[2] + 0.5)) < 0.3, before
    time.sleep(10)
    after = entities(client, "EntityVillager")[0]
    assert (after["x"], after["z"]) == (before["x"], before["z"]), f"The worker moved: {before} -> {after}"
    assert not entities(client, "EntitySeat"), "BiblioCraft seated the player instead"
    print("[✓] The worker sits at the desk, and stays there")


def check_working(client: HarnessClient, chat: Chat):
    print("[*] Working")
    time.sleep(6)  # The tray surveys its furniture every 5 s.
    status = desk_status(client, chat, TRAY)
    print(f"    {status}")
    assert status["worker"] == "employee", status
    assert status["bottleneck"] == "Working.", status
    assert 5.5 <= status["planning"] <= 6.5, f"Expected about 6 steps/s from a librarian, got {status}"
    print(f"[✓] Planning at {status['planning']} steps/s")


def check_craft(client: HarnessClient, chat: Chat):
    print("[*] A craft")
    client.chat("/ab craft minecraft:stone 200")
    assert chat.wait_for(r"Queued"), "The craft was not queued"
    t0 = time.time()
    started = chat.wait_for(r"planned and started", 120)
    assert started, "The plan never started"
    planned_in = time.time() - t0
    print(f"    Planned and started after {planned_in:.1f} s")
    status = desk_status(client, chat, TRAY)
    print(f"    {status}")
    assert status["busy"] == 1, status
    before = count_items(client, PUSHED, "Cobblestone")
    time.sleep(20)
    pushed = count_items(client, PUSHED, "Cobblestone") - before
    print(f"    {pushed} operations in 20 s")
    assert 20 <= pushed <= 30, f"Expected about 1.25 ops/s, got {pushed} in 20 s"
    print(f"[✓] Planned in {planned_in:.0f} s, dispatching at {pushed / 20:.2f} ops/s")


def check_reload(client: HarnessClient, chat: Chat, driver: InWorldTestDriver):
    print("[*] Reload")
    client.save_world()
    client.exitworld()
    time.sleep(2.0)
    assert driver.wait_for_in_world(f"test_{TAG}"), "Failed to reload"
    client.teleport(*STANDING_ON)
    time.sleep(8)
    chat = Chat(client)
    status = desk_status(client, chat, TRAY)
    print(f"    {status}")
    assert status["busy"] == 1, f"Expected the job to survive the reload, got {status}"
    before = count_items(client, PUSHED, "Cobblestone")
    time.sleep(10)
    assert count_items(client, PUSHED, "Cobblestone") > before, "The job stopped dispatching after the reload"
    assert len(entities(client, "EntityWorkstationMount")) == 1, "The worker lost its seat in the reload"
    print("[✓] The job and the worker survived the reload")
    return chat


def check_release(client: HarnessClient, chat: Chat):
    print("[*] Release")
    client.set_player_slot(0, "appliedbureaucratics:pink_slip", 1)
    client.select_item("appliedbureaucratics:pink_slip")
    villager = entities(client, "EntityVillager")[0]
    client.look_at(villager["x"], villager["y"] + 1.2, villager["z"])
    time.sleep(0.3)
    client.right_click()
    assert chat.wait_for(r"has been let go"), "No release message"
    time.sleep(2)
    villager = entities(client, "EntityVillager")[0]
    assert "," not in villager["name"], villager
    assert not entities(client, "EntityWorkstationMount"), "The mount stayed"
    print("[✓] Released")


def main(port: int) -> bool:
    driver = InWorldTestDriver(tag=TAG, port=port)

    def test(client: HarnessClient):
        client.teleport(*STANDING_ON)
        chat = Chat(client)
        check_vacancy(client, chat)
        check_hiring(client, chat)
        check_seating(client, chat)
        check_working(client, chat)
        check_craft(client, chat)
        chat = check_reload(client, chat, driver)
        check_release(client, chat)

    return driver.run(build_fn=build, test_fn=test)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="The Desk: hiring, seating, planning, a craft, a reload")
    parser.add_argument("--port", type=int, default=int(os.environ.get("MCFLEET_HARNESS_PORT", 25571)),
                        help="AgentHarness port (default: $MCFLEET_HARNESS_PORT, as `mc-fleet test` sets it)")
    args = parser.parse_args()
    sys.exit(0 if main(args.port) else 1)
