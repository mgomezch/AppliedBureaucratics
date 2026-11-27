package com.gtnewhorizons.appliedbureaucratics.facility;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.compute.Affinity;
import com.gtnewhorizons.appliedbureaucratics.workforce.Employment;
import com.gtnewhorizons.appliedbureaucratics.workforce.EntityWorkstationMount;
import com.gtnewhorizons.appliedbureaucratics.workforce.WorkforceActions;

/// Who sits where: the workers and managers on workstation mounts, and their affinities.
public final class Workstations {

    /// A contracted mob on shift, at the seat `x, y, z`.
    public record Seated(EntityLivingBase mob, Employment employment, int x, int y, int z) {

        public double distanceSq(final double px, final double py, final double pz) {
            final double dx = this.x + 0.5 - px;
            final double dy = this.y + 0.5 - py;
            final double dz = this.z + 0.5 - pz;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    private Workstations() {}

    /// The worker on shift at the seat `x, y, z`, if any.
    public static @Nullable Seated at(final World world, final int x, final int y, final int z) {
        final EntityWorkstationMount mount = WorkforceActions.mountAt(world, x, y, z);
        if (mount != null && mount.riddenByEntity instanceof EntityLivingBase rider && Employment.isOnShift(rider)) {
            final Employment employment = Employment.of(rider);
            if (employment != null) {
                return new Seated(rider, employment, x, y, z);
            }
        }
        return null;
    }

    /// Every mob on shift within `range` blocks of `x, y, z`.
    public static List<Seated> within(final World world, final double x, final double y, final double z,
        final double range) {
        final List<Seated> found = new ArrayList<>();
        for (final Object o : world.getEntitiesWithinAABB(
            EntityWorkstationMount.class,
            AxisAlignedBB.getBoundingBox(x - range, y - range, z - range, x + range, y + range, z + range))) {
            final EntityWorkstationMount mount = (EntityWorkstationMount) o;
            if (!mount.isDead && mount.riddenByEntity instanceof EntityLivingBase rider
                && Employment.isOnShift(rider)) {
                final Employment employment = Employment.of(rider);
                if (employment != null) {
                    found.add(new Seated(rider, employment, mount.getSeatX(), mount.getSeatY(), mount.getSeatZ()));
                }
            }
        }
        return found;
    }

    /// A worker's affinity, from its villager profession. See the design document, section 4.1.
    public static Affinity affinity(final @Nullable EntityLivingBase worker) {
        if (worker instanceof EntityVillager villager) {
            return switch (villager.getProfession()) {
                case 0 -> Affinity.DISPATCH;
                case 1 -> Affinity.PLANNING;
                case 2 -> Affinity.EITHER;
                default -> Affinity.NONE;
            };
        }
        return Affinity.NONE;
    }

    public static int profession(final EntityLivingBase worker) {
        return worker instanceof EntityVillager villager ? villager.getProfession() : -1;
    }
}
