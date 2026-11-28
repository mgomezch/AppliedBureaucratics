package com.gtnewhorizons.appliedbureaucratics.gui.founder;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gtnewhorizons.appliedbureaucratics.gui.founder.Puzzle.Outcome;

/// The founder's puzzles: what counts as solved, wrong, or nothing.
class PuzzleTest {

    @Test
    void sum() {
        final Puzzle.Sum sum = new Puzzle.Sum(3, 4);
        assertAll(
            () -> assertEquals(Outcome.SOLVED, sum.key('7')),
            () -> assertEquals(Outcome.WRONG, sum.key('6')),
            () -> assertEquals(Outcome.NONE, sum.key('x')));
    }

    @Test
    void sumsAreOneDigit() {
        final Random random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            final Puzzle.Sum sum = Puzzle.Sum.random(random);
            assertTrue(sum.a >= 1 && sum.b >= 1 && sum.a + sum.b <= 9, sum.a + " + " + sum.b);
        }
    }

    @Test
    void captchaTakesItsLettersInOrderAndStartsOverOnAMistake() {
        final Puzzle.Captcha captcha = new Puzzle.Captcha("ABC", new float[3], 0);
        assertAll(
            () -> assertEquals(Outcome.NONE, captcha.key('a')),
            () -> assertEquals(Outcome.WRONG, captcha.key('x')),
            () -> assertEquals(0, captcha.typed),
            () -> assertEquals(Outcome.NONE, captcha.key('1')),
            () -> assertEquals(Outcome.NONE, captcha.key('A')),
            () -> assertEquals(Outcome.NONE, captcha.key('b')),
            () -> assertEquals(Outcome.SOLVED, captcha.key('C')));
    }

    @Test
    void spookyIsClickedOnItsCell() {
        final Puzzle.Spooky spooky = new Puzzle.Spooky(new int[64], 9);
        // 96 by 96: cells of 12, so cell 9 (row 1, column 1) is 12..23 both ways.
        assertAll(
            () -> assertEquals(Outcome.SOLVED, spooky.click(15, 20, 96, 96)),
            () -> assertEquals(Outcome.WRONG, spooky.click(2, 2, 96, 96)),
            () -> assertEquals(Outcome.NONE, spooky.click(200, 2, 96, 96)),
            () -> assertEquals(9, Puzzle.Spooky.cellAt(15 + 12, 20, 120, 96)));
    }

    @Test
    void sudokuMissesOneDigit() {
        final Random random = new Random(2);
        for (int i = 0; i < 100; i++) {
            final Puzzle.Sudoku sudoku = Puzzle.Sudoku.random(random);
            final int[] sorted = sudoku.cells.clone();
            Arrays.sort(sorted);
            assertEquals(0, sorted[0]);
            assertNotEquals(0, sorted[1]);
            assertEquals(Outcome.SOLVED, sudoku.key((char) ('0' + sudoku.answer)));
        }
    }

    @Test
    void neverTheSameKindTwiceInARow() {
        final Random random = new Random(3);
        Puzzle last = null;
        for (int i = 0; i < 200; i++) {
            final Puzzle next = Puzzle.next(random, last);
            if (last != null) {
                assertNotEquals(last.getClass(), next.getClass());
            }
            last = next;
        }
    }
}
