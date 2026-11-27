package com.gtnewhorizons.appliedbureaucratics.compute;

/// Turns a dispatch rate in operations per second into whole operations per tick, carrying the fraction, so that
/// 1 op/s is one operation every 20 ticks. See Note [The compute model].
public final class DispatchMeter {

    private double carry;

    /// The operations granted this tick at `opsPerSecond`. Call once per tick.
    public int grant(final double opsPerSecond) {
        if (opsPerSecond <= 0) {
            this.carry = 0;
            return 0;
        }
        this.carry += opsPerSecond / 20;
        final int whole = (int) Math.min(Integer.MAX_VALUE, Math.floor(this.carry));
        this.carry -= whole;
        return whole;
    }

    /// Gives back operations granted and not used, up to one tick's worth, so a slot with nothing ready does not
    /// lose its share.
    public void refund(final int unused, final double opsPerSecond) {
        if (unused > 0) {
            this.carry = Math.min(this.carry + unused, Math.max(1, opsPerSecond / 20));
        }
    }
}
