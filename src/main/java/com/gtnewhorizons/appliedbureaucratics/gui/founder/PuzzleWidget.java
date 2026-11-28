package com.gtnewhorizons.appliedbureaucratics.gui.founder;

import java.util.Random;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;

/// The founder's puzzle: shows one, takes keys and clicks for it, and puts up the next as soon as it is solved. See
/// Note [Founder mode].
public final class PuzzleWidget extends Widget<PuzzleWidget> implements Interactable {

    private final Random random = new Random();
    private final Runnable onSolved;
    private Puzzle puzzle = Puzzle.next(this.random, null);
    /// Until when (wall clock) a wrong answer shows red.
    private long wrongUntil;

    public PuzzleWidget(final Runnable onSolved) {
        this.onSolved = onSolved;
    }

    public Puzzle getPuzzle() {
        return this.puzzle;
    }

    @Override
    public void draw(final ModularGuiContext context, final WidgetThemeEntry<?> widgetTheme) {
        final int w = this.getArea().width;
        final int h = this.getArea().height;
        PuzzleRenderer.draw(this.puzzle, w, h, System.currentTimeMillis());
        if (System.currentTimeMillis() < this.wrongUntil) {
            GuiDraw.drawRect(0, 0, w, h, 0x60E02020);
        }
    }

    @Override
    public Result onMousePressed(final int mouseButton) {
        this.handle(
            this.puzzle.click(
                this.getContext()
                    .getMouseX(),
                this.getContext()
                    .getMouseY(),
                this.getArea().width,
                this.getArea().height));
        return Result.SUCCESS;
    }

    /// A key typed anywhere in the GUI; true if the puzzle took it.
    public boolean key(final char c) {
        final Puzzle.Outcome outcome = this.puzzle.key(c);
        this.handle(outcome);
        return outcome != Puzzle.Outcome.NONE || Character.isLetterOrDigit(c);
    }

    private void handle(final Puzzle.Outcome outcome) {
        switch (outcome) {
            case SOLVED -> {
                this.onSolved.run();
                this.puzzle = Puzzle.next(this.random, this.puzzle);
            }
            case WRONG -> this.wrongUntil = System.currentTimeMillis() + 250;
            default -> {}
        }
    }
}
