package com.gtnewhorizons.appliedbureaucratics.gui.founder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.jspecify.annotations.Nullable;

/// A founder's busywork: four kinds of puzzle a player solves in about two seconds without thinking. The logic is
/// here, the looks in [PuzzleRenderer]. See Note [Founder mode].
public abstract class Puzzle {

    public enum Outcome {
        /// The input meant nothing to this puzzle, or was right but the puzzle is not done.
        NONE,
        SOLVED,
        WRONG
    }

    /// The lang key of the line under the puzzle that says what to do.
    public abstract String instruction();

    /// A key typed: `c` is the character, if any.
    public Outcome key(final char c) {
        return Outcome.NONE;
    }

    /// A click at `x, y` in a puzzle drawn `w` by `h`.
    public Outcome click(final int x, final int y, final int w, final int h) {
        return Outcome.NONE;
    }

    /// The next puzzle: any kind but the last one's.
    public static Puzzle next(final Random random, final @Nullable Puzzle last) {
        while (true) {
            final Puzzle p = switch (random.nextInt(4)) {
                case 0 -> Sum.random(random);
                case 1 -> Captcha.random(random);
                case 2 -> Spooky.random(random);
                default -> Sudoku.random(random);
            };
            if (last == null || p.getClass() != last.getClass()) {
                return p;
            }
        }
    }

    static int digit(final char c) {
        return c >= '0' && c <= '9' ? c - '0' : -1;
    }

    /// Two one-digit numbers to add, whose sum is one digit too: press it.
    public static final class Sum extends Puzzle {

        public final int a;
        public final int b;

        public Sum(final int a, final int b) {
            this.a = a;
            this.b = b;
        }

        static Sum random(final Random random) {
            final int a = 1 + random.nextInt(8);
            return new Sum(a, 1 + random.nextInt(9 - a));
        }

        @Override
        public String instruction() {
            return "ab.founder.do.sum";
        }

        @Override
        public Outcome key(final char c) {
            final int d = digit(c);
            return d < 0 ? Outcome.NONE : d == this.a + this.b ? Outcome.SOLVED : Outcome.WRONG;
        }
    }

    /// Three letters, a little wobbly: type them.
    public static final class Captcha extends Puzzle {

        /// No I, O or Q, which read as other letters.
        static final String LETTERS = "ABCDEFGHJKLMNPRSTUVWXYZ";

        public final String letters;
        /// Each letter's tilt in degrees, and the seed of its noise.
        public final float[] tilts;
        public final long seed;
        public int typed;

        public Captcha(final String letters, final float[] tilts, final long seed) {
            this.letters = letters;
            this.tilts = tilts;
            this.seed = seed;
        }

        static Captcha random(final Random random) {
            final StringBuilder s = new StringBuilder();
            final float[] tilts = new float[3];
            for (int i = 0; i < 3; i++) {
                s.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
                tilts[i] = -18 + 36 * random.nextFloat();
            }
            return new Captcha(s.toString(), tilts, random.nextLong());
        }

        @Override
        public String instruction() {
            return "ab.founder.do.captcha";
        }

        @Override
        public Outcome key(final char c) {
            if (!Character.isLetter(c)) {
                return Outcome.NONE;
            }
            if (Character.toUpperCase(c) != this.letters.charAt(this.typed)) {
                this.typed = 0;
                return Outcome.WRONG;
            }
            return ++this.typed == this.letters.length() ? Outcome.SOLVED : Outcome.NONE;
        }
    }

    /// An 8 by 8 grid of digits, one of which is scary and fidgets: click it.
    public static final class Spooky extends Puzzle {

        public static final int SIZE = 8;

        public final int[] digits;
        public final int spooky;

        public Spooky(final int[] digits, final int spooky) {
            this.digits = digits;
            this.spooky = spooky;
        }

        static Spooky random(final Random random) {
            final int[] digits = new int[SIZE * SIZE];
            for (int i = 0; i < digits.length; i++) {
                digits[i] = random.nextInt(10);
            }
            return new Spooky(digits, random.nextInt(digits.length));
        }

        @Override
        public String instruction() {
            return "ab.founder.do.spooky";
        }

        /// The side of a cell of the grid, drawn as a square in the middle of `w` by `h`.
        public static int cell(final int w, final int h) {
            return Math.min(w, h) / SIZE;
        }

        /// The cell at `x, y`, or -1.
        public static int cellAt(final int x, final int y, final int w, final int h) {
            final int c = cell(w, h);
            final int x0 = (w - c * SIZE) / 2;
            final int y0 = (h - c * SIZE) / 2;
            if (c <= 0 || x < x0 || y < y0 || x >= x0 + c * SIZE || y >= y0 + c * SIZE) {
                return -1;
            }
            return (y - y0) / c * SIZE + (x - x0) / c;
        }

        @Override
        public Outcome click(final int x, final int y, final int w, final int h) {
            final int at = cellAt(x, y, w, h);
            return at < 0 ? Outcome.NONE : at == this.spooky ? Outcome.SOLVED : Outcome.WRONG;
        }
    }

    /// A 3 by 3 sudoku with one digit missing: press it.
    public static final class Sudoku extends Puzzle {

        /// The digits by cell, row by row; 0 for the blank one.
        public final int[] cells;
        public final int answer;

        public Sudoku(final int[] cells, final int answer) {
            this.cells = cells;
            this.answer = answer;
        }

        static Sudoku random(final Random random) {
            final List<Integer> digits = new ArrayList<>();
            for (int d = 1; d <= 9; d++) {
                digits.add(d);
            }
            Collections.shuffle(digits, random);
            final int[] cells = new int[9];
            for (int i = 0; i < 9; i++) {
                cells[i] = digits.get(i);
            }
            final int blank = random.nextInt(9);
            final int answer = cells[blank];
            cells[blank] = 0;
            return new Sudoku(cells, answer);
        }

        @Override
        public String instruction() {
            return "ab.founder.do.sudoku";
        }

        @Override
        public Outcome key(final char c) {
            final int d = digit(c);
            return d < 0 ? Outcome.NONE : d == this.answer ? Outcome.SOLVED : Outcome.WRONG;
        }
    }
}
