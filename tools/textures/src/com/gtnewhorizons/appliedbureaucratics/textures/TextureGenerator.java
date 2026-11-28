package com.gtnewhorizons.appliedbureaucratics.textures;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

/*-
Note [Textures are drawn at build time] (generated)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
None of the mod's textures is a committed image.  Every one is drawn by this program, which the build runs (the
Gradle task generateTextures, in build.gradle.kts) into build/generated/textures; that directory is a resource
directory of the main source set, so the textures go into the jar, and into runClient, like any resource.

How each texture is made:

  pixel art       blocks and items, 16 by 16: rectangles and points of given colours on a Canvas, by the methods
                  below (casing, document, reel, ...)
  glyphs          the sudoku's digits for the founder's puzzles (textures/gui/serif_N.png), 64 by 64: rendered
                  with AWT from Noto Serif Display, in tools/textures/fonts with its SIL Open Font License
                  (OFL.txt).  The font is used only here, at build time; the jar has the rendered digits, not the
                  font.

The Snack Vending Machine's front is not ours: it is the GTNH Vending Machine's, loaded from that mod's own assets
at runtime (ABTextures).

To add or change a texture, change this program; nothing under src/main/resources/.../textures is kept.  The
program clears its output directory first, so a texture taken out of it leaves the jar too.
*/
/// Draws every texture of the mod. Run by the build: see Note [Textures are drawn at build time].
///
/// Arguments: the font file for the sudoku's digits, and the output directory (the root of `assets`).
public final class TextureGenerator {

    private static final String ASSETS = "assets/appliedbureaucratics/textures";

    private final File out;

    private TextureGenerator(final File out) {
        this.out = out;
    }

    public static void main(final String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: TextureGenerator <font.ttf> <output dir>");
        }
        final File out = new File(args[1]);
        deleteTree(out);
        final TextureGenerator gen = new TextureGenerator(new File(out, ASSETS));
        gen.blocks();
        gen.items();
        gen.serifDigits(new File(args[0]));
    }

    // Pixel art.

    /// A 16 by 16 picture, transparent until drawn on.
    static final class Canvas {

        final int[][] px = new int[16][16];

        Canvas() {}

        Canvas(final int fill) {
            this.rect(0, 0, 15, 15, fill);
        }

        /// Fills the rectangle from `x0, y0` to `x1, y1`, both corners included.
        Canvas rect(final int x0, final int y0, final int x1, final int y1, final int color) {
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    this.px[y][x] = color;
                }
            }
            return this;
        }

        Canvas point(final int x, final int y, final int color) {
            this.px[y][x] = color;
            return this;
        }

        Canvas frame(final int color) {
            return this.rect(0, 0, 15, 0, color)
                .rect(0, 15, 15, 15, color)
                .rect(0, 0, 0, 15, color)
                .rect(15, 0, 15, 15, color);
        }

        BufferedImage image() {
            final BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    img.setRGB(x, y, this.px[y][x]);
                }
            }
            return img;
        }
    }

    /// An opaque colour.
    static int rgb(final int r, final int g, final int b) {
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    static int hex(final int rrggbb) {
        return 0xFF000000 | rrggbb;
    }

    static final int STEEL = rgb(120, 124, 132);
    static final int STEEL_DARK = rgb(78, 80, 88);
    static final int STEEL_LIGHT = rgb(160, 164, 172);
    static final int PAPER = rgb(236, 232, 214);
    static final int PAPER_SHADE = rgb(205, 200, 180);
    static final int INK = rgb(40, 40, 52);
    static final int FLUIX = rgb(122, 70, 200);

    private void save(final String path, final BufferedImage image) throws IOException {
        final File file = new File(this.out, path + ".png");
        file.getParentFile()
            .mkdirs();
        ImageIO.write(image, "png", file);
    }

    private void save(final String path, final Canvas canvas) throws IOException {
        this.save(path, canvas.image());
    }

    static Canvas casing(final int base, final int dark, final int light) {
        return new Canvas(base).frame(dark)
            .rect(1, 1, 14, 1, light)
            .rect(1, 1, 1, 14, light);
    }

    private void blocks() throws IOException {
        final Map<String, Canvas> casings = new LinkedHashMap<>();
        final Canvas mainframe = casing(STEEL, STEEL_DARK, STEEL_LIGHT);
        for (final int[] r : new int[][] { { 3, 3 }, { 12, 3 }, { 3, 12 }, { 12, 12 } }) {
            mainframe.point(r[0], r[1], STEEL_DARK);
        }
        casings.put("mainframe", mainframe);
        final Canvas tapeWindow = casing(STEEL, STEEL_DARK, STEEL_LIGHT).rect(2, 4, 13, 11, rgb(30, 34, 40));
        for (final int cx : new int[] { 5, 10 }) {
            tapeWindow.rect(cx - 2, 6, cx + 2, 9, rgb(90, 70, 40))
                .point(cx, 7, rgb(200, 200, 200))
                .point(cx, 8, rgb(200, 200, 200));
        }
        casings.put("tape_window", tapeWindow);
        final Canvas tube = casing(rgb(90, 100, 110), rgb(60, 65, 70), rgb(130, 140, 150));
        for (final int x : new int[] { 3, 7, 11 }) {
            tube.rect(x, 1, x + 1, 14, rgb(170, 210, 230))
                .point(x, 5, rgb(255, 230, 120))
                .point(x + 1, 10, rgb(255, 230, 120));
        }
        casings.put("tube", tube);
        casings.put(
            "backbone",
            casing(rgb(50, 55, 60), rgb(30, 32, 36), rgb(80, 85, 90)).rect(6, 0, 9, 15, rgb(60, 200, 120))
                .rect(7, 0, 8, 15, rgb(180, 255, 210)));
        final Canvas dataHall = casing(rgb(200, 205, 210), rgb(150, 155, 160), rgb(230, 235, 240));
        for (final int y : new int[] { 4, 8, 12 }) {
            dataHall.rect(2, y, 13, y, rgb(50, 55, 60));
        }
        for (final int x : new int[] { 4, 8, 11 }) {
            dataHall.point(x, 6, rgb(80, 255, 80));
        }
        casings.put("data_hall", dataHall);
        casings.put(
            "raised_floor",
            casing(rgb(180, 180, 185), rgb(120, 120, 125), rgb(210, 210, 215)).rect(7, 0, 8, 15, rgb(70, 75, 80))
                .rect(0, 7, 15, 8, rgb(70, 75, 80)));
        final Canvas containment = casing(rgb(30, 25, 40), rgb(15, 10, 20), rgb(60, 50, 80))
            .rect(5, 0, 10, 15, rgb(40, 30, 60));
        for (int y = 1; y < 15; y += 3) {
            containment.rect(6, y, 9, y, rgb(150, 90, 255));
        }
        casings.put("containment", containment);
        casings.put(
            "dish",
            casing(rgb(180, 180, 185), rgb(120, 120, 125), rgb(220, 220, 225)).rect(3, 3, 12, 12, rgb(210, 210, 215))
                .rect(6, 6, 9, 9, rgb(120, 120, 130)));
        for (final Map.Entry<String, Canvas> e : casings.entrySet()) {
            this.save("blocks/casing_" + e.getKey(), e.getValue());
        }

        final Canvas concentrator = casing(STEEL, STEEL_DARK, STEEL_LIGHT).rect(4, 4, 11, 11, rgb(40, 40, 50));
        for (final int[] p : new int[][] { { 5, 5 }, { 10, 5 }, { 5, 10 }, { 10, 10 } }) {
            concentrator.point(p[0], p[1], rgb(90, 200, 255));
        }
        this.save("blocks/terminal_concentrator", concentrator.rect(7, 7, 8, 8, rgb(255, 200, 60)));

        final Canvas port = casing(rgb(90, 100, 110), rgb(60, 65, 70), rgb(130, 140, 150));
        for (final int x : new int[] { 4, 7, 10 }) {
            port.rect(x, 6, x + 1, 9, rgb(20, 20, 25))
                .point(x, 5, rgb(60, 255, 90));
        }
        this.save("blocks/router_port", port);
        this.save(
            "blocks/gig_kiosk",
            casing(rgb(230, 120, 40), rgb(170, 80, 20), rgb(255, 170, 90)).rect(3, 3, 12, 8, rgb(30, 30, 40))
                .rect(4, 4, 11, 5, rgb(255, 255, 255))
                .rect(6, 10, 9, 12, rgb(255, 230, 120)));
        this.save(
            "blocks/docking_pad",
            casing(rgb(70, 75, 85), rgb(40, 45, 55), rgb(110, 115, 125)).rect(3, 3, 12, 12, rgb(230, 200, 40))
                .rect(5, 5, 10, 10, rgb(40, 40, 45)));
        final Canvas rack = casing(rgb(40, 44, 50), rgb(20, 22, 26), rgb(70, 74, 80));
        for (final int y : new int[] { 3, 6, 9, 12 }) {
            rack.rect(3, y, 12, y + 1, rgb(25, 28, 32))
                .point(11, y, rgb(60, 255, 120));
        }
        this.save("blocks/accelerator_rack", rack);

        this.save("blocks/OVERLAY_FRONT_MAINFRAME", screenOverlay(false));
        this.save("blocks/OVERLAY_FRONT_MAINFRAME_ACTIVE", screenOverlay(true));
        this.save("blocks/OVERLAY_FRONT_WATER_COOLER", waterCooler(false));
        this.save("blocks/OVERLAY_FRONT_WATER_COOLER_ACTIVE", waterCooler(true));
        this.save("blocks/OVERLAY_FRONT_COFFEE_MACHINE", coffeeMachine(false));
        this.save("blocks/OVERLAY_FRONT_COFFEE_MACHINE_ACTIVE", coffeeMachine(true));

        this.save(
            "blocks/requisition_tray_side",
            trayStripes(
                new Canvas(STEEL).frame(STEEL_DARK)
                    .rect(2, 5, 13, 10, STEEL_DARK)).rect(1, 1, 14, 1, STEEL_LIGHT));
        final Canvas trayTop = new Canvas(STEEL).frame(STEEL_DARK)
            .rect(2, 2, 13, 13, STEEL_DARK)
            .rect(3, 3, 12, 12, PAPER_SHADE)
            .rect(4, 4, 11, 11, PAPER);
        for (final int y : new int[] { 6, 8, 10 }) {
            trayTop.rect(5, y, 10, y, INK);
        }
        this.save("blocks/requisition_tray_top", trayTop);
        this.save(
            "blocks/requisition_tray_bottom",
            new Canvas(STEEL_DARK).frame(rgb(50, 52, 58))
                .rect(6, 6, 9, 9, FLUIX));
    }

    private static Canvas trayStripes(final Canvas c) {
        for (int x = 3; x < 13; x += 2) {
            c.point(x, 7, FLUIX)
                .point(x + 1, 8, FLUIX);
        }
        return c;
    }

    /// A Mainframe's screen, lit with lines of text while it runs.
    static Canvas screenOverlay(final boolean active) {
        final Canvas c = new Canvas().rect(2, 2, 13, 13, rgb(20, 25, 20))
            .rect(3, 3, 12, 10, active ? rgb(20, 120, 30) : rgb(10, 40, 10));
        if (active) {
            for (final int y : new int[] { 4, 6, 8 }) {
                c.rect(4, y, 4 + (y * 3) % 8, y, rgb(120, 255, 120));
            }
        }
        return c.rect(4, 12, 11, 12, rgb(90, 90, 90));
    }

    /// A Water Cooler's front: a blue jug on a white body with a hot and a cold tap and a drip tray; the water
    /// brighter and a drip falling while it is open.
    static Canvas waterCooler(final boolean active) {
        final Canvas c = new Canvas().rect(5, 1, 10, 6, hex(0x2f6fb8))
            .rect(6, 2, 9, 5, hex(0x5aa0e6))
            .point(7, 2, hex(0x9fd0ff))
            .point(7, 3, hex(0x9fd0ff))
            .rect(7, 7, 8, 7, hex(0x1f4f88))
            .rect(4, 8, 11, 14, hex(0xe8e8e8))
            .rect(4, 14, 11, 14, hex(0xc8c8c8))
            .rect(5, 10, 6, 10, hex(0x3060d0))
            .rect(9, 10, 10, 10, hex(0xd03030))
            .rect(5, 13, 10, 13, hex(0x707070));
        if (active) {
            c.rect(6, 2, 9, 5, hex(0x7fc4ff))
                .point(7, 2, hex(0xe0f4ff))
                .point(7, 3, hex(0xe0f4ff))
                .point(8, 4, hex(0xe0f4ff))
                .point(5, 11, hex(0x5aa0e6))
                .point(5, 12, hex(0x5aa0e6));
        }
        return c;
    }

    /// A Coffee Machine's front: a dark body, a brewing head and a cup; coffee in the cup, a stream, and a green
    /// light while it is open.
    static Canvas coffeeMachine(final boolean active) {
        final Canvas c = new Canvas().rect(3, 1, 12, 14, hex(0x2a2a2a))
            .rect(4, 2, 11, 4, hex(0x444444))
            .rect(6, 5, 9, 6, hex(0x909090))
            .rect(4, 7, 11, 12, hex(0x151515))
            .rect(6, 9, 9, 12, hex(0xf2f2f2))
            .point(10, 10, hex(0xf2f2f2))
            .rect(4, 13, 11, 13, hex(0x808080))
            .point(10, 3, hex(0x802020));
        if (active) {
            c.rect(6, 9, 9, 9, hex(0x5a3210))
                .point(7, 7, hex(0x3a1e08))
                .point(7, 8, hex(0x3a1e08))
                .point(10, 3, hex(0x30ff60));
        }
        return c;
    }

    // Items.

    static Canvas document(final int base, final Integer seal, final Integer stripe, final boolean signed) {
        final Canvas c = new Canvas().rect(3, 1, 12, 14, PAPER_SHADE)
            .rect(3, 1, 11, 13, base);
        for (final int y : new int[] { 3, 5, 7, 9 }) {
            c.rect(5, y, 10, y, y != 9 ? INK : PAPER_SHADE);
        }
        if (stripe != null) {
            c.rect(3, 1, 11, 2, stripe);
        }
        if (signed) {
            for (int x = 5; x < 11; x++) {
                c.point(x, 11 + (x - 5) % 2, rgb(30, 60, 160));
            }
        }
        if (seal != null) {
            c.rect(9, 10, 11, 12, seal);
        }
        return c;
    }

    static Canvas reel(final int color, final int hub) {
        return new Canvas().rect(3, 3, 12, 12, color)
            .rect(2, 5, 13, 10, color)
            .rect(5, 2, 10, 13, color)
            .rect(6, 6, 9, 9, hub);
    }

    static Canvas smallItem(final int fill) {
        return new Canvas().rect(3, 3, 12, 12, fill);
    }

    private void items() throws IOException {
        this.save("items/requisition_upgrade", smallItem(rgb(40, 120, 60)).rect(5, 5, 10, 10, rgb(230, 200, 40)));
        this.save("items/tape_reel", reel(rgb(60, 50, 40), rgb(200, 200, 200)));
        this.save("items/high_density_tape_reel", reel(rgb(40, 40, 80), rgb(230, 200, 90)));
        final Canvas disks = smallItem(rgb(150, 150, 160));
        for (final int y : new int[] { 5, 8, 11 }) {
            disks.rect(4, y, 11, y, rgb(40, 40, 50));
        }
        this.save("items/disk_array", disks);
        final Canvas card = smallItem(rgb(230, 210, 150));
        for (int y = 4; y < 12; y++) {
            for (int x = 0; x < 16; x++) {
                if ((x + y) % 3 == 0) {
                    card.point(x, y, rgb(60, 50, 30));
                }
            }
        }
        this.save("items/punch_card", card);
        this.save("items/blank_data_tape", smallItem(rgb(50, 50, 55)).rect(5, 5, 10, 8, rgb(200, 200, 200)));
        this.save("items/work_log", smallItem(rgb(50, 50, 55)).rect(5, 5, 10, 8, rgb(90, 200, 90)));
        this.save("items/accelerator_card", smallItem(rgb(20, 90, 40)).rect(5, 5, 10, 10, rgb(30, 30, 30)));
        this.save("items/converged_model_weights", smallItem(rgb(120, 60, 200)).rect(6, 6, 9, 9, rgb(255, 255, 255)));
        final int wire = rgb(190, 190, 200);
        final Canvas clip = new Canvas();
        for (int y = 3; y < 13; y++) {
            clip.point(5, y, wire)
                .point(10, y, wire);
        }
        this.save(
            "items/paperclip",
            clip.rect(5, 3, 10, 3, wire)
                .rect(7, 12, 10, 12, wire));
        this.save("items/employment_contract", document(PAPER, rgb(40, 170, 70), null, false));
        this.save("items/signed_contract", document(PAPER, rgb(40, 170, 70), null, true));
        this.save("items/management_contract", document(PAPER, rgb(200, 160, 40), rgb(120, 30, 30), false));
        this.save("items/pink_slip", document(rgb(240, 170, 200), null, null, false));
    }

    // Glyphs.

    /// The digits 1 to 9 for the founder's sudoku: black, 58 px Noto Serif Display, centred in 64 by 64 and set
    /// 2 px low.
    private void serifDigits(final File fontFile) throws Exception {
        final Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile)
            .deriveFont(58f);
        for (int d = 1; d <= 9; d++) {
            final BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            final FontRenderContext frc = g.getFontRenderContext();
            final GlyphVector glyphs = font.createGlyphVector(frc, String.valueOf(d));
            final Rectangle2D box = glyphs.getVisualBounds();
            final float x = (float) (32 - box.getCenterX());
            final float y = (float) (32 - box.getCenterY() + 2);
            g.setColor(Color.BLACK);
            g.drawGlyphVector(glyphs, x, y);
            g.dispose();
            this.save("gui/serif_" + d, img);
        }
    }

    private static void deleteTree(final File f) throws IOException {
        final File[] children = f.listFiles();
        if (children != null) {
            for (final File c : children) {
                deleteTree(c);
            }
        }
        if (f.exists() && !f.delete()) {
            throw new IOException("Cannot delete " + f);
        }
    }
}
