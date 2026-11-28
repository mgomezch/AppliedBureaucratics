#!/usr/bin/env python3
"""
In-world test: founder mode (Note [Founder mode]).

The Desk test's Desk, with the player on its seat instead of a worker.

  Sits:       sitting down opens the founder's GUI, and with no puzzle solved the Desk is "not grinding".
  Grinds:     solving puzzles keeps the founder focused: tickets are closed and the Desk plans.  The test cannot
              see which puzzle is up, so it solves them all by brute force: the digits 1 to 9 (a sum, a sudoku),
              a click on each of the 64 cells (the scary number), and every three letters (a captcha).  Wrong
              answers only flash red.
  Slacks:     with the GUI closed, the founder's focus runs out and the Desk stops within ten seconds.
  Returns:    right-clicking the typewriter while seated opens the GUI again.
"""

import itertools
import os
import sys
import time

from nhdev import NH_DEV_ROOT as REPO_ROOT  # noqa: F401 (puts the workspace on sys.path)

from tools.agentharness import HarnessClient
from tools.inworld_test_driver import InWorldTestDriver

import test_desk
from abtest import Chat, desk_status

TAG = "ab_founder"
SEAT = test_desk.SEAT
TRAY = test_desk.TRAY
TYPEWRITER = test_desk.TYPEWRITER
# FounderGui's panel and its puzzle, in GUI pixels. See FounderGui.build.
PANEL_W, PANEL_H = 320, 180
PUZZLE_X, PUZZLE_Y, PUZZLE_W, PUZZLE_H = 100, 22, 120, 116
LETTERS = "ABCDEFGHJKLMNPRSTUVWXYZ"


def build(client: HarnessClient):
    test_desk.build(client)


def scaled_screen(client: HarnessClient) -> tuple:
    """The scaled screen size, as Minecraft's ScaledResolution works it out with the GUI scale on auto."""
    path = str(client.screenshot()).split(": ", 1)[-1].strip()
    with open(path, "rb") as f:
        head = f.read(24)
    w, h = int.from_bytes(head[16:20], "big"), int.from_bytes(head[20:24], "big")
    scale = 1
    while w // (scale + 1) >= 320 and h // (scale + 1) >= 240:
        scale += 1
    return w // scale, h // scale


def shot(client: HarnessClient, name: str):
    import shutil
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "output")
    os.makedirs(out, exist_ok=True)
    shutil.copy(str(client.screenshot()).split(": ", 1)[-1].strip(), os.path.join(out, name + ".png"))


def gui_open(client: HarnessClient) -> bool:
    """Whether an MUI2 GUI is open: MUI2 wraps its screens in a GuiContainerWrapper."""
    gui = str(client.status().get("gui", ""))
    return "GuiContainerWrapper" in gui or "modularui" in gui.lower()


def solve_round(client: HarnessClient, screen: tuple):
    """One of every kind of answer: whatever puzzle is up, it is solved."""
    client.type_text("123456789")
    left = (screen[0] - PANEL_W) // 2 + PUZZLE_X
    top = (screen[1] - PANEL_H) // 2 + PUZZLE_Y
    cell = min(PUZZLE_W, PUZZLE_H) // 8
    x0 = left + (PUZZLE_W - cell * 8) // 2
    y0 = top + (PUZZLE_H - cell * 8) // 2
    for i in range(64):
        client.click_gui(x0 + (i % 8) * cell + cell // 2, y0 + (i // 8) * cell + cell // 2)
    client.type_text("".join("".join(t) for t in itertools.product(LETTERS, repeat=3)))


def check_sits(client: HarnessClient, chat: Chat):
    print("[*] Sits")
    client.set_player_slot(0)
    client.look_at(SEAT[0] + 0.5, SEAT[1] + 0.3, SEAT[2] + 0.5)
    time.sleep(0.3)
    client.right_click()
    deadline = time.time() + 10
    while time.time() < deadline and not gui_open(client):
        time.sleep(0.5)
    assert gui_open(client), "Sitting down did not open the founder's GUI"
    shot(client, "founder_gui")
    time.sleep(6)
    s = desk_status(client, chat, TRAY)
    print(f"    {s['bottleneck']}")
    assert "not grinding" in s["bottleneck"], f"Expected an unfocused founder, got {s}"
    print("[✓] Sitting down opens the GUI; with nothing solved, the founder is not grinding")


def check_grinds(client: HarnessClient, chat: Chat):
    print("[*] Grinds")
    screen = scaled_screen(client)
    print(f"    scaled screen {screen}")
    for _ in range(4):
        solve_round(client, screen)
    s = desk_status(client, chat, TRAY)
    for _ in range(4):
        if s["bottleneck"] == "Working.":
            break
        solve_round(client, screen)
        s = desk_status(client, chat, TRAY)
    print(f"    {s}")
    assert s["bottleneck"] == "Working.", f"Expected the founder at work, got {s}"
    assert s["worker"] == "founder", s
    assert s["planning"] > 0, s
    print(f"[✓] Grinding: the founder plans at {s['planning']} steps/s")


def check_slacks(client: HarnessClient, chat: Chat):
    print("[*] Slacks")
    client.close_gui()
    time.sleep(12)
    s = desk_status(client, chat, TRAY)
    print(f"    {s['bottleneck']}")
    assert "not grinding" in s["bottleneck"], f"Expected the founder's focus to run out, got {s}"
    print("[✓] With the GUI closed, focus runs out within ten seconds")


def check_returns(client: HarnessClient, chat: Chat):
    print("[*] Returns")
    client.look_at(TYPEWRITER[0] + 0.5, TYPEWRITER[1] + 0.3, TYPEWRITER[2] + 0.5)
    time.sleep(0.3)
    client.right_click()
    time.sleep(2)
    assert gui_open(client), "Right-clicking the typewriter did not open the founder's GUI"
    client.close_gui()
    print("[✓] The typewriter opens the GUI again")


def main(port: int) -> bool:
    driver = InWorldTestDriver(tag=TAG, port=port)

    def test(client: HarnessClient):
        client.teleport(*test_desk.STANDING_ON)
        chat = Chat(client)
        check_sits(client, chat)
        check_grinds(client, chat)
        check_slacks(client, chat)
        check_returns(client, chat)

    return driver.run(build_fn=build, test_fn=test)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Founder mode: the GUI, puzzles and focus")
    parser.add_argument("--port", type=int, default=int(os.environ.get("MCFLEET_HARNESS_PORT", 25571)))
    args = parser.parse_args()
    sys.exit(0 if main(args.port) else 1)
