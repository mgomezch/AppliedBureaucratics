package com.gtnewhorizons.appliedbureaucratics.compute;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/// Breaks: 30 s every 5 min unmanaged, up to a quarter more of both with the best kitchen; 20 s every 7 min managed.
class BreakScheduleTest {

    @Test
    void unmanaged() {
        assertAll(
            () -> assertEquals(new BreakSchedule(6000, 600), BreakSchedule.of(false, 0), "A bare break room"),
            () -> assertEquals(new BreakSchedule(4800, 750), BreakSchedule.of(false, 1), "The best kitchen"),
            () -> assertEquals(new BreakSchedule(5333, 675), BreakSchedule.of(false, 0.5), "Halfway"),
            () -> assertEquals(new BreakSchedule(4800, 750), BreakSchedule.of(false, 7), "Quality is capped"));
    }

    @Test
    void managedBreaksIgnoreTheKitchen() {
        assertAll(
            () -> assertEquals(new BreakSchedule(8400, 400), BreakSchedule.of(true, 0)),
            () -> assertEquals(new BreakSchedule(8400, 400), BreakSchedule.of(true, 1)));
    }
}
