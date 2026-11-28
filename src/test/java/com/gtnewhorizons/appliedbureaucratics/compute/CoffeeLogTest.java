package com.gtnewhorizons.appliedbureaucratics.compute;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/// The coffee's bonus: 4 black, 2 more per extra on average, the average moving a fifth of the way a cup.
class CoffeeLogTest {

    @Test
    void blackCoffeeUntilTheFirstCup() {
        assertEquals(4, new CoffeeLog().bonus());
    }

    @Test
    void theFirstCupSetsTheAverage() {
        final CoffeeLog log = new CoffeeLog();
        log.record(1, 3);
        assertEquals(10, log.bonus());
    }

    @Test
    void laterCupsMoveItAFifthOfTheWay() {
        final CoffeeLog log = new CoffeeLog();
        log.record(1, 3);
        log.record(1, 0);
        assertAll(() -> assertEquals(2.4, log.averageExtras(), 1e-9), () -> assertEquals(9, log.bonus()));
    }

    @Test
    void severalCupsAtOnceCountOneByOne() {
        final CoffeeLog log = new CoffeeLog();
        log.record(2, 4);
        assertEquals(2.0, log.averageExtras(), 1e-9);
    }

    @Test
    void savedAndLoaded() {
        final CoffeeLog log = new CoffeeLog();
        log.record(1, 2);
        final CoffeeLog again = new CoffeeLog();
        again.load(log.save());
        final CoffeeLog fresh = new CoffeeLog();
        fresh.load(new CoffeeLog().save());
        fresh.record(1, 1);
        assertAll(
            () -> assertEquals(log.bonus(), again.bonus()),
            () -> assertEquals(1.0, fresh.averageExtras(), 1e-9, "A log never written takes its first cup whole"));
    }
}
