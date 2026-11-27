package com.gtnewhorizons.appliedbureaucratics.compute;

/*-
Note [The compute model] (generated)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Topic.  Applied Bureaucratics makes AE2 autocrafting need compute capacity, supplied by in-world facilities.  AE2
asks for it in three places, through the hooks on our AE2NH fork: planning a job (steps of the plan calculator),
holding a plan (bytes, called Filing here), and dispatching its tasks (operations, one pattern pushed to one
medium).  This package is the model that turns a facility's workers and furniture into those three numbers.  It
has no Minecraft types, so it is unit-tested directly (ComputeModelTest and friends).

Design.  A facility's output is measured in full-time equivalents (FTE).

    workers ──OrgUnit──▶ facility FTE ──Workforce.split──▶ planning FTE, dispatch FTE
                                         × soft factors (morale, paper, crunch), never below the floor
                         planning FTE × kPlan  = steps/s   ──▶ the network's PlanningPool
                         dispatch FTE × kDisp  = ops/s     ──▶ the facility's DispatchMeter, per job slot

Filing is a byte count from storage media, held by the facility and checked only when a job is submitted.

Moving parts.
  ComputeSettings  the constants, all config values; defaults are the design document's (section 4).
  OrgUnit          a worker, or a manager over up to 8 reports.  Unmanaged roots cooperate badly:
                   a facility's FTE is the sum of its roots' FTE over the square root of their number.
  Workforce        splits a facility's FTE between planning and dispatch by a fraction, affine workers first.
  Morale           one number per facility, 0 to 100, drifting one point a minute towards a target.
  Payroll          wage units owed per worker per day of uptime, paid in emeralds.
  PlanningPool     a network's planning steps: credit accrues at the network's rate, plans draw on it, overtime
                   puts it into debt.
  DispatchMeter    turns an ops/s rate into whole operations per tick with a carried fraction.

Wrinkles.
  (W1) Slowdown only.  No soft factor can push output below ComputeSettings.floor (25 %).  A facility with
       no workers, power or structure is off instead, which is the ordinary GregTech rule, and its jobs wait.
  (W2) Players first.  When players and machines both have plans in progress on a network, half of each
       tick's steps are reserved for the players' plans (PlanningPool.allocate), so level maintainers cannot
       starve the person at the terminal.
  (W3) Standing orders.  A request that repeats one of the network's last 64 (same output, same requester)
       is charged only standingOrderCharge (10 %) of its steps, so level-maintained bases stay playable.  The
       calculation still runs in full; only the charge is discounted.
  (W4) Overtime.  AE2 callers that demand a plan at once (Future.get(), the lite planner) get it, and the
       steps are charged as debt, which the network pays off before any plan advances again.
  (W5) Committees.  Independent staffed facilities on one network plan by committee: their summed rate is
       divided by the square root of their number.  Machine facilities add without penalty.
*/

/// The constants of the compute model. All are config values; see Note [The compute model].
public final class ComputeSettings {

    /// Planning steps per second per FTE.
    public double kPlan = 4;
    /// Dispatch operations per second per FTE.
    public double kDisp = 1;
    /// The lowest fraction of its output a facility's soft requirements can leave it. See wrinkle (W1).
    public double floor = 0.25;
    /// Planning output without paper.
    public double noPaperFactor = 0.25;
    /// Planning steps per sheet of paper.
    public int stepsPerSheet = 256;
    /// Multiplier for a worker counted in its affine department.
    public double affinityBonus = 1.5;
    /// Multiplier for a worker with affinity for either department.
    public double eitherAffinityBonus = 1.25;
    /// The fraction of a network's planning steps reserved for players' plans. See wrinkle (W2).
    public double playerReservation = 0.5;
    /// How many recent requests a network remembers, and what a repeat is charged. See wrinkle (W3).
    public int standingOrderMemory = 64;
    public double standingOrderCharge = 0.1;
    /// How many seconds of its rate a network may bank as planning credit.
    public double creditSeconds = 1;
    /// Ticks per Minecraft day, the wage period.
    public int ticksPerDay = 24000;
    /// Wage units per emerald.
    public int wageUnitsPerEmerald = 8;
    /// Output multiplier while crunching, and the morale penalty for it.
    public double crunchFactor = 1.5;
    /// The direct reports a manager may have.
    public int maxReports = 8;

    private static ComputeSettings current = new ComputeSettings();

    public static ComputeSettings get() {
        return current;
    }

    public static void set(final ComputeSettings settings) {
        current = settings;
    }
}
