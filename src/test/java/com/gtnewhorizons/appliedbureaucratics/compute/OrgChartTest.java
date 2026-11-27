package com.gtnewhorizons.appliedbureaucratics.compute;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/// The organisation rules of the design document, section 4.2, from seat positions.
class OrgChartTest {

    private static final double EPS = 1e-9;

    private static List<OrgChart.Worker> row(int n, double x0, double z) {
        final List<OrgChart.Worker> workers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            workers.add(new OrgChart.Worker(x0 + i, 0, z, 1, Affinity.NONE));
        }
        return workers;
    }

    @Test
    void sixteenUnmanagedWorkersAreWorthFour() {
        final OrgChart.Result r = OrgChart.build(row(16, 0, 0), Collections.emptyList(), 2);

        assertAll(() -> assertEquals(4, OrgUnit.facilityFte(r.roots()), EPS), () -> assertEquals(16, r.unmanaged()));
    }

    @Test
    void twoEndermenLeadEightEach() {
        final List<OrgChart.Worker> workers = new ArrayList<>(row(8, 0, 0));
        workers.addAll(row(8, 0, 40));
        final List<OrgChart.Manager> managers = Arrays.asList(
            new OrgChart.Manager(4, 0, 2, 0.8, OrgChart.LEAD, true),
            new OrgChart.Manager(4, 0, 42, 0.8, OrgChart.LEAD, true));

        final OrgChart.Result r = OrgChart.build(workers, managers, 2);

        assertAll(
            () -> assertEquals(2, r.leads()),
            () -> assertEquals(0, r.unmanaged()),
            () -> assertEquals(16 * 0.8 / Math.sqrt(2), OrgUnit.facilityFte(r.roots()), EPS));
    }

    @Test
    void aBossDirectsTwoLeads() {
        final List<OrgChart.Worker> workers = new ArrayList<>(row(8, 0, 0));
        workers.addAll(row(8, 0, 20));
        final List<OrgChart.Manager> managers = Arrays.asList(
            new OrgChart.Manager(4, 0, 2, 0.8, OrgChart.LEAD, true),
            new OrgChart.Manager(4, 0, 22, 0.8, OrgChart.LEAD, true),
            new OrgChart.Manager(4, 0, 11, 0.9, OrgChart.DIRECT, true));

        final OrgChart.Result r = OrgChart.build(workers, managers, 2);

        assertAll(
            () -> assertEquals(1, r.directors()),
            () -> assertEquals(
                1,
                r.roots()
                    .size()),
            () -> assertEquals(16 * 0.8 * 0.9, OrgUnit.facilityFte(r.roots()), EPS));
    }

    @Test
    void anUnpaidManagerWorksAtHalf() {
        final OrgChart.Result r = OrgChart.build(
            row(8, 0, 0),
            Collections.singletonList(new OrgChart.Manager(4, 0, 2, 1.0, OrgChart.LEAD | OrgChart.EXECUTE, false)),
            3);

        assertEquals(4, OrgUnit.facilityFte(r.roots()), EPS);
    }

    @Test
    void workersOutOfRangeStayUnmanaged() {
        final OrgChart.Result r = OrgChart.build(
            row(2, 100, 100),
            Collections.singletonList(new OrgChart.Manager(0, 0, 0, 0.8, OrgChart.LEAD, true)),
            2);

        assertAll(() -> assertEquals(2, r.unmanaged()), () -> assertEquals(0, r.leads()));
    }

    @Test
    void anOfficeAllowsNoExecutives() {
        final List<OrgChart.Worker> workers = new ArrayList<>(row(8, 0, 0));
        final List<OrgChart.Manager> managers = Arrays.asList(
            new OrgChart.Manager(4, 0, 2, 0.8, OrgChart.LEAD, true),
            new OrgChart.Manager(4, 0, 6, 1.0, OrgChart.LEAD | OrgChart.EXECUTE, true),
            new OrgChart.Manager(4, 0, 8, 1.0, OrgChart.LEAD | OrgChart.EXECUTE, true));

        final OrgChart.Result office = OrgChart.build(workers, managers, 2);

        assertEquals(0, office.executives());
    }

    @Test
    void aLeadCannotDirect() {
        final List<OrgChart.Worker> workers = new ArrayList<>(row(8, 0, 0));
        workers.addAll(row(8, 0, 20));
        final List<OrgChart.Manager> managers = Arrays.asList(
            new OrgChart.Manager(4, 0, 2, 0.8, OrgChart.LEAD, true),
            new OrgChart.Manager(4, 0, 22, 0.8, OrgChart.LEAD, true),
            new OrgChart.Manager(4, 0, 11, 0.9, OrgChart.LEAD, true));

        assertEquals(
            0,
            OrgChart.build(workers, managers, 2)
                .directors(),
            "Directors must be bosses");
    }

    @Test
    void aDemonInTheBoardroomRunsTheDirectors() {
        final List<OrgChart.Worker> workers = new ArrayList<>(row(8, 0, 0));
        workers.addAll(row(8, 0, 20));
        final List<OrgChart.Manager> managers = Arrays.asList(
            new OrgChart.Manager(4, 0, 2, 0.8, OrgChart.LEAD, true),
            new OrgChart.Manager(4, 0, 22, 0.8, OrgChart.LEAD, true),
            new OrgChart.Manager(4, 0, 11, 0.95, OrgChart.DIRECT, true),
            new OrgChart.Manager(4, 0, 12, 1.0, OrgChart.LEAD | OrgChart.EXECUTE, true));

        final OrgChart.Result r = OrgChart.build(workers, managers, 3);

        assertAll(
            () -> assertEquals(1, r.executives()),
            () -> assertEquals(1, r.directors()),
            () -> assertEquals(2, r.leads()),
            () -> assertEquals(16 * 0.8 * 0.95 * 1.0, OrgUnit.facilityFte(r.roots()), EPS));
    }
}
