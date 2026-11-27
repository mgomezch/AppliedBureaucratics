package com.gtnewhorizons.appliedbureaucratics.facility.datacenter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizons.appliedbureaucratics.item.ItemComponent;

/// An Accelerator Rack: part of a Data Center's hall, with four slots for Accelerator Cards. See Note [The Data
/// Center].
public class TileAcceleratorRack extends TileEntity implements IInventory {

    private final ItemStack[] cards = new ItemStack[4];

    public int cards() {
        int n = 0;
        for (final ItemStack card : this.cards) {
            if (card != null && ItemComponent.Kind.ACCELERATOR_CARD.is(card)) {
                n++;
            }
        }
        return n;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public int getSizeInventory() {
        return this.cards.length;
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        return this.cards[slot];
    }

    @Override
    public ItemStack decrStackSize(final int slot, final int count) {
        final ItemStack stack = this.cards[slot];
        if (stack == null) {
            return null;
        }
        final ItemStack taken = stack.splitStack(Math.min(count, stack.stackSize));
        if (stack.stackSize <= 0) {
            this.cards[slot] = null;
        }
        this.markDirty();
        return taken;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(final int slot) {
        return null;
    }

    @Override
    public void setInventorySlotContents(final int slot, final ItemStack stack) {
        this.cards[slot] = stack;
        this.markDirty();
    }

    @Override
    public String getInventoryName() {
        return "appliedbureaucratics.accelerator_rack";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(final EntityPlayer player) {
        return player.getDistanceSq(this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5) <= 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
        return ItemComponent.Kind.ACCELERATOR_CARD.is(stack);
    }

    @Override
    public void writeToNBT(final NBTTagCompound tag) {
        super.writeToNBT(tag);
        final NBTTagList list = new NBTTagList();
        for (int i = 0; i < this.cards.length; i++) {
            if (this.cards[i] != null) {
                final NBTTagCompound item = new NBTTagCompound();
                item.setByte("slot", (byte) i);
                this.cards[i].writeToNBT(item);
                list.appendTag(item);
            }
        }
        tag.setTag("cards", list);
    }

    @Override
    public void readFromNBT(final NBTTagCompound tag) {
        super.readFromNBT(tag);
        final NBTTagList list = tag.getTagList("cards", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            final NBTTagCompound item = list.getCompoundTagAt(i);
            final int slot = item.getByte("slot");
            if (slot >= 0 && slot < this.cards.length) {
                this.cards[slot] = ItemStack.loadItemStackFromNBT(item);
            }
        }
    }
}
