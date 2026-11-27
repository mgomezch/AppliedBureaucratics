package com.gtnewhorizons.appliedbureaucratics.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/// A row of up to nine slots over the player's inventory, for blocks that are only storage.
public class ContainerSimple extends Container {

    final IInventory inventory;

    public ContainerSimple(final InventoryPlayer player, final IInventory inventory) {
        this.inventory = inventory;
        final int n = Math.min(9, inventory.getSizeInventory());
        for (int i = 0; i < n; i++) {
            this.addSlotToContainer(new Slot(inventory, i, 8 + i * 18, 20) {

                @Override
                public boolean isItemValid(final ItemStack stack) {
                    return this.inventory.isItemValidForSlot(this.getSlotIndex(), stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(player, col, 8 + col * 18, 109));
        }
    }

    @Override
    public boolean canInteractWith(final EntityPlayer player) {
        return this.inventory.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(final EntityPlayer player, final int index) {
        final Slot slot = (Slot) this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return null;
        }
        final int own = Math.min(9, this.inventory.getSizeInventory());
        final ItemStack stack = slot.getStack();
        final ItemStack original = stack.copy();
        if (index < own) {
            if (!this.mergeItemStack(stack, own, this.inventorySlots.size(), true)) {
                return null;
            }
        } else if (!this.inventory.isItemValidForSlot(0, stack) || !this.mergeItemStack(stack, 0, own, false)) {
            return null;
        }
        if (stack.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        return original;
    }
}
