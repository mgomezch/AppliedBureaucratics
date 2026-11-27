package com.gtnewhorizons.appliedbureaucratics.facility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.compute.DispatchMeter;

import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkCraftingCpuChange;
import appeng.me.cluster.implementations.HostedCraftingCPUCluster;
import appeng.me.cluster.implementations.ICraftingCPUHost;

/*-
Note [Facility job slots] (generated)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Each job slot of a facility is one crafting CPU on the ME network, an AE2 HostedCraftingCPUCluster whose host is the
facility (the ICraftingCPUHost contract is documented on that interface in our AE2NH fork).  FacilitySlots does the
host's side of that contract for every facility:

  Filing    The facility's Filing is a pool.  A busy slot holds its job's bytes; every slot reports the free part
            of the pool as what it can take, a busy slot on top of what it holds (so AE2 can merge a job of the same
            item into it).  Filing is checked only when a job is submitted, so capacity lost later never cancels a
            job: see wrinkle (W1) of Note [The compute model].
  Dispatch  The facility's dispatch rate is shared equally among its busy slots.  Each slot has its own
            DispatchMeter, which hands out whole operations per tick and carries the fraction.  A slot's
            co-processor count is set to what a stock CPU would need for the same rate, for display and for how
            AE2 ranks CPUs when picking one.
  Saving    Each slot's job is saved in the facility's NBT and restored only once the facility's grid node is on a
            network (restore), since a job's patterns and crafting link need the world and the grid.
  Removal   dispose() cancels each slot's job, returns its items to the network and drops at the facility what the
            network does not take.  Breaking a facility therefore never loses items while the network is there.

Growing or shrinking the number of slots (setSlotCount) only ever removes idle slots; a busy one stays until its job
ends.
*/

/// The job slots of a facility. See Note [Facility job slots].
public final class FacilitySlots {

    private final ICraftingCPUHost host;
    private final List<HostedCraftingCPUCluster> cpus = new ArrayList<>();
    private final List<DispatchMeter> meters = new ArrayList<>();
    private int wanted;
    private long filing;
    private double dispatchRate;
    private int busy;
    private @Nullable NBTTagList saved;

    public FacilitySlots(final ICraftingCPUHost host) {
        this.host = host;
    }

    public Collection<HostedCraftingCPUCluster> getCPUs() {
        return Collections.unmodifiableList(this.cpus);
    }

    public int getBusyCount() {
        return this.busy;
    }

    public boolean anyBusy() {
        return this.busy > 0;
    }

    public long getFiling() {
        return this.filing;
    }

    public double getDispatchRate() {
        return this.dispatchRate;
    }

    /// The bytes held by busy slots.
    public long getUsedFiling() {
        long used = 0;
        for (final HostedCraftingCPUCluster cpu : this.cpus) {
            if (cpu.isBusy()) {
                used += cpu.getUsedStorage();
            }
        }
        return used;
    }

    public void setSlotCount(final int slots) {
        this.wanted = Math.max(0, slots);
        this.resize();
    }

    public void setFiling(final long bytes) {
        this.filing = Math.max(0, bytes);
    }

    public void setDispatchRate(final double opsPerSecond) {
        this.dispatchRate = Math.max(0, opsPerSecond);
    }

    /// The operations `cpu` may perform this tick.
    public int grant(final HostedCraftingCPUCluster cpu) {
        final int i = this.cpus.indexOf(cpu);
        if (i < 0 || this.busy == 0) {
            return 0;
        }
        return this.meters.get(i)
            .grant(this.dispatchRate / this.busy);
    }

    /// Call once per tick, before AE2 runs the slots' jobs.
    public void tick() {
        this.resize();
        int busy = 0;
        for (final HostedCraftingCPUCluster cpu : this.cpus) {
            if (cpu.isBusy()) {
                busy++;
            }
        }
        this.busy = busy;
        final long free = Math.max(0, this.filing - this.getUsedFiling());
        final double perSlot = busy > 0 ? this.dispatchRate / busy : this.dispatchRate;
        // A stock CPU with c co-processors dispatches (c + 1) / 4 operations a tick, 5 (c + 1) a second.
        final int coProcessors = (int) Math.max(0, Math.min(Integer.MAX_VALUE, Math.ceil(perSlot / 5 - 1)));
        for (final HostedCraftingCPUCluster cpu : this.cpus) {
            cpu.setAvailableStorage(cpu.isBusy() ? cpu.getUsedStorage() + free : free);
            cpu.setCoProcessors(coProcessors);
        }
    }

    private void resize() {
        boolean changed = false;
        while (this.cpus.size() < this.wanted) {
            this.cpus.add(new HostedCraftingCPUCluster(this.host));
            this.meters.add(new DispatchMeter());
            changed = true;
        }
        for (int i = this.cpus.size() - 1; i >= this.wanted; i--) {
            final HostedCraftingCPUCluster cpu = this.cpus.get(i);
            if (cpu.isBusy()) {
                break;
            }
            cpu.dispose();
            this.cpus.remove(i);
            this.meters.remove(i);
            changed = true;
        }
        if (changed) {
            this.cpusChanged();
            for (final HostedCraftingCPUCluster cpu : this.cpus) {
                cpu.updateName();
            }
        }
    }

    private void cpusChanged() {
        final IGridNode node = this.host.getActionableNode();
        if (node != null && node.getGrid() != null) {
            node.getGrid()
                .postEvent(new MENetworkCraftingCpuChange(node));
        }
    }

    public boolean hasSavedJobs() {
        return this.saved != null;
    }

    /// Restores the jobs read by [#readFromNBT]. Call once the host's grid node is on a network.
    public void restore() {
        final NBTTagList saved = this.saved;
        if (saved == null) {
            return;
        }
        this.saved = null;
        this.wanted = Math.max(this.wanted, saved.tagCount());
        this.resize();
        for (int i = 0; i < saved.tagCount() && i < this.cpus.size(); i++) {
            final NBTTagCompound job = saved.getCompoundTagAt(i);
            if (job.hasKey("finalOutput")) {
                this.cpus.get(i)
                    .readFromNBT(job);
            }
        }
        this.cpusChanged();
    }

    public void writeToNBT(final NBTTagCompound tag) {
        final NBTTagList list = new NBTTagList();
        if (this.saved != null) {
            // Not restored yet: keep what was read.
            for (int i = 0; i < this.saved.tagCount(); i++) {
                list.appendTag(this.saved.getCompoundTagAt(i));
            }
        } else {
            for (final HostedCraftingCPUCluster cpu : this.cpus) {
                final NBTTagCompound job = new NBTTagCompound();
                if (cpu.isBusy()) {
                    cpu.writeToNBT(job);
                }
                list.appendTag(job);
            }
        }
        tag.setTag("slots", list);
    }

    public void readFromNBT(final NBTTagCompound tag) {
        this.saved = tag.getTagList("slots", Constants.NBT.TAG_COMPOUND);
    }

    /// Ends every slot for good, when the facility is removed.
    public void dispose() {
        for (final HostedCraftingCPUCluster cpu : this.cpus) {
            cpu.dispose();
        }
        this.cpus.clear();
        this.meters.clear();
        this.wanted = 0;
    }
}
