package com.gtnewhorizons.appliedbureaucratics.facility.breakroom;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;

import com.gtnewhorizons.appliedbureaucratics.integration.bibliocraft.Furniture;

/// What makes a break room a microkitchen: kitchen fixtures (Cooking for Blockheads' sinks, fridges, ovens,
/// counters and cabinets, or any of [Furniture#KITCHEN]) and plants (potted plants, Natura's bushes, crops,
/// saplings, flowers) within [#REACH] blocks of the machine. See Note [Break rooms].
///
/// @param fixtures how many different kinds of kitchen fixture
/// @param plants how many plants
public record Kitchen(int fixtures, int plants) {

    public static final int REACH = 6;
    /// Points per kind of fixture, up to [#FIXTURE_CAP]; one per plant, up to [#PLANT_CAP].
    public static final int PER_FIXTURE = 4;
    public static final int FIXTURE_CAP = 20;
    public static final int PLANT_CAP = 5;
    public static final int MAX = FIXTURE_CAP + PLANT_CAP;

    public static final Kitchen BARE = new Kitchen(0, 0);

    /// The kitchen's points, 0 to [#MAX]: its morale bonus on top of the machine's.
    public int points() {
        return Math.min(FIXTURE_CAP, PER_FIXTURE * this.fixtures) + Math.min(PLANT_CAP, this.plants);
    }

    /// The kitchen's quality, 0 to 1.
    public double quality() {
        return (double) this.points() / MAX;
    }

    /// Looks around the break room machine at `x, y, z`: [#REACH] blocks across, 2 down and 3 up.
    public static Kitchen around(final World world, final int x, final int y, final int z) {
        final Set<Block> kinds = Collections.newSetFromMap(new IdentityHashMap<>());
        int plants = 0;
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dz = -REACH; dz <= REACH; dz++) {
                for (int dy = -2; dy <= 3; dy++) {
                    final Block block = world.getBlock(x + dx, y + dy, z + dz);
                    if (block == Blocks.air) {
                        continue;
                    }
                    if (Furniture.KITCHEN.matches(block)) {
                        kinds.add(block);
                    } else if (isPlant(block)) {
                        plants++;
                    }
                }
            }
        }
        return new Kitchen(kinds.size(), plants);
    }

    private static boolean isPlant(final Block block) {
        return Furniture.PLANT.matches(block)
            || block instanceof IPlantable && block != Blocks.tallgrass && block != Blocks.deadbush;
    }
}
