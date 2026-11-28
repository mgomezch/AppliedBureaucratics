package com.gtnewhorizons.appliedbureaucratics.compute;

/// How long a founder stays focused: each puzzle solved buys [#PER_SOLVE] ticks of work, up to [#MAX] ahead, and a
/// solve sooner than [#MIN_GAP] ticks after the last one is not counted. See Note [Founder mode].
public final class FounderFocus {

    public static final long PER_SOLVE = 80;
    public static final long MAX = 200;
    public static final long MIN_GAP = 8;

    private long until = Long.MIN_VALUE;
    private long lastSolve = Long.MIN_VALUE / 2;
    private int solved;

    /// Counts a puzzle solved at world time `now`; false if it came too soon after the last.
    public boolean solve(final long now) {
        if (now - this.lastSolve < MIN_GAP) {
            return false;
        }
        this.lastSolve = now;
        this.until = Math.min(now + MAX, Math.max(this.until, now) + PER_SOLVE);
        this.solved++;
        return true;
    }

    public boolean isFocused(final long now) {
        return now < this.until;
    }

    /// Ticks of focus left.
    public long remaining(final long now) {
        return Math.max(0, this.until - now);
    }

    /// Puzzles solved, ever: "tickets closed".
    public int solved() {
        return this.solved;
    }

    public void setSolved(final int solved) {
        this.solved = solved;
    }
}
