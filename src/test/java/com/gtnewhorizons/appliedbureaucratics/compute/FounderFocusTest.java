package com.gtnewhorizons.appliedbureaucratics.compute;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/// A founder works only while focused: 4 s a puzzle, up to 10 s ahead, no faster than a puzzle every 8 ticks.
class FounderFocusTest {

    @Test
    void unfocusedUntilTheFirstPuzzle() {
        assertFalse(new FounderFocus().isFocused(1000));
    }

    @Test
    void aPuzzleBuysFourSeconds() {
        final FounderFocus focus = new FounderFocus();
        assertTrue(focus.solve(1000));
        assertAll(
            () -> assertTrue(focus.isFocused(1079)),
            () -> assertFalse(focus.isFocused(1080)),
            () -> assertEquals(80, focus.remaining(1000)));
    }

    @Test
    void focusAddsUpToTenSeconds() {
        final FounderFocus focus = new FounderFocus();
        focus.solve(1000);
        focus.solve(1010);
        focus.solve(1020);
        assertEquals(200, focus.remaining(1020), "Three quick puzzles reach the cap");
    }

    @Test
    void tooQuickIsNotCounted() {
        final FounderFocus focus = new FounderFocus();
        focus.solve(1000);
        assertAll(
            () -> assertFalse(focus.solve(1007)),
            () -> assertEquals(1, focus.solved()),
            () -> assertTrue(focus.solve(1008)));
    }
}
