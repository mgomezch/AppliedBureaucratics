package com.gtnewhorizons.appliedbureaucratics.compute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.gtnewhorizons.appliedbureaucratics.compute.Affinity.Department;

/// Splits a facility's FTE between planning and dispatch. See Note [The compute model].
///
/// Each worker contributes its FTE, scaled by its managers and by the square-root rule over the facility's roots.
/// Workers are assigned to planning in order of how good they are at it until `planningShare` of the facility's FTE
/// is assigned, the last one in part, and the rest to dispatch. Each department's FTE then counts its workers'
/// affinity multipliers.
public final class Workforce {

    /// The facility FTE before the split, and each department's FTE after it.
    public record Split(double facilityFte, double planningFte, double dispatchFte) {

        public static final Split NONE = new Split(0, 0, 0);
    }

    private Workforce() {}

    public static Split split(final List<OrgUnit> roots, final double planningShare) {
        if (roots.isEmpty()) {
            return Split.NONE;
        }
        final List<double[]> contributions = new ArrayList<>();
        final List<Affinity> affinities = new ArrayList<>();
        final double rootScale = 1 / Math.sqrt(roots.size());
        for (final OrgUnit root : roots) {
            root.collectWorkers(rootScale, contributions, affinities);
        }
        final List<Integer> order = new ArrayList<>();
        double total = 0;
        for (int i = 0; i < contributions.size(); i++) {
            order.add(i);
            total += contributions.get(i)[0];
        }
        // Best planners first: the highest planning multiplier relative to dispatch.
        order.sort(
            Comparator.comparingDouble(
                i -> -(affinities.get(i)
                    .multiplier(Department.PLANNING)
                    - affinities.get(i)
                        .multiplier(Department.DISPATCH))));
        final double share = Math.max(0, Math.min(1, planningShare));
        double toPlanning = share * total;
        double planning = 0;
        double dispatch = 0;
        for (final int i : order) {
            final double fte = contributions.get(i)[0];
            final Affinity affinity = affinities.get(i);
            final double planned = Math.min(fte, toPlanning);
            toPlanning -= planned;
            planning += planned * affinity.multiplier(Department.PLANNING);
            dispatch += (fte - planned) * affinity.multiplier(Department.DISPATCH);
        }
        return new Split(total, planning, dispatch);
    }
}
