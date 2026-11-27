package com.gtnewhorizons.appliedbureaucratics.compute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// Builds a facility's organisation from where its people sit. See Note [The compute model] and the design
/// document, section 4.2.
///
/// Each manager has roles ([#LEAD], [#DIRECT], [#EXECUTE]). Team leads are placed first, best first: each takes up to
/// [ComputeSettings#maxReports] of the nearest unmanaged workers within [#LEAD_RANGE] blocks. Directors then take up to
/// as many of the nearest leads within [#DIRECTOR_RANGE] blocks, where the facility allows directors. Executives,
/// where the facility allows them (the Internet's boardroom), take directors the same way; an executive with no
/// directors to take leads workers instead, if it can. Everyone left over is a root of their own, and managers with
/// nobody to manage do nothing.
public final class OrgChart {

    public static final double LEAD_RANGE = 12;
    public static final double DIRECTOR_RANGE = 24;

    public static final int LEAD = 1;
    public static final int DIRECT = 2;
    public static final int EXECUTE = 4;

    public record Worker(double x, double y, double z, double fte, Affinity affinity) {}

    /// @param roles bits of [#LEAD], [#DIRECT] and [#EXECUTE]
    /// @param paid an unpaid manager works at efficiency 0.5
    public record Manager(double x, double y, double z, double efficiency, int roles, boolean paid) {

        double effective() {
            return this.paid ? this.efficiency : 0.5;
        }

        boolean can(final int role) {
            return (this.roles & role) != 0;
        }
    }

    /// The organisation: its roots, and how many managers sit at each level.
    public record Result(List<OrgUnit> roots, int leads, int directors, int executives, int unmanaged) {}

    private OrgChart() {}

    private static double distanceSq(final double ax, final double ay, final double az, final double bx,
        final double by, final double bz) {
        final double dx = ax - bx;
        final double dy = ay - by;
        final double dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    private record Unit(OrgUnit org, double x, double y, double z) {}

    /// @param maxLevel the highest management level the facility allows: 2 for a Mainframe office, 3 with a boardroom
    public static Result build(final List<Worker> workers, final List<Manager> managers, final int maxLevel) {
        final int maxReports = ComputeSettings.get().maxReports;
        final List<Manager> ordered = new ArrayList<>(managers);
        ordered.sort(Comparator.comparingDouble(m -> -m.effective()));
        final List<Worker> free = new ArrayList<>(workers);
        final List<Unit> leads = new ArrayList<>();
        final List<Manager> executives = new ArrayList<>();
        for (final Manager manager : ordered) {
            if (manager.can(EXECUTE) && maxLevel >= 3) {
                executives.add(manager);
            } else if (manager.can(LEAD)) {
                lead(manager, free, leads, maxReports);
            }
        }
        final List<Unit> directors = new ArrayList<>();
        if (maxLevel >= 2) {
            for (final Manager manager : ordered) {
                if (manager.can(DIRECT)) {
                    takeUnits(manager, leads, directors, maxReports);
                }
            }
        }
        final List<Unit> top = new ArrayList<>();
        final List<Unit> lateLeads = new ArrayList<>();
        for (final Manager manager : executives) {
            if (!takeUnits(manager, directors, top, maxReports) && manager.can(LEAD)) {
                lead(manager, free, lateLeads, maxReports);
            }
        }
        final int leadCount = leads.size() + lateLeads.size() + reportsOf(directors) + reportsOfReports(top);
        final int directorCount = directors.size() + reportsOf(top);
        final List<OrgUnit> roots = new ArrayList<>();
        for (final List<Unit> level : java.util.Arrays.asList(top, directors, leads, lateLeads)) {
            for (final Unit u : level) {
                roots.add(u.org);
            }
        }
        for (final Worker w : free) {
            roots.add(OrgUnit.worker(w.fte, w.affinity));
        }
        return new Result(roots, leadCount, directorCount, top.size(), free.size());
    }

    private static void lead(final Manager manager, final List<Worker> free, final List<Unit> leads,
        final int maxReports) {
        free.sort(Comparator.comparingDouble(w -> distanceSq(w.x, w.y, w.z, manager.x, manager.y, manager.z)));
        final List<OrgUnit> team = new ArrayList<>();
        while (team.size() < maxReports && !free.isEmpty()
            && distanceSq(free.get(0).x, free.get(0).y, free.get(0).z, manager.x, manager.y, manager.z)
                <= LEAD_RANGE * LEAD_RANGE) {
            final Worker w = free.remove(0);
            team.add(OrgUnit.worker(w.fte, w.affinity));
        }
        if (!team.isEmpty()) {
            leads.add(new Unit(OrgUnit.manager(manager.effective(), team), manager.x, manager.y, manager.z));
        }
    }

    private static int reportsOfReports(final List<Unit> units) {
        int n = 0;
        for (final Unit u : units) {
            for (final OrgUnit r : u.org.getReports()) {
                n += r.getReports()
                    .size();
            }
        }
        return n;
    }

    private static int reportsOf(final List<Unit> units) {
        int n = 0;
        for (final Unit u : units) {
            n += u.org.getReports()
                .size();
        }
        return n;
    }

    /// `manager` takes up to [ComputeSettings#maxReports] of the nearest units in `below` within the director range,
    /// which move into its new unit in `above`. Returns whether it took any.
    private static boolean takeUnits(final Manager manager, final List<Unit> below, final List<Unit> above,
        final int maxReports) {
        if (below.isEmpty()) {
            return false;
        }
        below.sort(Comparator.comparingDouble(u -> distanceSq(u.x, u.y, u.z, manager.x, manager.y, manager.z)));
        final List<OrgUnit> reports = new ArrayList<>();
        while (reports.size() < maxReports && !below.isEmpty()
            && distanceSq(below.get(0).x, below.get(0).y, below.get(0).z, manager.x, manager.y, manager.z)
                <= DIRECTOR_RANGE * DIRECTOR_RANGE) {
            reports.add(below.remove(0).org);
        }
        if (reports.isEmpty()) {
            return false;
        }
        above.add(new Unit(OrgUnit.manager(manager.effective(), reports), manager.x, manager.y, manager.z));
        return true;
    }
}
