package com.gtnewhorizons.appliedbureaucratics.util;

import java.util.concurrent.ThreadLocalRandom;

/*-
Note [Spatial checks are staggered] (generated)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Facilities and workers look around them now and then: a Desk for its seat and furniture, a Mainframe for its office
and managers, a seat for the workstation its worker faces.  Each such check runs on a Cadence of its own, not on a shared clock, and not every tick:

  small reach (a few blocks: a seat, a Desk's furniture, an office's terminals and amenities)    every 5 s
  large reach (24 blocks or more: a Mainframe's managers, a Gig Kiosk's villagers)               every 10 s

A Cadence counts down from a random point: the first check comes within its first two seconds (so a facility knows
its staff soon after a world loads), and every later one a whole period after the last.  Machines that load
together therefore check on different ticks, rather than all on the tick their chunks loaded, as they would with
`ticks % period == 0` on counters that all start at zero.
*/
/// A periodic check on a clock of its own. See Note [Spatial checks are staggered].
public final class Cadence {

    /// Ticks in 5 s: the period of checks of small reach.
    public static final int SMALL = 100;
    /// Ticks in 10 s: the period of checks of large reach.
    public static final int LARGE = 200;
    /// The first check comes within this many ticks.
    private static final int FIRST_WITHIN = 40;

    private final int period;
    private int countdown;

    public Cadence(final int period) {
        this.period = period;
        this.countdown = 1 + ThreadLocalRandom.current()
            .nextInt(Math.min(period, FIRST_WITHIN));
    }

    /// Counts one tick; true on the ticks the check is due. Call it once a tick.
    public boolean tick() {
        if (--this.countdown > 0) {
            return false;
        }
        this.countdown = this.period;
        return true;
    }

    /// Makes the next check come on the next tick.
    public void hurry() {
        this.countdown = 1;
    }
}
