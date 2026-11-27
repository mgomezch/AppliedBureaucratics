# Applied Bureaucratics

A satirical add-on for GregTech: New Horizons in which AE2 autocrafting needs **compute**: someone has to work out each
crafting plan, file it while it runs, and hand its tasks out.  You build that someone, one stage per voltage tier:

| Tier | Facility |
| :--- | :--- |
| HV | The Desk: a contracted villager at a BiblioCraft desk |
| EV | The Mainframe: a GregTech multiblock with OpenComputers racks, and an office of villagers at OC terminals, with managers |
| IV | The Internet: links Mainframes, runs several jobs at once, takes OC robots and gig workers |
| LuV | The AI Data Center: no staff, a lot of water and pollution, trained on the offices' work logs |
| ZPM | Stock AE2 crafting CPUs, weakened (blocks already in a world keep their full value) |
| UV | The Singularity |

Shortages only ever slow crafting down; nothing is lost or miscrafted.  See [the manual](docs/manual.md).

It is not part of the GTNH project.  The code was written by an AI coding agent (Claude Code).

## Requirements

- GTNH 2.10 dailies (Applied-Energistics-2-NH), with BiblioCraft and OpenComputers.
- A build of Applied-Energistics-2-NH with the compute policy hooks: branch `feature/compute-policy` of
  [mgomezch/Applied-Energistics-2-NH](https://github.com/mgomezch/Applied-Energistics-2-NH).  Stock AE2-NH does not have
  them.

## Build

Publish the AE2-NH fork to your local Maven repository, then build this mod:

```bash
cd ../Applied-Energistics-2-NH && VERSION=1.0.14-compute ./gradlew publishToMavenLocal
cd ../AppliedBureaucratics && ./gradlew build
```

Unit tests run with `./gradlew test`; the in-world tests in `inworld-tests/` need the nh-dev workspace's test harness.
