package com.gtnewhorizons.appliedbureaucratics.integration.ae2;

import java.util.Collection;

import com.gtnewhorizons.appliedbureaucratics.compute.PlanningPool;
import com.gtnewhorizons.appliedbureaucratics.facility.IComputeFacility;

import appeng.api.networking.IGridCache;
import appeng.api.networking.crafting.compute.ICraftingPlanningJob;

/// A network's compute: its facilities and its planning pool. Registered with AE2 as a grid cache, so every network
/// has one. See Note [The compute model].
public interface IComputeGrid extends IGridCache {

    PlanningPool<ICraftingPlanningJob> getPlanningPool();

    Collection<IComputeFacility> getFacilities();

    /// Whether any plan is being calculated on this network.
    boolean hasPlansInProgress();

    /// Counts steps charged to this network, to share among its facilities for paper.
    void charge(double steps);
}
