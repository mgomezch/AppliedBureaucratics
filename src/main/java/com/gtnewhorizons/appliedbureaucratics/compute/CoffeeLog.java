package com.gtnewhorizons.appliedbureaucratics.compute;

/// How good a facility's coffee has been lately: the cups its workers drank at a Coffee Machine, and how many of the
/// optional things (sugar, cocoa powder, milk) each had. Kept by the facility, not by the worker. See Note [Break
/// rooms].
///
/// The average is a moving one: each cup moves it a fifth of the way to its own extras, so a few good cups in a row
/// lift it and a change of recipe shows within the hour.
public final class CoffeeLog {

    public static final int MAX_EXTRAS = 3;
    /// The bonus for coffee with nothing in it, and per extra on average.
    public static final int PLAIN = 4;
    public static final int PER_EXTRA = 2;
    private static final double WEIGHT = 0.2;

    private double extras;
    private boolean any;

    /// Notes `cups` cups with `extras` extras between them.
    public void record(final int cups, final int extras) {
        for (int i = 0; i < cups; i++) {
            final double cup = Math.min(MAX_EXTRAS, (double) extras / cups);
            this.extras = this.any ? this.extras + WEIGHT * (cup - this.extras) : cup;
            this.any = true;
        }
    }

    public double averageExtras() {
        return this.extras;
    }

    /// The coffee's morale bonus: [#PLAIN] for black coffee, up to [#PLAIN] + 3 × [#PER_EXTRA] for the works.
    public int bonus() {
        return (int) Math.round(PLAIN + PER_EXTRA * this.extras);
    }

    public double save() {
        return this.any ? this.extras : -1;
    }

    public void load(final double saved) {
        this.any = saved >= 0;
        this.extras = Math.max(0, saved);
    }
}
