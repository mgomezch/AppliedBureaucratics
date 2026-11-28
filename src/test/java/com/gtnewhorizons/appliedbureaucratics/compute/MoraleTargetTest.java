package com.gtnewhorizons.appliedbureaucratics.compute;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/// The morale table of the design document, section 7.3.
class MoraleTargetTest {

    private static double target(boolean paid, int amenities, int breakRoom, boolean party, boolean crowded,
        int pollution, int managers, boolean crunch) {
        return new MoraleTarget(paid, amenities, breakRoom, party, crowded, pollution, managers, crunch).value();
    }

    @Test
    void theTable() {
        assertAll(
            () -> assertEquals(60, target(true, 0, 0, false, false, 0, 0, false), "Base and paid"),
            () -> assertEquals(10, target(false, 0, 0, false, false, 0, 0, false), "Unpaid"),
            () -> assertEquals(100, target(true, 40, 10, false, false, 0, 0, false), "Amenities and a coffee machine"),
            () -> assertEquals(100, target(true, 99, 0, false, false, 0, 0, false), "Amenities are capped at 40"),
            () -> assertEquals(80, target(true, 0, 0, true, false, 0, 0, false), "A pizza party"),
            () -> assertEquals(50, target(true, 0, 0, false, true, 0, 0, false), "Crowding"),
            () -> assertEquals(40, target(true, 0, 0, false, false, 20, 0, false), "Smog"),
            () -> assertEquals(50, target(true, 0, 0, false, false, 0, 10, false), "A demon manager"),
            () -> assertEquals(30, target(true, 0, 0, false, false, 0, 0, true), "Crunch"),
            () -> assertEquals(0, target(false, 0, 0, false, true, 20, 10, true), "Never below 0"),
            () -> assertEquals(100, target(true, 0, 99, false, false, 0, 0, false), "Break rooms are capped at 40"));
    }

    @Test
    void pollutionPenaltyRisesFromTheSmogThreshold() {
        assertAll(
            () -> assertEquals(0, MoraleTarget.pollutionPenalty(999, 1000)),
            () -> assertEquals(5, MoraleTarget.pollutionPenalty(1000, 1000)),
            () -> assertEquals(20, MoraleTarget.pollutionPenalty(4000, 1000)),
            () -> assertEquals(20, MoraleTarget.pollutionPenalty(40000, 1000)));
    }

    @Test
    void humanResourcesHalvesTheDecay() {
        final Morale morale = new Morale();
        morale.drift(0, 10, 0.5);
        assertEquals(55, morale.get(), 1e-9);
    }

    @Test
    void crunchPaysForAboutTwentyFiveMinutes() {
        // Crunch: FTE x 1.5, morale target 30 from 60. Output is FTE x morale multiplier.
        final Morale morale = new Morale();
        int minutes = 0;
        while (1.5 * morale.multiplier() > 1.0 && minutes < 120) {
            morale.drift(30, 1);
            minutes++;
        }
        final int worthIt = minutes;
        assertAll(
            () -> assertEquals(0.9375, 1.5 * Morale.multiplier(30), 1e-9, "Crunching at morale 30 is a loss"),
            () -> assertEquals(true, worthIt >= 20 && worthIt <= 30, "Crunch pays for " + worthIt + " minutes"));
    }
}
