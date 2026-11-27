package com.gtnewhorizons.appliedbureaucratics.block;

import net.minecraft.block.Block;

import com.gtnewhorizons.appliedbureaucratics.facility.breakroom.TileBreakRoomCounter;
import com.gtnewhorizons.appliedbureaucratics.facility.desk.TileRequisitionTray;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModBlocks {

    public static Block requisitionTray;
    public static Block breakRoomCounter;

    private ModBlocks() {}

    public static void init() {
        requisitionTray = new BlockRequisitionTray();
        GameRegistry.registerBlock(requisitionTray, "requisition_tray");
        GameRegistry.registerTileEntity(TileRequisitionTray.class, "appliedbureaucratics:requisition_tray");
        breakRoomCounter = new BlockBreakRoomCounter();
        GameRegistry.registerBlock(breakRoomCounter, "break_room_counter");
        GameRegistry.registerTileEntity(TileBreakRoomCounter.class, "appliedbureaucratics:break_room_counter");
    }
}
