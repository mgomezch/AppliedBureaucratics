package com.gtnewhorizons.appliedbureaucratics.facility;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.gtnewhorizons.appliedbureaucratics.compute.MoraleTarget;
import com.gtnewhorizons.appliedbureaucratics.integration.bibliocraft.Furniture;

import gregtech.GTMod;
import gregtech.common.pollution.Pollution;

/// What a workstation's surroundings do for morale: amenities, crowding and smog. See the design document,
/// section 7.3.
public final class Amenities {

    private Amenities() {}

    /// The morale bonus of the amenities within 4 blocks of the seat, capped at 40: light, a clock, a potted plant,
    /// a window with sky behind it, posters and paintings.
    public static int score(final World world, final int x, final int y, final int z) {
        int bonus = 0;
        if (world.getBlockLightValue(x, y + 1, z) >= 10) {
            bonus += 5;
        }
        boolean clock = false;
        boolean plant = false;
        boolean window = false;
        int posters = 0;
        final int r = 4;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    final int bx = x + dx;
                    final int by = y + dy;
                    final int bz = z + dz;
                    final Block block = world.getBlock(bx, by, bz);
                    if (block.isAir(world, bx, by, bz)) {
                        continue;
                    }
                    clock |= Furniture.CLOCK.matches(block);
                    plant |= Furniture.PLANT.matches(block);
                    if (Furniture.POSTER.matches(block)) {
                        posters++;
                    }
                    if (!window && block.getMaterial() == Material.glass) {
                        window = skyBehind(world, bx, by, bz);
                    }
                }
            }
        }
        posters += world
            .getEntitiesWithinAABB(
                EntityPainting.class,
                AxisAlignedBB.getBoundingBox(x - r, y - r, z - r, x + r + 1, y + r + 1, z + r + 1))
            .size();
        bonus += (clock ? 5 : 0) + (plant ? 5 : 0) + (window ? 10 : 0) + posters;
        return Math.min(40, bonus);
    }

    private static boolean skyBehind(final World world, final int x, final int y, final int z) {
        final int[][] sides = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (final int[] side : sides) {
            final int ax = x + side[0];
            final int az = z + side[1];
            if (world.isAirBlock(ax, y, az) && world.canBlockSeeTheSky(ax, y, az)) {
                return true;
            }
        }
        return world.canBlockSeeTheSky(x, y + 1, z) && world.isAirBlock(x, y + 1, z);
    }

    /// More than one seat per 4 m² in the 5 by 5 around the seat at `x, y, z`.
    public static boolean crowded(final World world, final int x, final int y, final int z) {
        int seats = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Furniture.SEAT.matches(world.getBlock(x + dx, y, z + dz))) {
                    seats++;
                }
            }
        }
        return seats > 25 / 4;
    }

    /// The morale penalty for the smog of the chunk at `x, z`.
    public static int smog(final World world, final int x, final int z) {
        try {
            return MoraleTarget.pollutionPenalty(
                Pollution.getPollution(world.getChunkFromBlockCoords(x, z)),
                GTMod.proxy.mPollutionSmogLimit);
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }
}
