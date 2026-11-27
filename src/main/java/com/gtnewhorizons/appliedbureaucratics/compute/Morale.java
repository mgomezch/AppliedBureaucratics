package com.gtnewhorizons.appliedbureaucratics.compute;

/// A facility's morale: one number from 0 to 100 that drifts one point a minute towards a target, and scales
/// output from 25 % at 0 through 100 % at 60 to 120 % at 100. See Note [The compute model].
public final class Morale {

    public static final double START = 60;

    private double value = START;

    public double get() {
        return this.value;
    }

    public void set(final double value) {
        this.value = clamp(value);
    }

    /// Moves morale towards `target` by `minutes` points at most.
    public void drift(final double target, final double minutes) {
        final double goal = clamp(target);
        if (this.value < goal) {
            this.value = Math.min(goal, this.value + minutes);
        } else {
            this.value = Math.max(goal, this.value - minutes);
        }
    }

    /// The output multiplier at this morale.
    public double multiplier() {
        return multiplier(this.value);
    }

    public static double multiplier(final double morale) {
        final double m = clamp(morale);
        if (m <= 60) {
            return 0.25 + 0.75 * m / 60;
        }
        return 1 + 0.2 * (m - 60) / 40;
    }

    private static double clamp(final double v) {
        return Math.max(0, Math.min(100, v));
    }
}
