package com.gtnewhorizons.appliedbureaucratics.facility.breakroom;

import net.minecraft.item.ItemStack;

import com.gtnewhorizons.appliedbureaucratics.block.ABTextures;
import com.gtnewhorizons.appliedbureaucratics.config.ItemList;

import gregtech.api.interfaces.IIconContainer;

/// The three break room machines, by the area they serve: a Water Cooler serves its own chunk, a Coffee Machine the
/// 3 by 3 chunks around it, a Snack Vending Machine the 5 by 5. See Note [Break rooms].
public enum BreakRoomKind {

    /// "Did you see the game last night?"
    WATER_COOLER(32210, "water_cooler", "Water Cooler", 1, 0, 0),
    /// "Don't talk to me before my coffee."
    COFFEE_MACHINE(32211, "coffee_machine", "Coffee Machine", 3, 1, 4096),
    /// "Exact change only."
    SNACK_MACHINE(32212, "snack_machine", "Snack Vending Machine", 4, 2, 16384);

    /// A Water Cooler's bonus.
    public static final int WATER_BONUS = 5;
    /// A Snack Vending Machine's bonus: [#SNACK_PLAIN], and [#PER_SANDWICH] more per kind of sandwich in stock, up to
    /// [#SANDWICH_KINDS] kinds.
    public static final int SNACK_PLAIN = 4;
    public static final int PER_SANDWICH = 2;
    public static final int SANDWICH_KINDS = 6;
    /// Millilitres of water a Water Cooler pours a visit, and of milk a cup of coffee takes.
    public static final int WATER_PER_VISIT = 250;
    public static final int MILK_PER_CUP = 100;

    public final int id;
    public final String name;
    public final String regionalName;
    /// The voltage tier of its hull and power.
    public final int tier;
    /// The chunks it serves around its own, in each direction: 0 for one chunk, 1 for 3 by 3, 2 for 5 by 5.
    public final int chunkRadius;
    /// The power a visit takes; 0 for a Water Cooler, which needs none.
    public final long euPerVisit;

    BreakRoomKind(final int id, final String name, final String regionalName, final int tier, final int chunkRadius,
        final long euPerVisit) {
        this.id = id;
        this.name = name;
        this.regionalName = regionalName;
        this.tier = tier;
        this.chunkRadius = chunkRadius;
        this.euPerVisit = euPerVisit;
    }

    public boolean isPowered() {
        return this.euPerVisit > 0;
    }

    /// Whether its tank takes a fluid: milk, for a Coffee Machine. A Water Cooler drinks from the tank above it.
    public boolean hasTank() {
        return this == COFFEE_MACHINE;
    }

    /// Whether `stack` goes in its slots: what it serves, or party food.
    public boolean accepts(final ItemStack stack) {
        if (ItemList.PARTY_FOOD.matches(stack)) {
            return true;
        }
        return switch (this) {
            case WATER_COOLER -> false;
            case COFFEE_MACHINE -> ItemList.COFFEE_BEANS.matches(stack) || ItemList.SUGAR.matches(stack)
                || ItemList.COCOA_POWDER.matches(stack)
                || ItemList.SOY_MILK.matches(stack);
            case SNACK_MACHINE -> ItemList.SANDWICHES.matches(stack);
        };
    }

    public IIconContainer front(final boolean active) {
        return switch (this) {
            case WATER_COOLER -> active ? ABTextures.WATER_COOLER_ACTIVE : ABTextures.WATER_COOLER;
            case COFFEE_MACHINE -> active ? ABTextures.COFFEE_MACHINE_ACTIVE : ABTextures.COFFEE_MACHINE;
            case SNACK_MACHINE -> active ? ABTextures.VENDING_FRONT_ON : ABTextures.VENDING_FRONT_OFF;
        };
    }
}
