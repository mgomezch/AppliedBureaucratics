package com.gtnewhorizons.appliedbureaucratics.integration.oc;

import net.minecraft.item.ItemStack;

import com.gtnewhorizons.appliedbureaucratics.item.ModItems;

import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverItem;

/// OpenComputers' driver for the Requisition Upgrade: a tier 1 upgrade for robots.
public class DriverRequisitionUpgrade extends DriverItem {

    public DriverRequisitionUpgrade() {
        super(new ItemStack(ModItems.requisitionUpgrade));
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        return host.world() != null && !host.world().isRemote ? new RequisitionUpgradeEnvironment(host) : null;
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 0;
    }
}
