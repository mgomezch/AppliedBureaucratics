package com.gtnewhorizons.appliedbureaucratics.facility.breakroom;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;
import com.gtnewhorizons.appliedbureaucratics.config.ItemList;

/// The Break Room Counter: "in lieu of a raise." Stocked with coffee, it adds to the morale of every facility within
/// [ABConfig#breakRoomRange] blocks; stocked with party food, it throws a pizza party that lasts a day. Buses and AE2
/// can keep it stocked.
public class TileBreakRoomCounter extends TileEntity implements IInventory {

    private static final WeakHashMap<World, Set<TileBreakRoomCounter>> COUNTERS = new WeakHashMap<>();

    private final ItemStack[] inventory = new ItemStack[9];
    /// Worker-ticks of coffee owed and not yet taken from the counter.
    private long coffeeOwed;
    private boolean hasCoffee;
    private long partyUntil;

    /// The counter within range of `x, y, z`, if any: the first that has coffee, or else any.
    public static @Nullable TileBreakRoomCounter near(final World world, final int x, final int y, final int z) {
        final Set<TileBreakRoomCounter> counters = COUNTERS.get(world);
        if (counters == null) {
            return null;
        }
        final double range = ABConfig.breakRoomRange;
        TileBreakRoomCounter found = null;
        for (final TileBreakRoomCounter counter : counters) {
            if (counter.isInvalid() || counter.getDistanceFrom(x + 0.5, y + 0.5, z + 0.5) > range * range) {
                continue;
            }
            if (counter.hasCoffee() || counter.isPartying()) {
                return counter;
            }
            found = counter;
        }
        return found;
    }

    @Override
    public void validate() {
        super.validate();
        if (this.worldObj != null && !this.worldObj.isRemote) {
            COUNTERS.computeIfAbsent(this.worldObj, w -> Collections.newSetFromMap(new WeakHashMap<>()))
                .add(this);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        final Set<TileBreakRoomCounter> counters = this.worldObj == null ? null : COUNTERS.get(this.worldObj);
        if (counters != null) {
            counters.remove(this);
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        this.invalidate();
    }

    @Override
    public void setWorldObj(final World world) {
        super.setWorldObj(world);
        if (!this.isInvalid()) {
            this.validate();
        }
    }

    public boolean hasCoffee() {
        return this.hasCoffee;
    }

    /// Serves `workers` workers coffee for `ticks` ticks: one cup per [ABConfig#coffeeWorkerDays] worker-days. Says
    /// whether there is coffee.
    public boolean serveCoffee(final int workers, final long ticks) {
        this.coffeeOwed += (long) workers * ticks;
        final long perCup = (long) ABConfig.coffeeWorkerDays * 24000;
        while (this.coffeeOwed >= perCup) {
            if (!this.take(ItemList.COFFEE)) {
                this.coffeeOwed = Math.min(this.coffeeOwed, perCup);
                break;
            }
            this.coffeeOwed -= perCup;
        }
        this.hasCoffee = this.find(ItemList.COFFEE) >= 0;
        return this.hasCoffee;
    }

    public boolean isPartying() {
        return this.worldObj != null && this.worldObj.getTotalWorldTime() < this.partyUntil;
    }

    /// Whether a pizza party is on; starts one with the counter's party food if none is.
    public boolean party() {
        if (this.isPartying()) {
            return true;
        }
        if (this.take(ItemList.PARTY_FOOD)) {
            this.partyUntil = this.worldObj.getTotalWorldTime() + 24000;
            this.markDirty();
            return true;
        }
        return false;
    }

    private int find(final ItemList list) {
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] != null && list.matches(this.inventory[i])) {
                return i;
            }
        }
        return -1;
    }

    private boolean take(final ItemList list) {
        final int slot = this.find(list);
        if (slot < 0) {
            return false;
        }
        this.decrStackSize(slot, 1);
        return true;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void writeToNBT(final NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setLong("coffeeOwed", this.coffeeOwed);
        tag.setLong("partyUntil", this.partyUntil);
        final NBTTagList items = new NBTTagList();
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] != null) {
                final NBTTagCompound item = new NBTTagCompound();
                item.setByte("slot", (byte) i);
                this.inventory[i].writeToNBT(item);
                items.appendTag(item);
            }
        }
        tag.setTag("items", items);
    }

    @Override
    public void readFromNBT(final NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.coffeeOwed = tag.getLong("coffeeOwed");
        this.partyUntil = tag.getLong("partyUntil");
        final NBTTagList items = tag.getTagList("items", 10);
        for (int i = 0; i < items.tagCount(); i++) {
            final NBTTagCompound item = items.getCompoundTagAt(i);
            final int slot = item.getByte("slot");
            if (slot >= 0 && slot < this.inventory.length) {
                this.inventory[slot] = ItemStack.loadItemStackFromNBT(item);
            }
        }
        this.hasCoffee = this.find(ItemList.COFFEE) >= 0;
    }

    @Override
    public int getSizeInventory() {
        return this.inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        return this.inventory[slot];
    }

    @Override
    public ItemStack decrStackSize(final int slot, final int count) {
        final ItemStack stack = this.inventory[slot];
        if (stack == null) {
            return null;
        }
        final ItemStack taken = stack.splitStack(Math.min(count, stack.stackSize));
        if (stack.stackSize <= 0) {
            this.inventory[slot] = null;
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
        this.inventory[slot] = stack;
        if (stack != null && ItemList.COFFEE.matches(stack)) {
            this.hasCoffee = true;
        }
        this.markDirty();
    }

    @Override
    public String getInventoryName() {
        return "appliedbureaucratics.break_room_counter";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(final EntityPlayer player) {
        return this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) == this
            && player.getDistanceSq(this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5) <= 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
        return ItemList.COFFEE.matches(stack) || ItemList.PARTY_FOOD.matches(stack);
    }
}
