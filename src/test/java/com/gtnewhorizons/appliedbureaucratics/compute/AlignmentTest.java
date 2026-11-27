package com.gtnewhorizons.appliedbureaucratics.compute;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

/// The Singularity's Alignment, design document section 6.6.
class AlignmentTest {

    private static long openRequest(Alignment a, Random r, Alignment.Kind only, long from) {
        long t = from;
        while (a.getOpen() == null && t < from + 10 * Alignment.DAY) {
            t += 1200;
            a.tick(t, r, k -> k == only);
        }
        return t;
    }

    @Test
    void requestsComeEveryTwoToFourDays() {
        final Alignment a = new Alignment();
        final Random r = new Random(1);
        a.tick(0, r, k -> true);
        assertTrue(a.getNextRequestAt() >= 2 * Alignment.DAY && a.getNextRequestAt() <= 4 * Alignment.DAY);
    }

    @Test
    void grantingAddsFifteenAndPaperclipsDouble() {
        final Alignment a = new Alignment();
        final Random r = new Random(2);
        final long t = openRequest(a, r, Alignment.Kind.PAPERCLIPS, 0);
        assertEquals(
            1,
            a.getOpen()
                .amount());
        a.grant(t, r);
        final long t2 = openRequest(a, r, Alignment.Kind.PAPERCLIPS, t);
        assertAll(
            () -> assertEquals(65, a.get(), 1e-9),
            () -> assertEquals(
                2,
                a.getOpen()
                    .amount()));
    }

    @Test
    void ignoringTakesTwentyAndNeverTakesAnything() {
        final Alignment a = new Alignment();
        final Random r = new Random(3);
        final long t = openRequest(a, r, Alignment.Kind.ITEM, 0);
        assertNotNull(a.getOpen());
        a.tick(t + Alignment.DAY + 1, r, k -> false);
        assertAll(() -> assertEquals(30, a.get(), 1e-9), () -> assertNull(a.getOpen()));
    }

    @Test
    void outputAndMetering() {
        final Alignment a = new Alignment();
        a.set(0);
        assertAll(() -> assertEquals(0.25, a.factor(0), 1e-9), () -> assertFalse(a.plansUnmetered()));
        a.set(100);
        assertAll(() -> assertEquals(1.25, a.factor(0), 1e-9), () -> assertTrue(a.plansUnmetered()));
    }

    @Test
    void aSabbaticalHalvesOutputForTenMinutes() {
        final Alignment a = new Alignment();
        final Random r = new Random(4);
        final long t = openRequest(a, r, Alignment.Kind.SABBATICAL, 0);
        a.grant(t, r);
        assertAll(
            () -> assertEquals((0.25 + 0.65) * 0.5, a.factor(t + 1), 1e-9),
            () -> assertEquals(0.25 + 0.65, a.factor(t + Alignment.SPELL), 1e-9));
    }

    @Test
    void paperclipsStopDoublingAtTheCap() {
        final Alignment a = new Alignment();
        final Random r = new Random(5);
        long t = 0;
        for (int i = 0; i < 30; i++) {
            t = openRequest(a, r, Alignment.Kind.PAPERCLIPS, t);
            a.grant(t, r);
        }
        assertEquals(Alignment.PAPERCLIP_CAP, a.getPaperclipExponent());
    }
}
