package com.gtnewhorizons.appliedbureaucratics.compute;

/// What a facility owes its workers: one wage unit per worker per day of uptime, paid from emeralds worth
/// [ComputeSettings#wageUnitsPerEmerald] units each. See Note [The compute model].
///
/// Call [#accrue] for each tick the facility works, then [#unitsDue] and [#pay] to settle with the emeralds in its
/// payroll. A facility is unpaid while it owes a whole unit it could not pay.
public final class Payroll {

    /// Worker-ticks worked and not yet turned into owed units.
    private long workerTicks;
    /// Wage units owed and not paid.
    private long owed;
    /// Wage units already paid for, from the part of the last emerald not yet used.
    private long prepaid;

    public void accrue(final int workers, final long ticks) {
        if (workers <= 0 || ticks <= 0) {
            return;
        }
        this.workerTicks += (long) workers * ticks;
        final int day = ComputeSettings.get().ticksPerDay;
        this.owed += this.workerTicks / day;
        this.workerTicks %= day;
        final long fromPrepaid = Math.min(this.owed, this.prepaid);
        this.owed -= fromPrepaid;
        this.prepaid -= fromPrepaid;
    }

    /// How many emeralds settle what is owed.
    public long emeraldsDue() {
        final int perEmerald = ComputeSettings.get().wageUnitsPerEmerald;
        return (this.owed + perEmerald - 1) / perEmerald;
    }

    /// Pays with `emeralds`, and keeps what is left of the last one for later wages.
    public void pay(final long emeralds) {
        if (emeralds <= 0) {
            return;
        }
        this.prepaid += emeralds * ComputeSettings.get().wageUnitsPerEmerald;
        final long fromPrepaid = Math.min(this.owed, this.prepaid);
        this.owed -= fromPrepaid;
        this.prepaid -= fromPrepaid;
    }

    public boolean isPaid() {
        return this.owed == 0;
    }

    public long getOwed() {
        return this.owed;
    }

    public long getPrepaid() {
        return this.prepaid;
    }

    public long getWorkerTicks() {
        return this.workerTicks;
    }

    public void load(final long workerTicks, final long owed, final long prepaid) {
        this.workerTicks = workerTicks;
        this.owed = owed;
        this.prepaid = prepaid;
    }
}
