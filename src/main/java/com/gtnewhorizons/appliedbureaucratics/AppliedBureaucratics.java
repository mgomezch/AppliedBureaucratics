package com.gtnewhorizons.appliedbureaucratics;

import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnewhorizons.appliedbureaucratics.block.ModBlocks;
import com.gtnewhorizons.appliedbureaucratics.command.CommandAB;
import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;
import com.gtnewhorizons.appliedbureaucratics.config.ItemList;
import com.gtnewhorizons.appliedbureaucratics.facility.ModMetaTileEntities;
import com.gtnewhorizons.appliedbureaucratics.gui.GuiHandler;
import com.gtnewhorizons.appliedbureaucratics.integration.ae2.ComputeGridCache;
import com.gtnewhorizons.appliedbureaucratics.integration.ae2.ComputePolicy;
import com.gtnewhorizons.appliedbureaucratics.integration.ae2.IComputeGrid;
import com.gtnewhorizons.appliedbureaucratics.integration.bibliocraft.Furniture;
import com.gtnewhorizons.appliedbureaucratics.item.ModItems;
import com.gtnewhorizons.appliedbureaucratics.workforce.EntityWorkstationMount;
import com.gtnewhorizons.appliedbureaucratics.workforce.ManagerKind;
import com.gtnewhorizons.appliedbureaucratics.workforce.WorkforceEvents;

import appeng.api.AEApi;
import appeng.api.networking.crafting.compute.CraftingComputePolicies;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;

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

    @SidedProxy(
        clientSide = "com.gtnewhorizons.appliedbureaucratics.ClientProxy",
        serverSide = "com.gtnewhorizons.appliedbureaucratics.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ABConfig.load(event.getSuggestedConfigurationFile());
        ModItems.init();
        ModBlocks.init();
        EntityRegistry.registerModEntity(EntityWorkstationMount.class, "WorkstationMount", 0, this, 64, 20, false);
        AEApi.instance()
            .registries()
            .gridCache()
            .registerGridCache(IComputeGrid.class, ComputeGridCache.class);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModMetaTileEntities.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        MinecraftForge.EVENT_BUS.register(new WorkforceEvents());
        proxy.registerRenderers();
        if (ABConfig.enableComputeRequirement) {
            CraftingComputePolicies.register(new ComputePolicy());
            LOG.info("Compute policy registered: AE2 autocrafting now needs compute capacity.");
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Furniture.resolveAll();
        ItemList.resolveAll();
        ManagerKind.resolveAll();
        Recipes.register();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandAB());
    }
}
