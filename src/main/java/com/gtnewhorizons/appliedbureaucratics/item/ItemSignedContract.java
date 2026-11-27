package com.gtnewhorizons.appliedbureaucratics.item;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.workforce.WorkforceActions;

/// A contract signed by a worker. Right-click a seat to assign the worker to it. It stays with the player as the
/// worker's file, so the worker can be moved again.
public class ItemSignedContract extends ItemAB {

    public ItemSignedContract(final String name) {
        super(name);
        this.setMaxStackSize(1);
    }

    public static ItemStack forWorker(final ItemStack stack, final UUID worker, final String name, final String title,
        final boolean manager) {
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("workerHi", worker.getMostSignificantBits());
        tag.setLong("workerLo", worker.getLeastSignificantBits());
        tag.setString("name", name);
        tag.setString("title", title);
        tag.setBoolean("manager", manager);
        stack.setTagCompound(tag);
        return stack;
    }

    public static @Nullable UUID worker(final ItemStack stack) {
        final NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("workerHi")) {
            return null;
        }
        return new UUID(tag.getLong("workerHi"), tag.getLong("workerLo"));
    }

    @Override
    public boolean onItemUse(final ItemStack stack, final EntityPlayer player, final World world, final int x,
        final int y, final int z, final int side, final float hitX, final float hitY, final float hitZ) {
        if (world.isRemote) {
            return true;
        }
        return WorkforceActions.assign(player, stack, world, x, y, z);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(final ItemStack stack, final EntityPlayer player, final List lines,
        final boolean advanced) {
        final NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("name")) {
            lines.add("§e" + tag.getString("name") + "§r, " + tag.getString("title"));
        }
        super.addInformation(stack, player, lines, advanced);
    }
}
