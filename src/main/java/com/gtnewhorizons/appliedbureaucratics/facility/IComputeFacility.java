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

    /// Lines for `/ab status`: the bottleneck first, then the figures.
    default List<String> statusLines() {
        return Collections.emptyList();
    }
}
