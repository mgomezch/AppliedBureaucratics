package com.gtnewhorizons.appliedbureaucratics.facility;

import java.util.Collections;
import java.util.List;

/// A machine on an ME network that supplies compute: a Desk, a Mainframe, an Internet, a Data Center, a
/// Singularity. The network finds it through its grid node, like AE2 finds an [appeng.me.cluster.implementations
/// .ICraftingCPUHost]; it is usually one too. See Note [The compute model].
public interface IComputeFacility {

    /// The planning steps per second this facility adds to its network now: 0 when it is off, or when it plans
    /// through another facility (a mainframe linked to an Internet).
    double getPlanningRate();

    /// Whether this facility would plan if a plan were waiting. A facility whose slots are busy gives planning no
    /// time until a plan is pending, so the network must not refuse a new plan just because its planning rate is 0
    /// at that moment: see [com.gtnewhorizons.appliedbureaucratics.compute.PlanningPool#hasCapacity].
    default boolean couldPlan() {
        return this.getPlanningRate() > 0;
    }

    /// Whether people staff it. Staffed facilities on one network plan by committee: see wrinkle (W5) of
    /// Note [The compute model].
    boolean isStaffed();

    /// The planning steps the network charged to this facility over the last second, for its paper.
    default void planningCharged(final double steps) {}

    /// The chance that a plan this facility calculated most of is re-verified: a Data Center's hallucinations. See
    /// Note [The Data Center].
    default double hallucinationChance() {
        return 0;
    }

    /// Whether this facility makes its network's planning unmetered, as a well-disposed Singularity does.
    default boolean unmeteredPlanning() {
        return false;
    }

    /// Pins the share of the facility's FTE spent planning, or with null lets it choose (the default: all of it while
    /// idle, none while busy, a quarter while both). For OpenComputers' `setAllocation`.
    default void setPinnedPlanningShare(final Double share) {}

    /// Forces crunch on or off, or with null leaves it to redstone. For OpenComputers' `setCrunch`.
    default void setCrunchOverride(final Boolean crunch) {}

    /// Lines for `/ab status`: the bottleneck first, then the figures.
    default List<String> statusLines() {
        return Collections.emptyList();
    }
}
