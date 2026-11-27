package com.gtnewhorizons.appliedbureaucratics.integration.ae2;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.compute.PlanningPool;
import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.compute.ICraftingComputePolicy;
import appeng.api.networking.crafting.compute.ICraftingPlanningJob;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.data.IAEStack;

/// Our compute policy, registered with AE2 when [ABConfig#enableComputeRequirement] is on. Planning is metered by the
/// network's [IComputeGrid]; CPU blocks placed from now on are metered and weakened. See Note [The compute model].
public final class ComputePolicy implements ICraftingComputePolicy {

    /// What makes a request a repeat of an earlier one: the same output, from the same requester. See wrinkle (W3)
    /// of Note [The compute model].
    private record StandingKey(IAEStack<?> output, Object requester) {}

    private static @Nullable IComputeGrid compute(final ICraftingPlanningJob job) {
        final IGrid grid = job.getPlanningGrid();
        return grid == null ? null : grid.getCache(IComputeGrid.class);
    }

    private static Supplier<PlanningPool.Request> request(final ICraftingPlanningJob job) {
        return () -> {
            final BaseActionSource src = job.getPlanningSource();
            final Object requester;
            if (src instanceof PlayerSource player && player.player != null) {
                requester = player.player.getUniqueID();
            } else if (src instanceof MachineSource machine) {
                requester = machine.via;
            } else {
                requester = null;
            }
            final IAEStack<?> output = job.getPlanningOutput()
                .copy();
            output.setStackSize(1);
            return new PlanningPool.Request(
                src != null && src.isPlayer(),
                requester == null ? null : new StandingKey(output, requester));
        };
    }

    @Override
    public long grantPlanningSteps(final ICraftingPlanningJob job) {
        final IComputeGrid compute = compute(job);
        if (compute == null) {
            return Long.MAX_VALUE;
        }
        final PlanningPool<ICraftingPlanningJob> pool = compute.getPlanningPool();
        if (!pool.hasCapacity()) {
            return REFUSE;
        }
        return pool.grant(job, request(job));
    }

    @Override
    public void reportPlanningSteps(final ICraftingPlanningJob job, final long steps, final boolean overtime) {
        final IComputeGrid compute = compute(job);
        if (compute != null) {
            compute.getPlanningPool()
                .report(job, request(job), steps, overtime);
            compute.charge(steps);
        }
    }

    @Override
    public void planningEnded(final ICraftingPlanningJob job) {
        final IComputeGrid compute = compute(job);
        if (compute != null) {
            final boolean completed = !(job instanceof Future<?>future) || !future.isCancelled();
            compute.getPlanningPool()
                .ended(job, completed);
        }
    }

    @Override
    public @Nullable IChatComponent getPlanningStatus(final ICraftingPlanningJob job) {
        final IComputeGrid compute = compute(job);
        if (compute == null) {
            return null;
        }
        final PlanningPool<ICraftingPlanningJob> pool = compute.getPlanningPool();
        if (pool.isUnmetered()) {
            return null;
        }
        if (!pool.hasCapacity()) {
            return new ChatComponentTranslation("ab.planning.noCapacity");
        }
        if (pool.getCredit() < 0) {
            return new ChatComponentTranslation("ab.planning.overtime", (long) Math.ceil(-pool.getCredit()));
        }
        final String key = pool.isStandingOrder(job) ? "ab.planning.standingOrder" : "ab.planning.rate";
        return new ChatComponentTranslation(
            key,
            job.getPlanningStepsTaken(),
            job.getPlanningRate(),
            compute.getFacilities()
                .size());
    }

    @Override
    public boolean meterNewCraftingBlocks() {
        return ABConfig.meterNewCraftingBlocks;
    }

    @Override
    public long effectiveStorage(final long legacyBytes, final long meteredBytes) {
        return (long) (legacyBytes * ABConfig.legacyCpuFactor) + (long) (meteredBytes * ABConfig.meteredCpuFactor);
    }

    @Override
    public int effectiveCoProcessors(final int legacy, final int metered) {
        final int weakened = Math.min(ABConfig.meteredCoProcessorCap, (int) (metered * ABConfig.meteredCpuFactor));
        return (int) (legacy * ABConfig.legacyCpuFactor) + weakened;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof ComputePolicy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ComputePolicy.class);
    }
}
