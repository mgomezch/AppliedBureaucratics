package com.gtnewhorizons.appliedbureaucratics.compute;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/// See Note [The compute model], wrinkles (W2) to (W4).
class PlanningPoolTest {

    private static final PlanningPool.Request PLAYER = new PlanningPool.Request(true, null);
    private static final PlanningPool.Request MACHINE = new PlanningPool.Request(false, null);

    @AfterEach
    void resetSettings() {
        ComputeSettings.set(new ComputeSettings());
    }

    /// Runs `ticks` ticks in which each plan takes its whole allowance, and returns the steps each took.
    private static long[] run(PlanningPool<String> pool, int ticks, String[] jobs, PlanningPool.Request[] requests) {
        final long[] taken = new long[jobs.length];
        for (int t = 0; t < ticks; t++) {
            pool.tick();
            for (int j = 0; j < jobs.length; j++) {
                final PlanningPool.Request request = requests[j];
                final long steps = pool.grant(jobs[j], () -> request);
                pool.report(jobs[j], () -> request, steps, false);
                taken[j] += steps;
            }
        }
        return taken;
    }

    @Test
    void aPlanGetsTheNetworksRate() {
        final PlanningPool<String> pool = new PlanningPool<>();
        pool.setRate(40);

        final long[] taken = run(pool, 101, new String[] { "a" }, new PlanningPool.Request[] { PLAYER });

        assertEquals(200, taken[0], 2, "40 steps/s over 5 s");
    }

    @Test
    void halfIsReservedForPlayers() {
        final PlanningPool<String> pool = new PlanningPool<>();
        pool.setRate(200);

        final long[] taken = run(
            pool,
            201,
            new String[] { "player", "m1", "m2", "m3" },
            new PlanningPool.Request[] { PLAYER, MACHINE, MACHINE, MACHINE });

        final long machines = taken[1] + taken[2] + taken[3];
        assertAll(
            () -> assertTrue(taken[0] >= 1000, "The player's plan gets at least half: " + taken[0]),
            () -> assertTrue(machines >= 800, "Three machines share the rest: " + machines),
            () -> assertEquals(2010, taken[0] + machines, 20, "All of 200 steps/s over 201 ticks"));
    }

    @Test
    void machinesGetEverythingWhenNoPlayerIsWaiting() {
        final PlanningPool<String> pool = new PlanningPool<>();
        pool.setRate(100);

        final long[] taken = run(pool, 101, new String[] { "m" }, new PlanningPool.Request[] { MACHINE });

        assertEquals(500, taken[0], 6);
    }

    @Test
    void aRepeatedRequestIsAStandingOrder() {
        final PlanningPool<String> pool = new PlanningPool<>();
        pool.setRate(20);
        final PlanningPool.Request refill = new PlanningPool.Request(false, "64 iron plates for the maintainer");
        run(pool, 21, new String[] { "first" }, new PlanningPool.Request[] { refill });
        pool.ended("first", true);

        final long[] taken = run(pool, 21, new String[] { "second" }, new PlanningPool.Request[] { refill });

        assertAll(
            () -> assertTrue(pool.isStandingOrder("second")),
            () -> assertEquals(200, taken[0], 20, "Ten times the steps for the same charge"));
    }

    @Test
    void overtimePutsTheNetworkInDebt() {
        final PlanningPool<String> pool = new PlanningPool<>();
        pool.setRate(20);
        pool.report("urgent", () -> PLAYER, 100, true);
        pool.ended("urgent", true);

        final long[] taken = run(pool, 100, new String[] { "next" }, new PlanningPool.Request[] { PLAYER });

        assertAll(
            () -> assertEquals(0, taken[0], "Nothing advances until 100 steps of debt are paid, 5 s at 20 steps/s"),
            () -> assertTrue(pool.getCredit() < 1));
    }

    @Test
    void aNetworkWithoutRateHasNoCapacity() {
        final PlanningPool<String> pool = new PlanningPool<>();

        assertFalse(pool.hasCapacity());
        pool.setUnmetered(true);
        assertAll(
            () -> assertTrue(pool.hasCapacity()),
            () -> assertEquals(Long.MAX_VALUE, pool.grant("x", () -> PLAYER)));
    }
}
