package com.gtnewhorizons.appliedbureaucratics.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import com.gtnewhorizons.appliedbureaucratics.facility.datacenter.TileAcceleratorRack;
import com.gtnewhorizons.appliedbureaucratics.facility.desk.TileRequisitionTray;
import com.gtnewhorizons.appliedbureaucratics.facility.internet.TileGigKiosk;
import com.gtnewhorizons.appliedbureaucratics.facility.internet.TileRouterPort;
import com.gtnewhorizons.appliedbureaucratics.facility.mainframe.TileTerminalConcentrator;
import com.gtnewhorizons.appliedbureaucratics.facility.singularity.TileSingularityCore;
import com.gtnewhorizons.appliedbureaucratics.integration.oc.TileDockingPad;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModBlocks {

    public static Block requisitionTray;
    public static Block casing;
    public static Block terminalConcentrator;
    public static Block routerPort;
    public static Block gigKiosk;
    public static Block dockingPad;
    public static Block acceleratorRack;
    public static Block singularityCore;

    private ModBlocks() {}

    public static void init() {
        requisitionTray = new BlockRequisitionTray();
        GameRegistry.registerBlock(requisitionTray, "requisition_tray");
        GameRegistry.registerTileEntity(TileRequisitionTray.class, "appliedbureaucratics:requisition_tray");
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
        acceleratorRack = new BlockSimpleTile("accelerator_rack", Material.iron, TileAcceleratorRack::new);
        GameRegistry.registerBlock(acceleratorRack, "accelerator_rack");
        GameRegistry.registerTileEntity(TileAcceleratorRack.class, "appliedbureaucratics:accelerator_rack");
        singularityCore = new BlockSingularityCore();
        GameRegistry.registerBlock(singularityCore, "singularity_core");
        GameRegistry.registerTileEntity(TileSingularityCore.class, "appliedbureaucratics:singularity_core");
    }
}
