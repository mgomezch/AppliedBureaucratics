package com.gtnewhorizons.appliedbureaucratics.integration.ae2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import com.gtnewhorizons.appliedbureaucratics.compute.PlanningPool;
import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;
import com.gtnewhorizons.appliedbureaucratics.facility.IComputeFacility;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridStorage;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.compute.ICraftingPlanningJob;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.cluster.implementations.HostedCraftingCPUCluster;

/// A network's compute. Every second it adds up its facilities' planning rates (wrinkle (W5) of Note [The compute
/// model]) and the stock crafting CPUs', and decides whether the network plans unmetered because it has legacy CPUs;
/// every tick it advances the planning pool.
public class ComputeGridCache implements IComputeGrid {

    private final IGrid grid;
    private final Set<IComputeFacility> facilities = Collections.newSetFromMap(new IdentityHashMap<>());
    private final PlanningPool<ICraftingPlanningJob> pool = new PlanningPool<>();
    private int ticks;
    private double charged;
    private double hallucination;

    public ComputeGridCache(final IGrid grid) {
        this.grid = grid;
    }

    @Override
    public PlanningPool<ICraftingPlanningJob> getPlanningPool() {
        return this.pool;
    }

    @Override
    public Collection<IComputeFacility> getFacilities() {
        return Collections.unmodifiableSet(this.facilities);
    }

    @Override
    public boolean hasPlansInProgress() {
        return this.pool.planCount() > 0;
    }

    @Override
    public double hallucinationChance() {
        return this.hallucination;
    }

    @Override
    public void charge(final double steps) {
        this.charged += steps;
    }

    @Override
    public void onUpdateTick() {
        if (this.ticks++ % 20 == 0) {
            this.recompute();
        }
        this.pool.tick();
    }

    private void recompute() {
        final List<Double> staffed = new ArrayList<>();
        double machines = 0;
        double total = 0;
        double hallucinating = 0;
        double weighted = 0;
        for (final IComputeFacility facility : this.facilities) {
            final double rate = facility.getPlanningRate();
            if (rate <= 0) {
                continue;
            }
            total += rate;
            if (facility.hallucinationChance() > 0) {
                hallucinating += rate;
                weighted += rate * facility.hallucinationChance();
            }
            if (facility.isStaffed()) {
                staffed.add(rate);
            } else {
                machines += rate;
            }
        }
        boolean legacy = false;
        final ICraftingGrid crafting = this.grid.getCache(ICraftingGrid.class);
        for (final ICraftingCPU cpu : crafting.getCpus()) {
            if (cpu instanceof CraftingCPUCluster cluster && !(cpu instanceof HostedCraftingCPUCluster)) {
                if (cluster.hasLegacyBlocks()) {
                    legacy = true;
                } else {
                    machines += ABConfig.meteredClusterPlanningRate;
                }
            }
        }
        this.pool.setRate(PlanningPool.networkRate(staffed, machines));
        this.hallucination = total > 0 && hallucinating * 2 >= total ? weighted / hallucinating : 0;
        this.pool.setUnmetered(legacy && ABConfig.legacyPlanningUnmetered);
        if (this.charged > 0 && total > 0) {
            for (final IComputeFacility facility : this.facilities) {
                final double rate = facility.getPlanningRate();
                if (rate > 0) {
                    facility.planningCharged(this.charged * rate / total);
                }
            }
        }
        this.charged = 0;
    }

    @Override
    public void addNode(final IGridNode gridNode, final IGridHost machine) {
        if (machine instanceof IComputeFacility facility) {
            this.facilities.add(facility);
        }
    }

    @Override
    public void removeNode(final IGridNode gridNode, final IGridHost machine) {
        if (machine instanceof IComputeFacility facility) {
            this.facilities.remove(facility);
        }
    }

    @Override
    public void onSplit(final IGridStorage destinationStorage) {}

    @Override
    public void onJoin(final IGridStorage sourceStorage) {}

    @Override
    public void populateGridStorage(final IGridStorage destinationStorage) {}
}
