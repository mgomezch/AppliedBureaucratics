"""Helpers for Applied Bureaucratics' in-world tests: chat from the client log, /ab status, and the AE2 setup."""

import re
import time
from pathlib import Path
from typing import Dict, List, Optional

from nhdev import NH_DEV_ROOT  # noqa: F401 (puts the workspace on sys.path)

from tools.agentharness import HarnessClient


class Chat:
    """The chat lines the client logs from now on, read in order."""

    def __init__(self, client: HarnessClient):
        self.log = Path(client.game_dir()) / "logs" / "latest.log"
        self.offset = self.log.stat().st_size if self.log.exists() else 0
        self.pending: List[str] = []

    def _read(self):
        with open(self.log, "rb") as f:
            f.seek(self.offset)
            data = f.read()
        # Only whole lines; a partial last line is read next time.
        end = data.rfind(b"\n") + 1
        self.offset += end
        for line in data[:end].decode("utf-8", "replace").splitlines():
            if "[CHAT] " in line:
                self.pending.append(re.sub("§.", "", line.split("[CHAT] ", 1)[1]))

    def skip(self):
        """Forgets every line logged so far."""
        self._read()
        self.pending.clear()

    def wait_for(self, pattern: str, seconds: float = 10.0) -> Optional[str]:
        """The next line matching `pattern`, dropping the lines before it."""
        deadline = time.time() + seconds
        seen: List[str] = []
        while True:
            self._read()
            while self.pending:
                line = self.pending.pop(0)
                seen.append(line)
                if re.search(pattern, line):
                    return line
            if time.time() >= deadline:
                print(f"    (chat seen: {seen[-8:]})")
                return None
            time.sleep(0.25)


STATUS_LINE = re.compile(
    r"planning (?P<planning>[\d.]+) steps/s, dispatch (?P<dispatch>[\d.]+) ops/s, filing (?P<used>\d+)/(?P<filing>\d+) B, "
    r"morale (?P<morale>\d+), (?P<paid>paid|unpaid), worker (?P<worker>\w+), slots busy (?P<busy>\d+)")
NETWORK_LINE = re.compile(
    r"network: (?P<facilities>\d+) facilities, planning (?P<rate>[\d.]+) steps/s, credit (?P<credit>-?[\d.]+), (?P<plans>\d+) plans")


def desk_status(client: HarnessClient, chat: Chat, tray: tuple) -> Dict[str, object]:
    """Runs /ab status for the tray at `tray` and parses its three lines."""
    chat.skip()
    client.chat("/ab status %d %d %d" % tray)
    head = chat.wait_for(r"^Desk at")
    assert head, "No /ab status reply"
    body = chat.wait_for(r"planning .* steps/s, dispatch")
    net = chat.wait_for(r"network:", 3)
    status: Dict[str, object] = {"bottleneck": head.split(": ", 1)[1]}
    m = STATUS_LINE.search(body or "")
    if m:
        status.update({k: float(v) if k in ("planning", "dispatch") else v for k, v in m.groupdict().items()})
        for k in ("used", "filing", "morale", "busy"):
            status[k] = int(status[k])
    n = NETWORK_LINE.search(net or "")
    if n:
        status["network"] = {k: float(v) for k, v in n.groupdict().items()}
    return status


def entities(client: HarnessClient, cls: str, radius: float = 12) -> List[dict]:
    return [e for e in client.nearby_entities(radius=radius) if e.get("class") == cls]


def count_items(client: HarnessClient, pos: tuple, name: str) -> int:
    info = client.blockinfo(*pos)
    return sum(i.get("count", 0) for i in info.get("items", []) if name in i.get("name", ""))
