package com.gtnewhorizons.appliedbureaucratics.facility.breakroom;

import net.minecraft.entity.EntityLivingBase;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.compute.CoffeeLog;
import com.gtnewhorizons.appliedbureaucratics.workforce.Employment;
import com.gtnewhorizons.appliedbureaucratics.workforce.Shift;

/// A facility's view of its workers' break rooms. See Note [Break rooms].
public final class BreakRoomBindings {

    private BreakRoomBindings() {}

    /// The break room `worker` is bound to, if the binding still holds.
    public static Shift.@Nullable Binding valid(final EntityLivingBase worker) {
        final Employment employment = Employment.of(worker);
        final Shift.Binding room = employment == null ? null : employment.shift().room;
        return room != null && room.isValid(worker.worldObj.provider.dimensionId, worker.worldObj.getTotalWorldTime())
            ? room
            : null;
    }

    /// The morale bonus `room` gives a worker of a facility whose coffee is `coffee`: nothing if it is closed; else
    /// its kitchen's points and what it serves, the facility's coffee for a Coffee Machine.
    public static int bonus(final Shift.@Nullable Binding room, final CoffeeLog coffee) {
        if (room == null || !room.open()) {
            return 0;
        }
        return room.kitchen() + (room.kind() == BreakRoomKind.COFFEE_MACHINE ? coffee.bonus() : room.stockBonus());
    }

    /// Takes the cups of coffee `worker` drank since the last time into the facility's `coffee`.
    public static void collectCups(final EntityLivingBase worker, final CoffeeLog coffee) {
        final Employment employment = Employment.of(worker);
        if (employment == null || employment.shift().cups == 0) {
            return;
        }
        coffee.record(employment.shift().cups, employment.shift().cupExtras);
        employment.shift().cups = 0;
        employment.shift().cupExtras = 0;
    }
}
