package com.gtnewhorizons.appliedbureaucratics.block;

import net.minecraft.block.Block;

import com.gtnewhorizons.appliedbureaucratics.facility.desk.TileRequisitionTray;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModBlocks {

    public static Block requisitionTray;

    private ModBlocks() {}

    public static void init() {
        requisitionTray = new BlockRequisitionTray();
        GameRegistry.registerBlock(requisitionTray, "requisition_tray");
        GameRegistry.registerTileEntity(TileRequisitionTray.class, "appliedbureaucratics:requisition_tray");
    }
}
