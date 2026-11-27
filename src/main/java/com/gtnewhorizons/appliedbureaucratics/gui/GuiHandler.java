package com.gtnewhorizons.appliedbureaucratics.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnewhorizons.appliedbureaucratics.facility.desk.TileRequisitionTray;

import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

    public static final int REQUISITION_TRAY = 0;

    @Override
    public Object getServerGuiElement(final int id, final EntityPlayer player, final World world, final int x,
        final int y, final int z) {
        final TileEntity te = world.getTileEntity(x, y, z);
        if (id == REQUISITION_TRAY && te instanceof TileRequisitionTray tray) {
            return new ContainerRequisitionTray(player.inventory, tray);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(final int id, final EntityPlayer player, final World world, final int x,
        final int y, final int z) {
        final TileEntity te = world.getTileEntity(x, y, z);
        if (id == REQUISITION_TRAY && te instanceof TileRequisitionTray tray) {
            return new GuiRequisitionTray(player.inventory, tray);
        }
        return null;
    }
}
