package com.gtnewhorizons.appliedbureaucratics.workforce;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;
import com.gtnewhorizons.appliedbureaucratics.integration.bibliocraft.Furniture;
import com.gtnewhorizons.appliedbureaucratics.item.ModItems;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/// Contracts, pink slips and employee cards on right-click, and invulnerability on shift. See Note [Contracted mobs
/// are held by mixins].
public final class WorkforceEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onInteract(final EntityInteractEvent event) {
        if (event.entityPlayer.worldObj.isRemote || !(event.target instanceof EntityLivingBase target)) {
            return;
        }
        final ItemStack held = event.entityPlayer.getCurrentEquippedItem();
        if (held != null
            && (held.getItem() == ModItems.employmentContract || held.getItem() == ModItems.managementContract)) {
            WorkforceActions.hire(event.entityPlayer, held, target);
            event.setCanceled(true);
            return;
        }
        if (held != null && held.getItem() == ModItems.pinkSlip) {
            WorkforceActions.release(event.entityPlayer, held, target);
            event.setCanceled(true);
            return;
        }
        final Employment employment = Employment.of(target);
        if (employment != null) {
            WorkforceActions.showCard(event.entityPlayer, target, employment);
            event.setCanceled(true);
        }
    }

    /// A signed contract used on a seat assigns its worker. This runs before the block's own activation, since a
    /// BiblioCraft seat would otherwise seat the player instead, sneaking or not.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(final PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final ItemStack held = event.entityPlayer.getCurrentEquippedItem();
        if (held == null || held.getItem() != ModItems.signedContract
            || !Furniture.SEAT.matches(event.world.getBlock(event.x, event.y, event.z))) {
            return;
        }
        // The client must still send the click: cancelling it there would keep it from the server.
        if (!event.world.isRemote) {
            WorkforceActions.assign(event.entityPlayer, held, event.world, event.x, event.y, event.z);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttack(final LivingAttackEvent event) {
        if (ABConfig.workersInvulnerable && Employment.isOnShift(event.entityLiving)) {
            event.setCanceled(true);
        }
    }
}
