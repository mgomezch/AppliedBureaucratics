package com.gtnewhorizons.appliedbureaucratics.integration.oc;

import net.minecraft.util.MathHelper;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ManagedEnvironment;

/// The Requisition Upgrade's component, `requisition`, in a robot. Its one callback, `work()`, clocks the robot in.
/// See Note [Robots].
public class RequisitionUpgradeEnvironment extends ManagedEnvironment {

    private final EnvironmentHost host;

    public RequisitionUpgradeEnvironment(final EnvironmentHost host) {
        this.host = host;
        this.setNode(
            Network.newNode(this, Visibility.Neighbors)
                .withComponent("requisition")
                .create());
    }

    @Callback(doc = "function():boolean -- Clocks this robot in at its Docking Pad. Call it at least every 5 seconds.")
    public Object[] work(final Context context, final Arguments args) {
        if (this.host.world() == null) {
            return new Object[] { false };
        }
        BotRegistry.clockIn(
            this.host.world(),
            MathHelper.floor_double(this.host.xPosition()),
            MathHelper.floor_double(this.host.yPosition()),
            MathHelper.floor_double(this.host.zPosition()));
        return new Object[] { true };
    }
}
