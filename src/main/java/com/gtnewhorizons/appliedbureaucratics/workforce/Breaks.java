package com.gtnewhorizons.appliedbureaucratics.workforce;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnewhorizons.appliedbureaucratics.compute.BreakSchedule;
import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;
import com.gtnewhorizons.appliedbureaucratics.facility.breakroom.MTEBreakRoom;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/// Workers' breaks: when one is due, the walk to the break room, the wander about it, and the walk back to the seat.
/// See Note [Break rooms].
public final class Breaks {

    /// How fast a worker on a break walks, as a fraction of its speed.
    private static final double SPEED = 0.6;
    /// A walk that has not arrived in a minute is given up on: the worker is put where it was going.
    private static final long WALK_TIMEOUT = 1200;
    /// How long a seat counts as away after its worker was last seen on its break.
    private static final long AWAY_GRACE = 100;

    /// Per world, the seats whose workers are on a break, by [#key], with when each worker was last seen walking.
    private static final Map<World, Map<Long, Long>> AWAY = new WeakHashMap<>();

    private Breaks() {}

    /// Called every tick for a worker on its seat: starts its break when one is due.
    public static void whileSeated(final EntityWorkstationMount mount, final EntityLiving worker,
        final Employment employment) {
        if (employment.getRole() != Employment.Role.WORKER) {
            return;
        }
        final World world = mount.worldObj;
        final long now = world.getTotalWorldTime();
        final Shift shift = employment.shift();
        final Shift.Binding room = shift.room;
        // No open break room, no breaks.
        if (room == null || !room.open() || !room.isValid(world.provider.dimensionId, now)) {
            return;
        }
        final BreakSchedule schedule = BreakSchedule.of(shift.isManaged(now), room.quality());
        if (shift.nextBreakAt == 0) {
            // A new worker, or one just loaded: its first break comes anywhere in the next interval.
            shift.nextBreakAt = now + (long) (schedule.intervalTicks() * ThreadLocalRandom.current()
                .nextDouble());
            return;
        }
        if (now < shift.nextBreakAt) {
            return;
        }
        shift.nextBreakAt = 0;
        shift.onBreak = new Shift.Break(
            mount.getSeatX(),
            mount.getSeatY(),
            mount.getSeatZ(),
            room.x(),
            room.y(),
            room.z(),
            schedule.durationTicks(),
            now);
        worker.mountEntity(null);
        Employment.save(worker, employment);
        away(world).put(key(mount.getSeatX(), mount.getSeatY(), mount.getSeatZ()), now);
    }

    /// Called every tick, in place of its own AI, for a worker on a break.
    public static void drive(final EntityLiving worker) {
        final Employment employment = Employment.of(worker);
        final Shift.Break trip = employment == null ? null : employment.shift().onBreak;
        if (trip == null) {
            return;
        }
        final World world = worker.worldObj;
        final long now = world.getTotalWorldTime();
        // A BiblioCraft seat sits down any mob that walks onto it. See wrinkle (W7) of Note [Break rooms].
        if (worker.ridingEntity != null) {
            worker.mountEntity(null);
        }
        away(world).put(key(trip.seatX, trip.seatY, trip.seatZ), now);
        final PathNavigate nav = worker.getNavigator();
        switch (trip.phase) {
            case GOING -> {
                if (near(worker, trip.roomX, trip.roomY, trip.roomZ, 3)) {
                    nav.clearPathEntity();
                    arrive(worker, trip, now);
                } else if (now - trip.phaseStart > WALK_TIMEOUT) {
                    worker.setPositionAndUpdate(trip.roomX + 0.5, trip.roomY, trip.roomZ + 1.5);
                    arrive(worker, trip, now);
                } else if (nav.noPath() && now % 20 == 0) {
                    walkTo(world, nav, trip.roomX, trip.roomY, trip.roomZ);
                }
            }
            case THERE -> {
                if (now - trip.phaseStart >= trip.stay) {
                    nav.clearPathEntity();
                    trip.enter(Shift.Phase.RETURNING, now);
                } else if (nav.noPath() && worker.getRNG()
                    .nextInt(60) == 0) {
                        final int r = ABConfig.breakRoomWander;
                        nav.tryMoveToXYZ(
                            trip.roomX + 0.5
                                + worker.getRNG()
                                    .nextInt(2 * r + 1)
                                - r,
                            trip.roomY,
                            trip.roomZ + 0.5
                                + worker.getRNG()
                                    .nextInt(2 * r + 1)
                                - r,
                            SPEED);
                    }
            }
            case RETURNING -> {
                if (near(worker, trip.seatX, trip.seatY, trip.seatZ, 1.5)) {
                    backToWork(worker, employment);
                    return;
                }
                if (now - trip.phaseStart > WALK_TIMEOUT) {
                    worker.setPositionAndUpdate(trip.seatX + 0.5, trip.seatY, trip.seatZ + 0.5);
                    backToWork(worker, employment);
                    return;
                }
                if (nav.noPath() && now % 20 == 0) {
                    nav.tryMoveToXYZ(trip.seatX + 0.5, trip.seatY, trip.seatZ + 0.5, SPEED);
                }
            }
        }
        // What EntityLiving.updateAITasks does besides running the AI's tasks, which stay suspended.
        nav.onUpdateNavigation();
        worker.getMoveHelper()
            .onUpdateMoveHelper();
        worker.getLookHelper()
            .onUpdateLook();
        worker.getJumpHelper()
            .doJump();
    }

    /// Whether the worker of the seat at `x, y, z` is away on a break.
    public static boolean isAway(final World world, final int x, final int y, final int z) {
        final Long seen = away(world).get(key(x, y, z));
        return seen != null && world.getTotalWorldTime() - seen <= AWAY_GRACE;
    }

    /// How many seats within `range` blocks of `x, y, z` have their workers away on a break.
    public static int awayNear(final World world, final double x, final double y, final double z, final double range) {
        final long now = world.getTotalWorldTime();
        int n = 0;
        for (final Map.Entry<Long, Long> e : away(world).entrySet()) {
            if (now - e.getValue() > AWAY_GRACE) {
                continue;
            }
            final long k = e.getKey();
            final double dx = unpackX(k) + 0.5 - x;
            final double dy = unpackY(k) + 0.5 - y;
            final double dz = unpackZ(k) + 0.5 - z;
            if (dx * dx + dy * dy + dz * dz <= range * range) {
                n++;
            }
        }
        return n;
    }

    /// At the break room: it serves the worker, who then wanders about it for the rest of the break.
    private static void arrive(final EntityLiving worker, final Shift.Break trip, final long now) {
        final TileEntity te = worker.worldObj.getTileEntity(trip.roomX, trip.roomY, trip.roomZ);
        if (te instanceof IGregTechTileEntity tile && tile.getMetaTileEntity() instanceof MTEBreakRoom room) {
            room.visit(worker);
        }
        trip.enter(Shift.Phase.THERE, now);
    }

    private static void backToWork(final EntityLiving worker, final Employment employment) {
        final Shift.Break trip = employment.shift().onBreak;
        employment.shift().onBreak = null;
        Employment.save(worker, employment);
        worker.getNavigator()
            .clearPathEntity();
        if (trip != null) {
            away(worker.worldObj).remove(key(trip.seatX, trip.seatY, trip.seatZ));
            WorkforceActions.sit(worker.worldObj, worker, trip.seatX, trip.seatY, trip.seatZ, worker.rotationYaw);
        }
    }

    /// Walks towards the break room at `x, y, z`: to a free block beside it, since the machine itself is solid.
    private static void walkTo(final World world, final PathNavigate nav, final int x, final int y, final int z) {
        final int[][] sides = { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } };
        for (final int[] side : sides) {
            final int ax = x + side[0];
            final int az = z + side[1];
            if (world.isAirBlock(ax, y, az) && world.isAirBlock(ax, y + 1, az)
                && nav.tryMoveToXYZ(ax + 0.5, y, az + 0.5, SPEED)) {
                return;
            }
        }
        nav.tryMoveToXYZ(x + 0.5, y, z + 0.5, SPEED);
    }

    private static boolean near(final EntityLiving worker, final int x, final int y, final int z, final double r) {
        final double dx = worker.posX - (x + 0.5);
        final double dz = worker.posZ - (z + 0.5);
        return dx * dx + dz * dz <= r * r && Math.abs(worker.posY - y) <= 2;
    }

    private static Map<Long, Long> away(final World world) {
        return AWAY.computeIfAbsent(world, w -> new HashMap<>());
    }

    private static long key(final int x, final int y, final int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static int unpackX(final long k) {
        return (int) (k >> 38) << 6 >> 6;
    }

    private static int unpackZ(final long k) {
        return (int) ((k >> 12) & 0x3FFFFFF) << 6 >> 6;
    }

    private static int unpackY(final long k) {
        return (int) (k & 0xFFF);
    }
}
