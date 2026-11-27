package com.gtnewhorizons.appliedbureaucratics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = AppliedBureaucratics.MODID,
    name = AppliedBureaucratics.MODNAME,
    version = Tags.VERSION,
    dependencies = "required-after:appliedenergistics2;required-after:gregtech;after:OpenComputers;after:BiblioCraft;after:AWWayofTime;after:Thaumcraft")
public class AppliedBureaucratics {

    public static final String MODID = "appliedbureaucratics";
    public static final String MODNAME = "Applied Bureaucratics";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static AppliedBureaucratics instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {}

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {}

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {}
}
