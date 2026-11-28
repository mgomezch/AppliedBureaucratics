# Applied Bureaucratics: the manual

*Per my last memo.*

AE2 autocrafting needs **compute**: someone has to work out each crafting plan, file it while it runs, and hand its
tasks out.  This mod makes you build that someone.  Each facility is a crafting CPU on your ME network.

## The three capacities

| Capacity | What it limits | Comes from |
| :--- | :--- | :--- |
| **Planning** | How fast plans are calculated, in steps per second | Workers' time, and paper |
| **Filing** | The largest plan a job slot can hold, in bytes | Books, tape reels, disk arrays |
| **Dispatch** | How many tasks a running job hands out, in operations per second | Workers' time |

Planning is pooled across your network; Filing and Dispatch belong to the facility whose slot runs the job.  A worker
plans while its slots are idle and dispatches while they are busy.

### Plans take time now

On the craft confirm screen, while the plan is still calculating, **Start** reads **Queue**.  Press it and go back to
your terminal: the plan keeps calculating, is listed in the crafting status screen as *Planning* (hover it for the
steps, the rate and why it is slow), and starts by itself on a free CPU when it is ready.  If no CPU is free it waits as
*Waiting for CPU*.  Chat tells you when it starts.  Shift-clicking *Next* queues at once.  A queued plan can be
cancelled from the crafting status screen.

Hover a busy CPU there to see its dispatch rate, and the rate it would need to start every ready task without delay.

Shortages only ever slow things down.  Nothing is lost, miscrafted or cancelled because of compute.

## The Desk (HV)

*Sole proprietorship.  You are the IT department.*

A **Requisition Tray** on ME cable, with within 2 blocks a BiblioCraft **seat**, a BiblioCraft **desk** next to the
seat, and a **typewriter** within a block of the seat (no typewriter, no planning).  A **desk bell** adds a quarter to
dispatch.  Bookcases within 3 blocks are its Filing: 64 bytes per book, 192 per vanilla bookshelf, up to 8 k.  Paper
goes in the tray (planning runs at a quarter without it), emeralds pay wages.

Sit on the seat yourself and you are the worker (*founder mode*).  Or hire one:

1. Right-click a villager with an **Employment Contract**.  You get a **Signed Contract**: their file.
2. Right-click the seat with the Signed Contract.  The villager sits down and stays there.
3. A **Pink Slip** lets them go.  We wish them well.

Librarians are better at planning, farmers at dispatch, priests at both.

## Morale

One number per facility, 0 to 100, drifting a point a minute towards a target.  Output is 25 % at 0, 100 % at 60 and
120 % at 100.  The target starts at 50: +10 paid (−40 unpaid), amenities within 4 blocks of the seat up to +40 (light,
a clock, a potted plant, a window with sky behind it, posters and paintings), +10 for coffee from a **Break Room
Counter** within 16 blocks (cocoa beans count), +20 for a day after a party (put a cake or a pizza in the counter),
−10 if crowded, smog, and the managers' penalties.

**Crunch**: a redstone signal on the tray or controller.  Output ×1.5, morale target −30.  Worth it for about 25
minutes.

## The Mainframe (EV)

*Nobody ever got fired for buying one.*  A 7×4×3 GregTech multiblock: Mainframe Casing, a row of Tape Reel Windows,
four OpenComputers **racks** beside the controller (their servers are its hardware capacity: 8/16/32 FTE per tier 1/2/3
server), and a **Terminal Concentrator** in the middle of the back.  Hatches anywhere: energy (EV), maintenance, input
bus (tape reels for Filing, paper or punch cards, emeralds, ender pearls and gold for managers, blank data tapes) and an
output bus (Work Logs).

The **office** is wherever you like: OC screens wired to the concentrator by OC cable, each one a workstation: a
seat, an OC **keyboard** beside it, and the screen right behind the keyboard (at its height or one up), with a
contracted worker on the seat.  A worker by a screen with no keyboard in front counts for nothing.  The concentrator
powers the screens.  Up to 32.  Seated workers face their workstation: the typewriter, the keyboard, or the desk.

### Managers

Unmanaged people do not cooperate: a facility's FTE is the sum of its unmanaged units over the square root of their
number.  A manager sits at a seat with a BiblioCraft desk and bell next to it, hired with a **Management Contract**.

| Role | Who | Efficiency | Upkeep |
| :--- | :--- | :--- | :--- |
| Team lead (8 workers within 12 blocks) | Enderman, Crimson Cleric, tamed demon | 0.8 / 0.9 / 1.0 | pearl, gold, 100 LP a minute |
| Director (8 leads within 24 blocks) | **Bosses**: a Wither, the Twilight Forest's bosses | 0.95 | a diamond a day |
| Executive (Internet boardroom) | Tamed demon | 1.0 | 100 LP a minute |

Unpaid managers work at half efficiency.  Keep Endermen under a roof: they teleport in daylight until seated.

## The Internet (IV)

*It's Web Scale!™*  A 5×5×9 tower of Tube Casing with steel frame corners, a Backbone Core up the middle and a
Satellite Dish on top that must see the sky.  Put **Router Ports** in its walls and run OC cable from each to a
Mainframe's terminal network: each port links one Mainframe.  Linked Mainframes plan as one, pool their dispatch, and
the Internet gets one job slot per Mainframe.  Without a demon in the **boardroom** (a manager's office within 6
blocks of its controller), one job may only use one Mainframe's Filing.  Disk Arrays in its input bus add Filing.

**Gig Kiosks** within 32 blocks enrol the uncontracted villagers near them: half an FTE of dispatch each, about half
the time, paid per operation from emeralds in the input bus.  They agreed to the Terms of Service by standing there.

**Robots**: on a linked Mainframe, an OpenComputers robot standing on a **Docking Pad** (on its terminal network), with
a **Requisition Upgrade**, clocks in by calling `component.requisition.work()`.  The `workerd` loot floppy does it for
you.  A Mainframe's robots work as one botnet at half their worth, plus a little for each villager seated by a pad as a
CAPTCHA solver.

## The AI Data Center (LuV)

*We are pleased to announce a reduction in headcount.*  A hall of Data Hall Casing 15 wide and 7 high, 7 to 31 long,
with three rows of **Accelerator Racks** per slice over a Raised Floor.  Its Accelerator Cards are its synthetic FTE: no
staff, no wages, no morale.  8 slots, 64 M of Filing.  It needs 1000 L/s of water (distilled counts double) or throttles,
pollutes while busy (it needs a muffler), and must be **trained** on Work Logs from your offices: 25 % output untrained,
100 % fully trained, drifting back to 60 % unless fed.  It sometimes re-verifies its plans; the plan is always right.
Trained, it makes **Converged Model Weights**.

## Legacy crafting CPUs (ZPM)

Stock crafting CPU blocks now need a ZPM assembler.  Blocks placed from now on count for a quarter, capped at 64
co-processors a cluster.  Blocks already in your world are *legacy*: they keep their full value, and a network with
one plans unmetered.  `/ab legacy` lists them.

## The Singularity (UV)

*It is not a product.*  A 17-block frame: an octagonal ring at the base and the top, four Containment Pylons, and an
empty 11-block cube in the middle, where it appears.  64 slots, unlimited Filing, unmetered planning, 20 480 operations
a second, all scaled by its **Alignment**.  Every few days it wants something: paperclips (in its input bus; it wants
twice as many each time), to draw extra power or take a sabbatical (signal its controller to approve), something you
have plenty of, or a villager to study (bring a contracted one, off shift, within 4 blocks; they come back).  Grant it
and Alignment rises; ignore it and it sulks.  It never takes anything you do not give it.

## Commands

* `/ab status [x y z]`: the nearest facility's bottleneck and figures, and its network's planning.
* `/ab craft <item> [count] [meta]`: plan and start a craft, as the Queue button does.
* `/ab legacy`: where the legacy crafting CPU blocks are.

## OpenComputers

Put an Adapter next to a facility's tray or controller.  Component `ab_facility`: `status()`, `planningRate()`,
`setAllocation(share)` (pin the planning share, 0 to 1, or nil), `setCrunch(bool)` (or nil to follow redstone).  Keep
dashboards on a separate OC network from the office's screens, joined only through the Adapter: an office has more
components than a computer can hold.

## Removing the mod

Cancel the jobs on Applied Bureaucratics facilities first.  A running job is saved with its facility's block, so
removing the mod removes its items.  Stock CPUs are unaffected.
