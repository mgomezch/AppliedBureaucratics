package com.gtnewhorizons.appliedbureaucratics.workforce;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.EntityEnderman;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.compute.OrgChart;
import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;

/// The mobs that can be managers, with their efficiency, their roles in the org chart ([OrgChart#LEAD],
/// [OrgChart#DIRECT], [OrgChart#EXECUTE]) and their morale penalty. Team leads are Endermen, Crimson Clerics and
/// tamed demons. Directors must be bosses: any mob with a boss bar (a Wither, the Twilight Forest's bosses), or one
/// of [ABConfig#bossClasses]. Executives are demons, in the Internet's boardroom. Thaumcraft's and Blood Magic's
/// classes are looked up by name once, so neither mod is needed.
public enum ManagerKind {

    /// "Let's take this offline."
    ENDERMAN(null, 0.80, OrgChart.LEAD, 0),
    /// Daily stand-up.
    CLERIC("thaumcraft.common.entities.monster.EntityCultistCleric", 0.90, OrgChart.LEAD, 5),
    DEMON("WayofTime.alchemicalWizardry.common.entity.mob.EntityDemon", 1.00, OrgChart.LEAD | OrgChart.EXECUTE, 10),
    /// "My door is always open." It is not a door.
    BOSS(null, 0.95, OrgChart.DIRECT, 15);

    private final @Nullable String className;
    private @Nullable Class<?> type;
    public final double efficiency;
    public final int roles;
    public final int moralePenalty;

    private static final List<Class<?>> BOSS_TYPES = new ArrayList<>();

    ManagerKind(final @Nullable String className, final double efficiency, final int roles, final int moralePenalty) {
        this.className = className;
        this.efficiency = efficiency;
        this.roles = roles;
        this.moralePenalty = moralePenalty;
    }

    public static void resolveAll() {
        ENDERMAN.type = EntityEnderman.class;
        for (final ManagerKind kind : values()) {
            if (kind.className != null) {
                kind.type = find(kind.className);
            }
        }
        BOSS_TYPES.clear();
        for (final String name : ABConfig.bossClasses) {
            final Class<?> type = find(name);
            if (type != null) {
                BOSS_TYPES.add(type);
            }
        }
    }

    private static @Nullable Class<?> find(final String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    /// Makes the manager's noise, so the office hears who is in charge: an Enderman screams, and every other kind
    /// makes its own idle sound, if it has one.
    public void intimidate(final EntityLivingBase manager) {
        if (this == ENDERMAN) {
            manager.worldObj.playSoundAtEntity(
                manager,
                "mob.endermen.scream",
                1.0F,
                0.9F + manager.getRNG()
                    .nextFloat() * 0.2F);
        } else if (manager instanceof EntityLiving living) {
            living.playLivingSound();
        }
    }

    public static @Nullable ManagerKind of(final Entity entity) {
        if (entity instanceof IBossDisplayData) {
            return BOSS;
        }
        for (final Class<?> type : BOSS_TYPES) {
            if (type.isInstance(entity)) {
                return BOSS;
            }
        }
        for (final ManagerKind kind : values()) {
            if (kind.type != null && kind.type.isInstance(entity)) {
                return kind;
            }
        }
        return null;
    }
}
