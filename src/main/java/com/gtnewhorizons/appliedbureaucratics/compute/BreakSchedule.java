package com.gtnewhorizons.appliedbureaucratics.compute;

import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;

/// How often a worker takes a break, and for how long, in ticks. See Note [Break rooms].
///
/// A managed worker takes [ABConfig#managedBreakSeconds] every [ABConfig#managedBreakMinutes], whatever the break
/// room is like. An unmanaged one takes [ABConfig#breakSeconds] every [ABConfig#breakMinutes], and a better kitchen
/// makes both breaks more frequent and longer, by up to [ABConfig#breakQualityStretch] (a quarter) at the best one.
public record BreakSchedule(long intervalTicks, long durationTicks) {

    /// @param managed whether the worker's facility has a paid manager
    /// @param quality the break room's kitchen quality, 0 to 1
    public static BreakSchedule of(final boolean managed, final double quality) {
        if (managed) {
            return new BreakSchedule(
                Math.round(ABConfig.managedBreakMinutes * 1200),
                Math.round(ABConfig.managedBreakSeconds * 20));
        }
        final double more = 1 + ABConfig.breakQualityStretch * Math.max(0, Math.min(1, quality));
        return new BreakSchedule(
            Math.round(ABConfig.breakMinutes * 1200 / more),
            Math.round(ABConfig.breakSeconds * 20 * more));
    }
}
