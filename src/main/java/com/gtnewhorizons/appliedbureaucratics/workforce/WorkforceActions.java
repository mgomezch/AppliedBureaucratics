package com.gtnewhorizons.appliedbureaucratics.workforce;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;
import com.gtnewhorizons.appliedbureaucratics.integration.bibliocraft.Furniture;
import com.gtnewhorizons.appliedbureaucratics.item.ItemSignedContract;
import com.gtnewhorizons.appliedbureaucratics.item.ModItems;

/// Hiring, assigning and releasing workers. See Note [Contracted mobs are held by mixins].
public final class WorkforceActions {

    private WorkforceActions() {}

    private static void tell(final EntityPlayer player, final String key, final Object... args) {
        player.addChatMessage(new ChatComponentTranslation("chat.appliedbureaucratics." + key, args));
    }

    /// Signs `target` with the contract `held`, and gives the player the signed contract. Villagers sign
    /// employment contracts; managers ([ManagerKind]) sign management contracts.
    public static boolean hire(final EntityPlayer player, final ItemStack held, final EntityLivingBase target) {
        final boolean management = held.getItem() == ModItems.managementContract;
        final ManagerKind kind = ManagerKind.of(target);
        if (management ? kind == null : !(target instanceof EntityVillager)) {
            tell(player, management ? "notAManager" : "notAWorker");
            return true;
        }
        final Employment existing = Employment.of(target);
        if (existing != null) {
            tell(player, "alreadyEmployed", existing.getName());
            return true;
        }
        final UUID id = target.getUniqueID();
        final int profession = target instanceof EntityVillager villager ? villager.getProfession() : -1;
        final String name = WorkerNames.name(id);
        final String title = WorkerNames.title(id, profession, management);
        Employment.set(
            target,
            new Employment(
                player.getUniqueID(),
                management ? Employment.Role.MANAGER : Employment.Role.WORKER,
                name,
                title,
                target.worldObj.getTotalWorldTime()));
        if (target instanceof EntityLiving living) {
            living.setCustomNameTag(name + ", " + title);
            living.func_110163_bv(); // enablePersistence
        }
        if (!player.capabilities.isCreativeMode) {
            held.stackSize--;
        }
        final ItemStack signed = ItemSignedContract
            .forWorker(new ItemStack(ModItems.signedContract), id, name, title, management);
        if (!player.inventory.addItemStackToInventory(signed)) {
            player.dropPlayerItemWithRandomChoice(signed, false);
        }
        tell(player, "hired", name, title);
        return true;
    }

    /// Releases `target` with a pink slip: its normal behaviour returns.
    public static boolean release(final EntityPlayer player, final ItemStack held, final EntityLivingBase target) {
        final Employment employment = Employment.of(target);
        if (employment == null) {
            tell(player, "notEmployed");
            return true;
        }
        if (!employment.getEmployer()
            .equals(player.getUniqueID()) && !player.capabilities.isCreativeMode) {
            tell(player, "notYourEmployee", employment.getName());
            return true;
        }
        if (target.ridingEntity instanceof EntityWorkstationMount) {
            target.mountEntity(null);
        }
        Employment.set(target, null);
        if (target instanceof EntityLiving living && living.getCustomNameTag()
            .startsWith(employment.getName() + ", ")) {
            living.setCustomNameTag("");
        }
        if (!player.capabilities.isCreativeMode) {
            held.stackSize--;
        }
        tell(player, "released", employment.getName());
        return true;
    }

    /// Shows a worker's employee card.
    public static void showCard(final EntityPlayer player, final EntityLivingBase target, final Employment employment) {
        final long days = (target.worldObj.getTotalWorldTime() - employment.getHiredAt()) / 24000;
        tell(
            player,
            Employment.isOnShift(target) ? "cardOnShift" : "cardOffShift",
            employment.getName(),
            employment.getTitle(),
            days);
    }

    /// Assigns the worker of the signed contract `stack` to the seat at `x, y, z`.
    public static boolean assign(final EntityPlayer player, final ItemStack stack, final World world, final int x,
        final int y, final int z) {
        if (!Furniture.SEAT.matches(world.getBlock(x, y, z))) {
            tell(player, "notASeat");
            return false;
        }
        final UUID id = ItemSignedContract.worker(stack);
        final EntityLivingBase worker = id == null ? null : findWorker(world, id, player);
        if (worker == null) {
            tell(player, "workerNotFound");
            return true;
        }
        final EntityWorkstationMount existing = mountAt(world, x, y, z);
        if (existing != null && existing.riddenByEntity != null && existing.riddenByEntity != worker) {
            tell(player, "seatTaken");
            return true;
        }
        if (worker.ridingEntity != null) {
            worker.mountEntity(null);
        }
        final EntityWorkstationMount mount = existing != null ? existing
            : new EntityWorkstationMount(world, x, y, z, facing(world, x, y, z, player.rotationYaw));
        if (existing == null) {
            world.spawnEntityInWorld(mount);
        }
        worker.mountEntity(mount);
        final Employment employment = Employment.of(worker);
        tell(player, "assigned", employment != null ? employment.getName() : worker.getCommandSenderName());
        return true;
    }

    /// The workstation mount on the seat at `x, y, z`, if any.
    public static @Nullable EntityWorkstationMount mountAt(final World world, final int x, final int y, final int z) {
        final List<EntityWorkstationMount> mounts = world.getEntitiesWithinAABB(
            EntityWorkstationMount.class,
            AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 2, z + 1));
        for (final EntityWorkstationMount mount : mounts) {
            if (!mount.isDead && mount.isAt(x, y, z)) {
                return mount;
            }
        }
        return null;
    }

    private static @Nullable EntityLivingBase findWorker(final World world, final UUID id, final EntityPlayer near) {
        final double range = ABConfig.assignRange;
        for (final Object o : world.loadedEntityList) {
            if (o instanceof EntityLivingBase living && id.equals(living.getUniqueID())
                && living.getDistanceSqToEntity(near) <= range * range) {
                return living;
            }
        }
        return null;
    }

    /// Seats `worker` on the seat at `x, y, z` again, as after a break: unless the seat is gone or someone else
    /// sits there, in which case it stays standing, off shift, until it is assigned again.
    public static boolean sit(final World world, final EntityLivingBase worker, final int x, final int y, final int z,
        final float yaw) {
        if (!Furniture.SEAT.matches(world.getBlock(x, y, z))) {
            return false;
        }
        final EntityWorkstationMount existing = mountAt(world, x, y, z);
        if (existing != null && existing.riddenByEntity != null && existing.riddenByEntity != worker) {
            return false;
        }
        final EntityWorkstationMount mount = existing != null ? existing
            : new EntityWorkstationMount(world, x, y, z, facing(world, x, y, z, yaw));
        if (existing == null) {
            world.spawnEntityInWorld(mount);
        }
        worker.mountEntity(mount);
        return true;
    }

    /// The yaw that faces the seat's workstation, or `fallback` if it has none.
    /// See Note [Facing the workstation].
    private static float facing(final World world, final int x, final int y, final int z, final float fallback) {
        final Float facing = Workplace.facing(world, x, y, z);
        return facing != null ? facing : fallback;
    }
}
