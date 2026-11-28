package com.gtnewhorizons.appliedbureaucratics.gui.founder;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.Widget;
import com.gtnewhorizons.appliedbureaucratics.compute.FounderFocus;
import com.gtnewhorizons.appliedbureaucratics.facility.desk.TileRequisitionTray;

/*-
Note [Founder mode] (generated)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
A player sitting on a Desk's seat is its worker, "the founder", and has to work for it: the Desk plans and
dispatches only while its founder is focused, and focus is bought by solving puzzles in a GUI.

Each puzzle is solvable in about two seconds without thinking (Puzzle):

  Sum       two one-digit numbers whose sum is one digit: press it
  Captcha   three tilted letters on a noisy grey: type them
  Spooky    an 8 by 8 grid of digits on a Lumon blue, one of them fidgeting (Severance's macrodata refinement):
            click it
  Sudoku    a 3 by 3 grid, black on white in a serif, with one of 1 to 9 missing: press it

The next puzzle comes up the moment one is solved, never of the same kind twice running.  Around the puzzle,
FounderGui shows four motivational quotes (ab.founder.quote.*), new ones every three puzzles.

Focus is FounderFocus: each puzzle buys 4 s, up to 10 s ahead, and a puzzle solved within 8 ticks of the last does
not count.  So a founder clicking along keeps the Desk busy, and one who closes the GUI to go and do something
else stops it within ten seconds ("Limited by: the founder is not grinding").

Moving parts:

  TileRequisitionTray   holds the FounderFocus; opens the GUI (an MUI2 IGuiHolder, by TileEntityGuiFactory) when
                        a player sits down at its seat, and again when a seated founder right-clicks the typewriter
                        (WorkforceEvents.onRightClickBlock); founderSolved counts a solve, on the server.
  FounderGui            the panel: puzzle, quotes, tickets closed, focus bar.
  PuzzleWidget          one puzzle at a time, taking keys (passed down by the panel) and clicks.
  PuzzleRenderer        the looks.  Minecraft's font has no serif, so the sudoku's digits are textures
                        (textures/gui/serif_N.png), rendered from Noto Serif Display when the mod is built:
                        see Note [Textures are drawn at build time].

Puzzles live on the client, which sends the server one thing: a solve, as the C2S write of an IntSyncValue
("tickets") whose server setter calls TileRequisitionTray.founderSolved.  The minimum gap is what keeps a client
from buying focus faster than a person could.
*/
/// The founder's GUI. See Note [Founder mode].
public final class FounderGui {

    public static final int WIDTH = 320;
    public static final int HEIGHT = 180;
    /// How many quotes the lang file has: ab.founder.quote.1 to ab.founder.quote.QUOTES.
    public static final int QUOTES = 30;

    private FounderGui() {}

    public static ModularPanel build(final TileRequisitionTray tray, final PosGuiData data,
        final PanelSyncManager sync) {
        final EntityPlayer player = data.getPlayer();
        final IntSyncValue tickets = new IntSyncValue(
            null,
            null,
            tray::getTicketsClosed,
            v -> tray.founderSolved(player)).allowC2S();
        final IntSyncValue focus = new IntSyncValue(null, null, () -> (int) tray.getFocusTicks(), null);
        sync.syncValue("tickets", tickets);
        sync.syncValue("focus", focus);

        final Quotes quotes = new Quotes();
        final PuzzleWidget puzzle = new PuzzleWidget(() -> {
            // Only the write goes to the server; the count comes back from it. See Note [Founder mode].
            tickets.setIntValue(tickets.getIntValue() + 1, false, true);
            quotes.solved();
        });
        final ModularPanel panel = new ModularPanel("ab_founder") {

            @Override
            public boolean onKeyPressed(final char typedChar, final int keyCode) {
                return puzzle.key(typedChar) || super.onKeyPressed(typedChar, keyCode);
            }
        };
        panel.size(WIDTH, HEIGHT);
        panel.child(
            IKey.lang("ab.founder.title")
                .asWidget()
                .pos(8, 7));
        panel.child(
            IKey.dynamic(() -> StatCollector.translateToLocalFormatted("ab.founder.tickets", tickets.getIntValue()))
                .asWidget()
                .pos(WIDTH - 110, 7)
                .width(102));
        panel.child(
            puzzle.pos(100, 22)
                .size(120, 116));
        panel.child(
            IKey.dynamic(
                () -> StatCollector.translateToLocal(
                    puzzle.getPuzzle()
                        .instruction()))
                .asWidget()
                .pos(96, 143)
                .width(128));
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            panel.child(
                IKey.dynamic(() -> "§o" + StatCollector.translateToLocal("ab.founder.quote." + quotes.get(slot)))
                    .asWidget()
                    .pos(i < 2 ? 6 : WIDTH - 92, 24 + (i % 2) * 62)
                    .width(86));
        }
        panel.child(
            IKey.lang("ab.founder.focus")
                .asWidget()
                .pos(8, HEIGHT - 17));
        panel.child(
            new FocusBar(focus).pos(48, HEIGHT - 17)
                .size(WIDTH - 56, 8));
        return panel;
    }

    /// Four quotes, by number, new ones every three puzzles.
    static final class Quotes {

        private final Random random = new Random();
        private final int[] shown = new int[4];
        private int solved;

        Quotes() {
            this.shuffle();
        }

        int get(final int slot) {
            return this.shown[slot];
        }

        void solved() {
            if (++this.solved % 3 == 0) {
                this.shuffle();
            }
        }

        private void shuffle() {
            for (int i = 0; i < this.shown.length; i++) {
                int q;
                do {
                    q = 1 + this.random.nextInt(QUOTES);
                } while (this.taken(q, i));
                this.shown[i] = q;
            }
        }

        private boolean taken(final int q, final int upTo) {
            for (int i = 0; i < upTo; i++) {
                if (this.shown[i] == q) {
                    return true;
                }
            }
            return false;
        }
    }

    /// How much focus is left, as a bar that empties.
    static final class FocusBar extends Widget<FocusBar> {

        private final IntSyncValue focus;

        FocusBar(final IntSyncValue focus) {
            this.focus = focus;
        }

        @Override
        public void draw(final ModularGuiContext context, final WidgetThemeEntry<?> widgetTheme) {
            final int w = this.getArea().width;
            final int h = this.getArea().height;
            GuiDraw.drawRect(0, 0, w, h, 0xFF303030);
            final float full = Math.min(1f, this.focus.getIntValue() / (float) FounderFocus.MAX);
            GuiDraw.drawRect(1, 1, (w - 2) * full, h - 2, full > 0.3f ? 0xFF3FB950 : 0xFFD29922);
        }
    }
}
