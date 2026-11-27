package com.gtnewhorizons.appliedbureaucratics.compute;

/// A Data Center's training level T, from 0 to 1 (design document, section 6.4). Work Logs raise it, 1/1024 each;
/// it drifts down 2 % a day to a minimum of 0.6 unless fed ("data drift"). Output scales from 25 % at T = 0 to 100 %
/// at T = 1, and a plan calculated mostly by a Data Center is re-verified with probability 20 % × (1 − T).
public final class Training {

    public static final double LOGS_TO_TRAIN = 1024;
    public static final double DRIFT_PER_DAY = 0.02;
    public static final double DRIFT_FLOOR = 0.6;

    private double level;

    public double get() {
        return this.level;
    }

    public void set(final double level) {
        this.level = Math.max(0, Math.min(1, level));
    }

    public void feed(final int logs) {
        this.set(this.level + logs / LOGS_TO_TRAIN);
    }

    /// A day passes: the level drifts down towards the floor, never below it, and never up to it.
    public void day() {
        if (this.level > DRIFT_FLOOR) {
            this.level = Math.max(DRIFT_FLOOR, this.level - DRIFT_PER_DAY);
        }
    }

    public double outputFactor() {
        return 0.25 + 0.75 * this.level;
    }

    public double hallucinationChance() {
        return 0.2 * (1 - this.level);
    }
}
