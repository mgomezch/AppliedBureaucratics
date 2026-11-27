package com.gtnewhorizons.appliedbureaucratics.compute;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/// The numbers of the design document's section 4, as implemented. See Note [The compute model].
class ComputeModelTest {

    private static final double EPS = 1e-9;

    @AfterEach
    void resetSettings() {
        ComputeSettings.set(new ComputeSettings());
    }

    private static List<OrgUnit> workers(int n, Affinity affinity) {
        final List<OrgUnit> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(OrgUnit.worker(1, affinity));
        }
        return list;
    }

    @Test
    void sixteenUnmanagedWorkersAreWorthFour() {
        assertEquals(4, OrgUnit.facilityFte(workers(16, Affinity.NONE)), EPS);
    }

    @Test
    void twoLeadsOverEightEach() {
        final double e = 0.8;
        final List<OrgUnit> roots = Arrays
            .asList(OrgUnit.manager(e, workers(8, Affinity.NONE)), OrgUnit.manager(e, workers(8, Affinity.NONE)));

        assertEquals(16 * e / Math.sqrt(2), OrgUnit.facilityFte(roots), EPS);
    }

    @Test
    void aDirectorOverTwoLeads() {
        final double lead = 0.8;
        final double director = 0.9;
        final OrgUnit root = OrgUnit.manager(
            director,
            Arrays.asList(
                OrgUnit.manager(lead, workers(8, Affinity.NONE)),
                OrgUnit.manager(lead, workers(8, Affinity.NONE))));

        assertEquals(16 * lead * director, OrgUnit.facilityFte(Collections.singletonList(root)), EPS);
    }

    @Test
    void aManagerCountsAtMostEightReports() {
        final OrgUnit lead = OrgUnit.manager(1, workers(12, Affinity.NONE));

        assertEquals(8, lead.fte(), EPS);
    }

    @Test
    void theSplitAssignsAffineWorkersFirst() {
        final List<OrgUnit> roots = new ArrayList<>();
        roots.add(OrgUnit.worker(1, Affinity.PLANNING));
        roots.add(OrgUnit.worker(1, Affinity.DISPATCH));
        roots.add(OrgUnit.worker(1, Affinity.NONE));
        roots.add(OrgUnit.worker(1, Affinity.NONE));
        // Four roots: each worker contributes 1 / 2.
        final Workforce.Split split = Workforce.split(roots, 0.25);

        assertAll(
            () -> assertEquals(2, split.facilityFte(), EPS),
            () -> assertEquals(0.5 * 1.5, split.planningFte(), EPS, "The librarian plans"),
            () -> assertEquals(0.5 * 1.5 + 0.5 + 0.5, split.dispatchFte(), EPS, "The farmer dispatches"));
    }

    @Test
    void moraleMultiplierPoints() {
        assertAll(
            () -> assertEquals(0.25, Morale.multiplier(0), EPS),
            () -> assertEquals(1.0, Morale.multiplier(60), EPS),
            () -> assertEquals(1.2, Morale.multiplier(100), EPS));
    }

    @Test
    void moraleDriftsAPointAMinute() {
        final Morale morale = new Morale();
        morale.drift(100, 3);
        assertEquals(63, morale.get(), EPS);
        morale.drift(62, 5);
        assertEquals(62, morale.get(), EPS);
    }

    @Test
    void outputNeverFallsBelowTheFloor() {
        final Workforce.Split split = new Workforce.Split(1, 1, 1);
        final FacilityOutput output = FacilityOutput.compute(split, Morale.multiplier(0), false, false, 1, 1);

        assertAll(
            () -> assertEquals(4 * 0.25, output.planningRate(), EPS, "No paper and no morale: still the floor"),
            () -> assertEquals(1 * 0.25, output.dispatchRate(), EPS));
    }

    @Test
    void aClerkWithPaperPlansFourStepsASecond() {
        final Workforce.Split split = Workforce.split(workers(1, Affinity.NONE), 1);
        final FacilityOutput output = FacilityOutput.compute(split, 1, true, false, 1, 1);

        assertAll(() -> assertEquals(4, output.planningRate(), EPS), () -> assertEquals(0, output.dispatchRate(), EPS));
    }

    @Test
    void oneOpASecondIsOneEveryTwentyTicks() {
        final DispatchMeter meter = new DispatchMeter();
        int total = 0;
        int firstTick = -1;
        for (int tick = 0; tick < 40; tick++) {
            final int granted = meter.grant(1);
            if (granted > 0 && firstTick < 0) {
                firstTick = tick;
            }
            total += granted;
        }

        assertEquals(2, total);
    }

    @Test
    void wagesAreOneUnitAWorkerADay() {
        final Payroll payroll = new Payroll();
        payroll.accrue(32, 24000);

        assertAll(() -> assertEquals(32, payroll.getOwed()), () -> assertEquals(4, payroll.emeraldsDue()));
        payroll.pay(4);
        assertTrue(payroll.isPaid());
    }

    @Test
    void anEmeraldPaysAheadForEightUnits() {
        final Payroll payroll = new Payroll();
        payroll.accrue(1, 24000);
        payroll.pay(1);
        payroll.accrue(1, 7 * 24000);

        assertAll(() -> assertTrue(payroll.isPaid()), () -> assertEquals(0, payroll.getPrepaid()));
        payroll.accrue(1, 24000);
        assertFalse(payroll.isPaid());
    }

    @Test
    void committeesDivideByTheSquareRoot() {
        assertEquals(8 / Math.sqrt(2) + 100, PlanningPool.networkRate(Arrays.asList(4.0, 4.0), 100), EPS);
    }
}
