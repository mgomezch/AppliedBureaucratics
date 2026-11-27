package com.gtnewhorizons.appliedbureaucratics.compute;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/// Training and drift, design document section 6.4.
class TrainingTest {

    @Test
    void workLogsTrainTheModel() {
        final Training t = new Training();
        t.feed(512);
        assertAll(
            () -> assertEquals(0.5, t.get(), 1e-9),
            () -> assertEquals(0.625, t.outputFactor(), 1e-9),
            () -> assertEquals(0.1, t.hallucinationChance(), 1e-9));
        t.feed(10_000);
        assertEquals(1, t.get(), 1e-9);
    }

    @Test
    void driftStopsAtTheFloor() {
        final Training t = new Training();
        t.set(0.7);
        for (int i = 0; i < 10; i++) {
            t.day();
        }
        assertEquals(0.6, t.get(), 1e-9);
        t.set(0.3);
        t.day();
        assertEquals(0.3, t.get(), 1e-9, "An untrained model has nothing to forget");
    }
}
