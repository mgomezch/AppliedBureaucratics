package com.gtnewhorizons.appliedbureaucratics.gui.founder;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.gtnewhorizons.appliedbureaucratics.AppliedBureaucratics;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// How the founder's puzzles look, drawn in a `w` by `h` box at the origin. See Note [Founder mode].
@SideOnly(Side.CLIENT)
final class PuzzleRenderer {

    private static final ResourceLocation[] SERIF = new ResourceLocation[10];

    static {
        for (int d = 1; d <= 9; d++) {
            SERIF[d] = new ResourceLocation(AppliedBureaucratics.MODID, "textures/gui/serif_" + d + ".png");
        }
    }

    private PuzzleRenderer() {}

    static void draw(final Puzzle puzzle, final int w, final int h, final long millis) {
        if (puzzle instanceof Puzzle.Sum sum) {
            sum(sum, w, h);
        } else if (puzzle instanceof Puzzle.Captcha captcha) {
            captcha(captcha, w, h);
        } else if (puzzle instanceof Puzzle.Spooky spooky) {
            spooky(spooky, w, h, millis);
        } else if (puzzle instanceof Puzzle.Sudoku sudoku) {
            sudoku(sudoku, w, h);
        }
    }

    /// A flash card: "3 + 4 = ?" on cream.
    private static void sum(final Puzzle.Sum sum, final int w, final int h) {
        GuiDraw.drawRect(0, 0, w, h, 0xFFF3EEDF);
        GuiDraw.drawRect(0, 0, w, 3, 0xFF3A6EA5);
        text(sum.a + " + " + sum.b + " = ?", w / 2f, h / 2f, 2.5f, 0xFF202020, 0);
    }

    /// Three tilted letters on a speckled grey, with two scratches through them, and what has been typed so far.
    private static void captcha(final Puzzle.Captcha captcha, final int w, final int h) {
        GuiDraw.drawRect(0, 0, w, h, 0xFFD8D8D2);
        final Random noise = new Random(captcha.seed);
        for (int i = 0; i < 140; i++) {
            final int x = noise.nextInt(w);
            final int y = noise.nextInt(h - 24);
            GuiDraw.drawRect(x, y, 1, 1, noise.nextBoolean() ? 0xFF9A9A90 : 0xFFB8B8AE);
        }
        final int[] inks = { 0xFF2B2B6E, 0xFF5A1E1E, 0xFF1E4A2B, 0xFF333333 };
        final float step = w / 4f;
        for (int i = 0; i < captcha.letters.length(); i++) {
            final float x = step * (i + 1) + (noise.nextFloat() - 0.5f) * 6;
            final float y = (h - 24) / 2f + (noise.nextFloat() - 0.5f) * 10;
            text(
                String.valueOf(captcha.letters.charAt(i)),
                x,
                y,
                3f,
                inks[noise.nextInt(inks.length)],
                captcha.tilts[i]);
        }
        for (int i = 0; i < 2; i++) {
            final float y = 10 + noise.nextFloat() * (h - 44);
            line(4, y, w - 4, y + (noise.nextFloat() - 0.5f) * 30, 0xFF555555);
        }
        GuiDraw.drawRect(0, h - 22, w, 22, 0xFFF4F4F0);
        final StringBuilder typed = new StringBuilder();
        for (int i = 0; i < captcha.letters.length(); i++) {
            typed.append(i < captcha.typed ? captcha.letters.charAt(i) : '_')
                .append(' ');
        }
        text(
            typed.toString()
                .trim(),
            w / 2f,
            h - 11,
            1.5f,
            0xFF202020,
            0);
    }

    /// Macrodata refinement: pale blue digits on the deep blue of a Lumon terminal, one of them fidgeting.
    private static void spooky(final Puzzle.Spooky spooky, final int w, final int h, final long millis) {
        GuiDraw.drawRect(0, 0, w, h, 0xFF06172A);
        final int c = Puzzle.Spooky.cell(w, h);
        final int x0 = (w - c * Puzzle.Spooky.SIZE) / 2;
        final int y0 = (h - c * Puzzle.Spooky.SIZE) / 2;
        for (int i = 0; i < spooky.digits.length; i++) {
            float x = x0 + (i % Puzzle.Spooky.SIZE) * c + c / 2f;
            float y = y0 + (i / Puzzle.Spooky.SIZE) * c + c / 2f;
            float scale = 1f;
            if (i == spooky.spooky) {
                x += (float) Math.sin(millis / 41.0) * 0.9f + (float) Math.sin(millis / 13.0) * 0.4f;
                y += (float) Math.cos(millis / 57.0) * 0.9f;
                scale = 1.1f;
            }
            text(String.valueOf(spooky.digits[i]), x, y, scale, 0xFF9BDCF0, 0);
        }
    }

    /// A newspaper's sudoku: black on white, thick border, digits in a serif.
    private static void sudoku(final Puzzle.Sudoku sudoku, final int w, final int h) {
        GuiDraw.drawRect(0, 0, w, h, 0xFFFFFFFF);
        final int c = (Math.min(w, h) - 8) / 3;
        final int x0 = (w - 3 * c) / 2;
        final int y0 = (h - 3 * c) / 2;
        for (int i = 0; i <= 3; i++) {
            final int t = i == 0 || i == 3 ? 2 : 1;
            GuiDraw.drawRect(x0 + i * c - t / 2f, y0 - 1, t, 3 * c + 2, 0xFF000000);
            GuiDraw.drawRect(x0 - 1, y0 + i * c - t / 2f, 3 * c + 2, t, 0xFF000000);
        }
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < 9; i++) {
            final int d = sudoku.cells[i];
            if (d == 0) {
                continue;
            }
            GL11.glColor4f(1, 1, 1, 1);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(SERIF[d]);
            final int pad = c / 8;
            GuiDraw.drawTexture(
                x0 + (i % 3) * c + pad,
                y0 + (i / 3) * c + pad,
                0,
                0,
                c - 2 * pad,
                c - 2 * pad,
                c - 2 * pad,
                c - 2 * pad);
        }
    }

    /// `s` centred on `x, y`, scaled and turned by `degrees`.
    private static void text(final String s, final float x, final float y, final float scale, final int color,
        final float degrees) {
        final FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glTranslatef(x, y, 0);
        if (degrees != 0) {
            GL11.glRotatef(degrees, 0, 0, 1);
        }
        GL11.glScalef(scale, scale, 1);
        font.drawString(s, -font.getStringWidth(s) / 2, -4, color);
        GL11.glPopMatrix();
    }

    /// A one-pixel line, as a thin rectangle turned about its start.
    private static void line(final float x1, final float y1, final float x2, final float y2, final int color) {
        final float dx = x2 - x1;
        final float dy = y2 - y1;
        GL11.glPushMatrix();
        GL11.glTranslatef(x1, y1, 0);
        GL11.glRotatef((float) Math.toDegrees(Math.atan2(dy, dx)), 0, 0, 1);
        GuiDraw.drawRect(0, 0, (float) Math.sqrt(dx * dx + dy * dy), 1, color);
        GL11.glPopMatrix();
    }
}
