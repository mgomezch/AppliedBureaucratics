package com.gtnewhorizons.appliedbureaucratics.integration.oc;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.World;

/// When each robot with a Requisition Upgrade last clocked in, by position. A robot counts as working on a Docking
/// Pad while its last `work()` call was under [#WINDOW] ticks ago. See Note [Robots].
public final class BotRegistry {

    public static final int WINDOW = 100;
    private static final Map<Long, Long> LAST_WORK = new HashMap<>();

    private BotRegistry() {}

    private static long key(final int dimension, final int x, final int y, final int z) {
        return ((long) (dimension & 0xFF) << 56) | ((long) (x & 0x3FFFFFF) << 30)
            | ((long) (y & 0xFF) << 22)
            | (z & 0x3FFFFF);
    }

    public static void clockIn(final World world, final int x, final int y, final int z) {
        LAST_WORK.put(key(world.provider.dimensionId, x, y, z), world.getTotalWorldTime());
    }

    public static boolean isWorking(final World world, final int x, final int y, final int z) {
        final Long last = LAST_WORK.get(key(world.provider.dimensionId, x, y, z));
        return last != null && world.getTotalWorldTime() - last <= WINDOW;
    }
}
