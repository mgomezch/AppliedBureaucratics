package com.gtnewhorizons.appliedbureaucratics.workforce;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;

import org.jspecify.annotations.Nullable;

/// The mobs that can be managers, with their efficiency and the levels they may sit at. Thaumcraft's cleric and
/// Blood Magic's demons are looked up by class name once, so neither mod is needed.
public enum ManagerKind {

    /// "Let's take this offline."
    ENDERMAN(null, 0.80, 1, -0),
    /// Daily stand-up.
    CLERIC("thaumcraft.common.entities.monster.EntityCultistCleric", 0.90, 2, -5),
    DEMON("WayofTime.alchemicalWizardry.common.entity.mob.EntityDemon", 1.00, 3, -10);

    private final @Nullable String className;
    private @Nullable Class<?> type;
    public final double efficiency;
    /// 1: team lead; 2: also director; 3: also executive.
    public final int maxLevel;
    public final int moralePenalty;

    ManagerKind(final @Nullable String className, final double efficiency, final int maxLevel,
        final int moralePenalty) {
        this.className = className;
        this.efficiency = efficiency;
        this.maxLevel = maxLevel;
        this.moralePenalty = moralePenalty;
    }

    public static void resolveAll() {
        ENDERMAN.type = EntityEnderman.class;
        for (final ManagerKind kind : values()) {
            if (kind.className != null) {
                try {
                    kind.type = Class.forName(kind.className);
                } catch (ClassNotFoundException | LinkageError e) {
                    kind.type = null;
                }
            }
        }
    }

    public static @Nullable ManagerKind of(final Entity entity) {
        for (final ManagerKind kind : values()) {
            if (kind.type != null && kind.type.isInstance(entity)) {
                return kind;
            }
        }
        return null;
    }
}
