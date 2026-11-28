package com.gtnewhorizons.appliedbureaucratics.workforce;

import net.minecraft.nbt.NBTTagCompound;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.facility.breakroom.BreakRoomKind;

/// What a contracted worker's day looks like beyond its contract: the break room it is bound to, whether it is
/// managed, when its next break is, and the break it is on. Kept with its [Employment]. Only the break it is on is
/// saved; the rest is refreshed by the break room's scan and the facility's survey within minutes of a load. See
/// Note [Break rooms].
public final class Shift {

    /// A break room machine's claim on a worker, until `until` (world time) unless it renews it.
    ///
    /// @param kitchen its kitchen's points
    /// @param stockBonus the bonus of what it serves, but for coffee, whose bonus is the facility's: a Water
    /// Cooler's, or a Snack Vending Machine's for its variety of sandwiches
    /// @param quality its kitchen's quality, 0 to 1
    /// @param open whether it is open; a closed one gives nothing and sends no one on a break
    /// @param party whether it is throwing a pizza party
    public record Binding(int dimension, int x, int y, int z, BreakRoomKind kind, int kitchen, int stockBonus,
        double quality, boolean open, boolean party, long until) {

        public boolean isValid(final int dimension, final long now) {
            return this.dimension == dimension && now <= this.until;
        }

        public boolean isAt(final int dimension, final int x, final int y, final int z) {
            return this.dimension == dimension && this.x == x && this.y == y && this.z == z;
        }
    }

    /// Where a break goes.
    public enum Phase {
        /// Walking to the break room.
        GOING,
        /// Wandering about the break room.
        THERE,
        /// Walking back to the seat.
        RETURNING
    }

    /// A break in progress: from the seat at `seat*`, to the break room at `room*`, for `stay` ticks there.
    public static final class Break {

        public final int seatX;
        public final int seatY;
        public final int seatZ;
        public final int roomX;
        public final int roomY;
        public final int roomZ;
        public final long stay;
        public Phase phase = Phase.GOING;
        /// When the current phase began (world time).
        public long phaseStart;

        public Break(final int seatX, final int seatY, final int seatZ, final int roomX, final int roomY,
            final int roomZ, final long stay, final long now) {
            this.seatX = seatX;
            this.seatY = seatY;
            this.seatZ = seatZ;
            this.roomX = roomX;
            this.roomY = roomY;
            this.roomZ = roomZ;
            this.stay = stay;
            this.phaseStart = now;
        }

        public void enter(final Phase phase, final long now) {
            this.phase = phase;
            this.phaseStart = now;
        }

        public NBTTagCompound save() {
            final NBTTagCompound tag = new NBTTagCompound();
            tag.setIntArray("seat", new int[] { this.seatX, this.seatY, this.seatZ });
            tag.setIntArray("room", new int[] { this.roomX, this.roomY, this.roomZ });
            tag.setLong("stay", this.stay);
            tag.setByte("phase", (byte) this.phase.ordinal());
            tag.setLong("phaseStart", this.phaseStart);
            return tag;
        }

        public static @Nullable Break load(final NBTTagCompound tag) {
            final int[] seat = tag.getIntArray("seat");
            final int[] room = tag.getIntArray("room");
            if (seat.length != 3 || room.length != 3) {
                return null;
            }
            final Break b = new Break(seat[0], seat[1], seat[2], room[0], room[1], room[2], tag.getLong("stay"), 0);
            b.phase = Phase.values()[Math.min(Phase.values().length - 1, Math.max(0, tag.getByte("phase")))];
            b.phaseStart = tag.getLong("phaseStart");
            return b;
        }
    }

    public @Nullable Binding room;
    /// Until when (world time) the worker counts as managed, as its facility last said.
    public long managedUntil;
    /// When the next break is due (world time); 0 until the worker is first seen seated.
    public long nextBreakAt;
    public @Nullable Break onBreak;
    /// Cups of coffee drunk since the facility last asked, and their extras between them. Not the worker's to
    /// keep: the facility takes them into its CoffeeLog at its next survey.
    public int cups;
    public int cupExtras;

    public boolean isManaged(final long now) {
        return now <= this.managedUntil;
    }
}
