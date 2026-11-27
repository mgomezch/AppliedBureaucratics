package com.gtnewhorizons.appliedbureaucratics.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import com.gtnewhorizons.appliedbureaucratics.facility.breakroom.TileBreakRoomCounter;
import com.gtnewhorizons.appliedbureaucratics.facility.desk.TileRequisitionTray;
import com.gtnewhorizons.appliedbureaucratics.facility.internet.TileGigKiosk;
import com.gtnewhorizons.appliedbureaucratics.facility.internet.TileRouterPort;
import com.gtnewhorizons.appliedbureaucratics.facility.mainframe.TileTerminalConcentrator;
import com.gtnewhorizons.appliedbureaucratics.integration.oc.TileDockingPad;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModBlocks {

    public static Block requisitionTray;
    public static Block breakRoomCounter;
    public static Block casing;
    public static Block terminalConcentrator;
    public static Block routerPort;
    public static Block gigKiosk;
    public static Block dockingPad;

    private ModBlocks() {}

    public static void init() {
        requisitionTray = new BlockRequisitionTray();
        GameRegistry.registerBlock(requisitionTray, "requisition_tray");
        GameRegistry.registerTileEntity(TileRequisitionTray.class, "appliedbureaucratics:requisition_tray");
        breakRoomCounter = new BlockBreakRoomCounter();
        GameRegistry.registerBlock(breakRoomCounter, "break_room_counter");
        GameRegistry.registerTileEntity(TileBreakRoomCounter.class, "appliedbureaucratics:break_room_counter");
        ABTextures.init();
        casing = new BlockABCasing();
        terminalConcentrator = new BlockTerminalConcentrator();
        GameRegistry.registerBlock(terminalConcentrator, "terminal_concentrator");
        GameRegistry.registerTileEntity(TileTerminalConcentrator.class, "appliedbureaucratics:terminal_concentrator");
        routerPort = new BlockSimpleTile("router_port", Material.iron, TileRouterPort::new);
        GameRegistry.registerBlock(routerPort, "router_port");
        GameRegistry.registerTileEntity(TileRouterPort.class, "appliedbureaucratics:router_port");
        gigKiosk = new BlockSimpleTile("gig_kiosk", Material.iron, TileGigKiosk::new);
        GameRegistry.registerBlock(gigKiosk, "gig_kiosk");
        GameRegistry.registerTileEntity(TileGigKiosk.class, "appliedbureaucratics:gig_kiosk");
        dockingPad = new BlockSimpleTile("docking_pad", Material.iron, TileDockingPad::new);
        GameRegistry.registerBlock(dockingPad, "docking_pad");
        GameRegistry.registerTileEntity(TileDockingPad.class, "appliedbureaucratics:docking_pad");
    }
}
