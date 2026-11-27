package com.gtnewhorizons.appliedbureaucratics.compute;

import java.util.Random;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

/// The Singularity's Alignment (design document, section 6.6): one number from 0 to 100, starting at 50. Every 2 to
/// 4 days it opens a request with a day to answer; granting it adds 15, ignoring it takes 20. Output scales from 25 %
/// at 0 to 125 % at 100, and below 25 its planning is metered. It never takes anything: a request is only granted by
/// what the player delivers or approves. See Note [The Singularity].
public final class Alignment {

    public static final long DAY = 24000;
    public static final int PAPERCLIP_CAP = 24;
    public static final long SPELL = 12000;

    public enum Kind {
        PAPERCLIPS,
        POWER,
        ITEM,
        VILLAGER,
        SABBATICAL
    }

    /// An open request: what it is, how much of it, and until when.
    public record Request(Kind kind, long amount, long deadline) {}

    private double value = 50;
    private int paperclipExponent;
    private long nextRequestAt = -1;
    private @Nullable Request open;
    private long powerUntil;
    private long sabbaticalUntil;

    public double get() {
        return this.value;
    }

    public void set(final double value) {
        this.value = Math.max(0, Math.min(100, value));
    }

    public @Nullable Request getOpen() {
        return this.open;
    }

    public int getPaperclipExponent() {
        return this.paperclipExponent;
    }

    /// Advances to world time `now`: an overdue request is ignored, and a new one opens when due, of a kind
    /// `feasible` accepts, so that every request can be met.
    public void tick(final long now, final Random random, final Predicate<Kind> feasible) {
        if (this.nextRequestAt < 0) {
            this.scheduleNext(now, random);
        }
        if (this.open != null && now > this.open.deadline()) {
            this.open = null;
            this.set(this.value - 20);
            this.scheduleNext(now, random);
        }
        if (this.open == null && now >= this.nextRequestAt) {
            final Kind[] kinds = Kind.values();
            for (int attempt = 0; attempt < 10; attempt++) {
                final Kind kind = kinds[random.nextInt(kinds.length)];
                if (feasible.test(kind)) {
                    final long amount = kind == Kind.PAPERCLIPS ? 1L << this.paperclipExponent : 1;
                    this.open = new Request(kind, amount, now + DAY);
                    break;
                }
            }
            if (this.open == null) {
                this.scheduleNext(now, random);
            }
        }
    }

    private void scheduleNext(final long now, final Random random) {
        this.nextRequestAt = now + 2 * DAY + (long) (random.nextDouble() * 2 * DAY);
    }

    /// The open request is granted at world time `now`.
    public void grant(final long now, final Random random) {
        final Request request = this.open;
        if (request == null) {
            return;
        }
        this.open = null;
        this.set(this.value + 15);
        switch (request.kind()) {
            case PAPERCLIPS -> this.paperclipExponent = Math.min(PAPERCLIP_CAP, this.paperclipExponent + 1);
            case POWER -> this.powerUntil = now + SPELL;
            case SABBATICAL -> this.sabbaticalUntil = now + SPELL;
            default -> {}
        }
        this.scheduleNext(now, random);
    }

    /// The output multiplier at world time `now`: 25 % at 0 to 125 % at 100, halved on sabbatical.
    public double factor(final long now) {
        return (0.25 + this.value / 100) * (now < this.sabbaticalUntil ? 0.5 : 1);
    }

    public boolean plansUnmetered() {
        return this.value >= 25;
    }

    public int powerMultiplier(final long now) {
        return now < this.powerUntil ? 4 : 1;
    }

    public long getNextRequestAt() {
        return this.nextRequestAt;
    }

    public long getPowerUntil() {
        return this.powerUntil;
    }

    public long getSabbaticalUntil() {
        return this.sabbaticalUntil;
    }

    public void load(final double value, final int exponent, final long next, final @Nullable Request open,
        final long powerUntil, final long sabbaticalUntil) {
        this.set(value);
        this.paperclipExponent = exponent;
        this.nextRequestAt = next;
        this.open = open;
        this.powerUntil = powerUntil;
        this.sabbaticalUntil = sabbaticalUntil;
    }
}
