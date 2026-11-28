package com.gtnewhorizons.appliedbureaucratics.workforce;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.integration.bibliocraft.Furniture;

/*-
Note [Facing the workstation] (generated)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
A worker on shift sits on a BiblioCraft seat and faces its workstation: the block beside the seat that it works at.
Workplace.facing finds it.  It looks at the four sides of the seat, at seat height and one above (a typewriter
stands on a desk), and takes the first kind it finds, in this order:

  1. a BiblioCraft typewriter: a Desk's clerk;
  2. an OpenComputers keyboard: a Mainframe's terminal operator;
  3. a BiblioCraft desk: a manager, or a clerk whose typewriter is elsewhere;
  4. an OpenComputers screen, for an office still being furnished.

The yaw is Minecraft's: 0 faces south (+z), 90 west, 180 north, 270 east.

EntityWorkstationMount holds the yaw and turns its rider to it every tick, on both sides.  The yaw is in the mount's
DataWatcher, so the client's copy of the mount, which is made from a spawn packet and never reads the mount's NBT,
turns the rider the same way as the server's.  The server looks for the workstation again every 5 s (Note
[Spatial checks are staggered]), so a seat furnished after its worker sat down, or refurnished, turns its worker
round.  A seat with no workstation keeps the
yaw it had: when the worker sat down, the way the player who seated it was looking.

A Mainframe terminal is a whole OC workstation, not just a screen: Workplace.atTerminal holds when the seat has an
OC keyboard beside it (at seat height or one above, on a desk) and an OC screen right behind the keyboard, at the
keyboard's height or one above.  In the office tests (test_mainframe.py, test_internet.py) the seat is at z 427,
the keyboard on the floor at z 426 and the screen at z 425, so the worker faces north, at the keyboard and screen.
MTEMainframe counts a worker at a screen only when its seat passes atTerminal.
*/
/// Where a seated worker works: which way it faces, and whether its seat is an OpenComputers workstation.
/// See Note [Facing the workstation].
public final class Workplace {

    /// The four sides of a seat, in yaw order: south, west, north, east.
    private static final int[][] SIDES = { { 0, 1 }, { -1, 0 }, { 0, -1 }, { 1, 0 } };
    private static final Furniture[] BY_PRIORITY = { Furniture.TYPEWRITER, Furniture.KEYBOARD, Furniture.DESK,
        Furniture.SCREEN };

    private Workplace() {}

    /// The yaw that faces the workstation beside the seat at `x, y, z`, or null if it has none.
    public static @Nullable Float facing(final World world, final int x, final int y, final int z) {
        for (final Furniture kind : BY_PRIORITY) {
            for (int i = 0; i < SIDES.length; i++) {
                for (int dy = 0; dy <= 1; dy++) {
                    if (kind.matches(world.getBlock(x + SIDES[i][0], y + dy, z + SIDES[i][1]))) {
                        return i * 90F;
                    }
                }
            }
        }
        return null;
    }

    /// Whether the seat at `x, y, z` has an OC keyboard beside it with a screen right behind the keyboard.
    public static boolean atTerminal(final World world, final int x, final int y, final int z) {
        for (final int[] side : SIDES) {
            for (int dy = 0; dy <= 1; dy++) {
                final int kx = x + side[0];
                final int ky = y + dy;
                final int kz = z + side[1];
                if (!Furniture.KEYBOARD.matches(world.getBlock(kx, ky, kz))) {
                    continue;
                }
                for (int sy = 0; sy <= 1; sy++) {
                    final Block behind = world.getBlock(kx + side[0], ky + sy, kz + side[1]);
                    if (Furniture.SCREEN.matches(behind)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
