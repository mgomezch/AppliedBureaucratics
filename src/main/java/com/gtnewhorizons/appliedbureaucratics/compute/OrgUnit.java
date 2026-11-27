package com.gtnewhorizons.appliedbureaucratics.compute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A unit of a facility's organisation: a worker, or a manager over other units. See Note [The compute model].
///
/// A managed unit's FTE is the sum of its reports' FTE times the manager's efficiency. A manager's own work is
/// nothing: managers produce nothing and are required.
public final class OrgUnit {

    private final boolean manager;
    private final double fte;
    private final Affinity affinity;
    private final double efficiency;
    private final List<OrgUnit> reports;

    private OrgUnit(final boolean manager, final double fte, final Affinity affinity, final double efficiency,
        final List<OrgUnit> reports) {
        this.manager = manager;
        this.fte = fte;
        this.affinity = affinity;
        this.efficiency = efficiency;
        this.reports = reports;
    }

    public static OrgUnit worker(final double fte, final Affinity affinity) {
        return new OrgUnit(false, fte, affinity, 1, Collections.emptyList());
    }

    /// A manager with efficiency `efficiency` over `reports`, of which only the first
    /// [ComputeSettings#maxReports] count.
    public static OrgUnit manager(final double efficiency, final List<OrgUnit> reports) {
        final int max = ComputeSettings.get().maxReports;
        final List<OrgUnit> kept = new ArrayList<>(reports.subList(0, Math.min(max, reports.size())));
        return new OrgUnit(true, 0, Affinity.NONE, efficiency, Collections.unmodifiableList(kept));
    }

    public boolean isManager() {
        return this.manager;
    }

    public double getEfficiency() {
        return this.efficiency;
    }

    public List<OrgUnit> getReports() {
        return this.reports;
    }

    public Affinity getAffinity() {
        return this.affinity;
    }

    /// This unit's FTE: a worker's own, or a manager's reports' times its efficiency.
    public double fte() {
        if (!this.manager) {
            return this.fte;
        }
        double sum = 0;
        for (final OrgUnit report : this.reports) {
            sum += report.fte();
        }
        return sum * this.efficiency;
    }

    /// The FTE of a facility whose organisation has these roots: their sum over the square root of their number.
    public static double facilityFte(final List<OrgUnit> roots) {
        if (roots.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (final OrgUnit root : roots) {
            sum += root.fte();
        }
        return sum / Math.sqrt(roots.size());
    }

    /// Adds this unit's workers to `out`, each with the factor its FTE is scaled by through its managers.
    void collectWorkers(final double scale, final List<double[]> out, final List<Affinity> affinities) {
        if (!this.manager) {
            if (this.fte > 0) {
                out.add(new double[] { this.fte * scale });
                affinities.add(this.affinity);
            }
            return;
        }
        for (final OrgUnit report : this.reports) {
            report.collectWorkers(scale * this.efficiency, out, affinities);
        }
    }
}
