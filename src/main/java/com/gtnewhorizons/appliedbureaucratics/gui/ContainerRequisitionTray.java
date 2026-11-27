package com.gtnewhorizons.appliedbureaucratics.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.appliedbureaucratics.facility.desk.TileRequisitionTray;

/// The Requisition Tray's screen: its paper and payroll slots, and its figures.
public class ContainerRequisitionTray extends Container {

    public static final int BOTTLENECK = 0;
    public static final int PLANNING_RATE_X10 = 1;
    public static final int DISPATCH_RATE_X10 = 2;
    public static final int FILING = 3;
    public static final int FILING_USED = 4;
    public static final int MORALE = 5;
    public static final int PAID = 6;
    public static final int WORKER = 7;
    public static final int BUSY = 8;
    /// Bits: 1 crunching, 2 coffee, 4 a party.
    public static final int FLAGS = 9;

    final TileRequisitionTray tray;
    final SyncedInts values = new SyncedInts(10);

    public ContainerRequisitionTray(final InventoryPlayer player, final TileRequisitionTray tray) {
        this.tray = tray;
        this.addSlotToContainer(new FilteredSlot(tray, TileRequisitionTray.SLOT_PAPER, 26, 72));
        this.addSlotToContainer(new FilteredSlot(tray, TileRequisitionTray.SLOT_PAYROLL, 134, 72));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, 102 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(player, col, 8 + col * 18, 160));
        }
    }

    @Override
    public boolean canInteractWith(final EntityPlayer player) {
        return this.tray.isUseableByPlayer(player);
    }

    @Override
    public void addCraftingToCrafters(final ICrafting crafter) {
        super.addCraftingToCrafters(crafter);
        this.update();
        this.values.send(this, java.util.Collections.singletonList(crafter), true);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        this.update();
        this.values.send(this, this.crafters, false);
    }

    private void update() {
        this.values.set(
            BOTTLENECK,
            this.tray.getBottleneck()
                .ordinal());
        this.values.set(
            PLANNING_RATE_X10,
            (int) Math.round(
                this.tray.getOutput()
                    .planningRate() * 10));
        this.values.set(
            DISPATCH_RATE_X10,
            (int) Math.round(
                this.tray.getOutput()
                    .dispatchRate() * 10));
        this.values.set(
            FILING,
            (int) Math.min(
                Integer.MAX_VALUE,
                this.tray.getSlots()
                    .getFiling()));
        this.values.set(
            FILING_USED,
            (int) Math.min(
                Integer.MAX_VALUE,
                this.tray.getSlots()
                    .getUsedFiling()));
        this.values.set(
            MORALE,
            (int) Math.round(
                this.tray.getMorale()
                    .get()));
        this.values.set(PAID, this.tray.isPaid() ? 1 : 0);
        this.values.set(
            WORKER,
            this.tray.getSurvey()
                .worker() != null ? 1
                    : this.tray.getSurvey()
                        .founder() != null ? 2 : 0);
        this.updateFlags();
        this.values.set(
            BUSY,
            this.tray.getSlots()
                .getBusyCount());
    }

    private void updateFlags() {
        this.values.set(
            FLAGS,
            (this.tray.isCrunching() ? 1 : 0) | (this.tray.hasCoffee() ? 2 : 0) | (this.tray.isPartying() ? 4 : 0));
    }

    @Override
    public void updateProgressBar(final int id, final int value) {
        this.values.receive(id, value);
    }

    @Override
    public ItemStack transferStackInSlot(final EntityPlayer player, final int index) {
        final Slot slot = (Slot) this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return null;
        }
        final ItemStack stack = slot.getStack();
        final ItemStack original = stack.copy();
        if (index < 2) {
            if (!this.mergeItemStack(stack, 2, this.inventorySlots.size(), true)) {
                return null;
            }
        } else {
            final int target = this.tray.isItemValidForSlot(TileRequisitionTray.SLOT_PAPER, stack) ? 0
                : this.tray.isItemValidForSlot(TileRequisitionTray.SLOT_PAYROLL, stack) ? 1 : -1;
            if (target < 0 || !this.mergeItemStack(stack, target, target + 1, false)) {
                return null;
            }
        }
        if (stack.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        return original;
    }

    private static final class FilteredSlot extends Slot {

        FilteredSlot(final TileRequisitionTray tray, final int index, final int x, final int y) {
            super(tray, index, x, y);
        }

        @Override
        public boolean isItemValid(final ItemStack stack) {
            return this.inventory.isItemValidForSlot(this.getSlotIndex(), stack);
        }
    }
}
