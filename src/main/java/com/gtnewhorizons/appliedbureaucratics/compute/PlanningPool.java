package com.gtnewhorizons.appliedbureaucratics.compute;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/// One network's planning capacity, shared by the plans in progress on it. See Note [The compute model].
///
/// Credit, in steps, accrues every tick at the network's rate, up to [ComputeSettings#creditSeconds] of it.
/// [#tick] then allocates the credit to the plans in progress, half reserved for players' plans when machines' plans
/// compete with them (wrinkle (W2)). A plan takes its allowance with [#grant], and the steps it took are charged with
/// [#report]: a standing order at a discount (wrinkle (W3)), overtime in full even if that puts the network in debt
/// (wrinkle (W4)).
///
/// @param <J> the type of a plan; plans are told apart by `equals`
public final class PlanningPool<J> {

    /// Who asked for a plan, and the key that makes a repeat of it a standing order.
    public record Request(boolean player, Object standingKey) {}

    private static final class Plan {

        final Request request;
        final boolean standing;
        long allowance;

        Plan(final Request request, final boolean standing) {
            this.request = request;
            this.standing = standing;
        }
    }

    private final Map<J, Plan> plans = new LinkedHashMap<>();
    private final ArrayDeque<Object> recent = new ArrayDeque<>();
    private double credit;
    private double rate;
    private boolean unmetered;
    private boolean couldPlan;

    /// A network's planning rate from its facilities' rates: staffed facilities plan by committee, their sum divided
    /// by the square root of their number (wrinkle (W5)); machine facilities add in full.
    public static double networkRate(final List<Double> staffed, final double machines) {
        double sum = 0;
        for (final double rate : staffed) {
            sum += rate;
        }
        final double committee = staffed.isEmpty() ? 0 : sum / Math.sqrt(staffed.size());
        return committee + machines;
    }

    /// Sets the network's planning rate, in steps per second.
    public void setRate(final double stepsPerSecond) {
        this.rate = Math.max(0, stepsPerSecond);
    }

    public double getRate() {
        return this.rate;
    }

    /// Whether plans on this network are not metered at all, as on a network with legacy crafting CPUs.
    public void setUnmetered(final boolean unmetered) {
        this.unmetered = unmetered;
    }

    public boolean isUnmetered() {
        return this.unmetered;
    }

    /// Planning credit in steps. Negative while the network pays off overtime.
    public double getCredit() {
        return this.credit;
    }

    public void setCredit(final double credit) {
        this.credit = credit;
    }

    /// Whether plans can ever advance here: false for a network with no planning rate and no credit left.
    public boolean hasCapacity() {
        return this.unmetered || this.rate > 0 || this.credit >= 1 || this.couldPlan;
    }

    /// Whether some facility on the network would plan once a plan is waiting, though the rate is 0 now (its slots
    /// are busy). A plan that arrives then waits instead of being refused, and its arrival turns that planning on.
    public void setCouldPlan(final boolean couldPlan) {
        this.couldPlan = couldPlan;
    }

    /// Accrues a tick's credit and allocates it to the plans in progress.
    public void tick() {
        final ComputeSettings settings = ComputeSettings.get();
        this.credit = Math.min(this.credit + this.rate / 20, Math.max(1, this.rate * settings.creditSeconds));
        this.allocate();
    }

    private void allocate() {
        final List<Plan> players = new ArrayList<>();
        final List<Plan> machines = new ArrayList<>();
        for (final Plan plan : this.plans.values()) {
            plan.allowance = 0;
            (plan.request.player() ? players : machines).add(plan);
        }
        if (this.credit < 1 || this.plans.isEmpty()) {
            return;
        }
        double playerShare = this.credit;
        double machineShare = this.credit;
        if (!players.isEmpty() && !machines.isEmpty()) {
            playerShare = this.credit * ComputeSettings.get().playerReservation;
            machineShare = this.credit - playerShare;
        }
        share(players, playerShare);
        share(machines, machineShare);
    }

    private static void share(final List<Plan> group, final double credit) {
        if (group.isEmpty()) {
            return;
        }
        final double each = credit / group.size();
        for (final Plan plan : group) {
            plan.allowance = (long) Math.floor(each / charge(plan));
        }
    }

    private static double charge(final Plan plan) {
        return plan.standing ? ComputeSettings.get().standingOrderCharge : 1;
    }

    /// The steps `job` may take now, or [Long#MAX_VALUE] if the network is unmetered. Registers the plan the first
    /// time; it gets its first allowance at the next [#tick].
    public long grant(final J job, final Supplier<Request> request) {
        if (this.unmetered) {
            return Long.MAX_VALUE;
        }
        final Plan plan = this.register(job, request);
        final long allowance = plan.allowance;
        plan.allowance = 0;
        return allowance;
    }

    /// Charges the steps `job` took. Overtime is charged the same way, and may put the network in debt.
    public void report(final J job, final Supplier<Request> request, final long steps, final boolean overtime) {
        if (this.unmetered || steps <= 0) {
            return;
        }
        final Plan plan = this.register(job, request);
        this.credit -= steps * charge(plan);
    }

    /// Forgets `job`. A plan that completed is remembered as a request a later one may repeat.
    public void ended(final J job, final boolean completed) {
        final Plan plan = this.plans.remove(job);
        if (plan != null && completed && plan.request.standingKey() != null) {
            this.recent.remove(plan.request.standingKey());
            this.recent.addLast(plan.request.standingKey());
            while (this.recent.size() > ComputeSettings.get().standingOrderMemory) {
                this.recent.removeFirst();
            }
        }
    }

    public boolean isStandingOrder(final J job) {
        final Plan plan = this.plans.get(job);
        return plan != null && plan.standing;
    }

    public int planCount() {
        return this.plans.size();
    }

    /// Registers `job` the first time it is seen; `request` is asked only then.
    private Plan register(final J job, final Supplier<Request> request) {
        Plan plan = this.plans.get(job);
        if (plan == null) {
            final Request asked = request.get();
            final boolean standing = asked.standingKey() != null && this.recent.contains(asked.standingKey());
            plan = new Plan(asked, standing);
            this.plans.put(job, plan);
        }
        return plan;
    }
}
