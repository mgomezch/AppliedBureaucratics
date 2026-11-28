package com.gtnewhorizons.appliedbureaucratics.facility.internet;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.gtnewhorizons.appliedbureaucratics.util.Cadence;
import com.gtnewhorizons.appliedbureaucratics.workforce.Employment;

/// A Gig Kiosk: "be your own boss." It enrols the uncontracted villagers within 16 blocks, up to 16, as gig workers
/// for an Internet within 32 blocks: no contract, no seat, no morale. See Note [The Internet].
public class TileGigKiosk extends TileEntity {

    public static final int MAX_WORKERS = 16;
    private static final WeakHashMap<World, Set<TileGigKiosk>> KIOSKS = new WeakHashMap<>();

    private int enrolled;
    private final Cadence scanCadence = new Cadence(Cadence.LARGE);

    public static Set<TileGigKiosk> in(final World world) {
        final Set<TileGigKiosk> kiosks = KIOSKS.get(world);
        return kiosks != null ? kiosks : Collections.emptySet();
    }

    public int getEnrolled() {
        return this.enrolled;
    }

    @Override
    public void updateEntity() {
        // A scan of large reach. See Note [Spatial checks are staggered].
        if (this.worldObj == null || this.worldObj.isRemote || !this.scanCadence.tick()) {
            return;
        }
        final List<?> villagers = this.worldObj.getEntitiesWithinAABB(
            EntityVillager.class,
            AxisAlignedBB.getBoundingBox(
                this.xCoord - 16,
                this.yCoord - 16,
                this.zCoord - 16,
                this.xCoord + 17,
                this.yCoord + 17,
                this.zCoord + 17));
        int n = 0;
        for (final Object o : villagers) {
            if (Employment.of((EntityVillager) o) == null) {
                n++;
            }
        }
        this.enrolled = Math.min(MAX_WORKERS, n);
    }

    @Override
    public void validate() {
        super.validate();
        if (this.worldObj != null && !this.worldObj.isRemote) {
            KIOSKS.computeIfAbsent(this.worldObj, w -> Collections.newSetFromMap(new WeakHashMap<>()))
                .add(this);
        }
    }

    @Override
    public void setWorldObj(final World world) {
        super.setWorldObj(world);
        if (!this.isInvalid()) {
            this.validate();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        final Set<TileGigKiosk> kiosks = this.worldObj == null ? null : KIOSKS.get(this.worldObj);
        if (kiosks != null) {
            kiosks.remove(this);
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        this.invalidate();
    }
}
