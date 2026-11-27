package com.gtnewhorizons.appliedbureaucratics;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.appliedbureaucratics.block.BlockABCasing.Casing;
import com.gtnewhorizons.appliedbureaucratics.block.ModBlocks;
import com.gtnewhorizons.appliedbureaucratics.facility.ModMetaTileEntities;
import com.gtnewhorizons.appliedbureaucratics.item.ItemComponent;
import com.gtnewhorizons.appliedbureaucratics.item.ModItems;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;

/// Recipe sketches from the design document. The tray must be obtainable no later than the ME Interface, so it is
/// late HV.
final class Recipes {

    private Recipes() {}

    static void register() {
        final ItemStack logic = AEApi.instance()
            .definitions()
            .materials()
            .logicProcessor()
            .maybeStack(1)
            .orNull();
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModBlocks.requisitionTray),
            GTModHandler.RecipeBits.BITS,
            new Object[] { "PCP", "BHB", "PCP", 'P', logic, 'C', OrePrefixes.circuit.get(Materials.HV), 'B',
                new ItemStack(Items.book), 'H', ItemList.Hull_HV });
        registerMainframe(logic);
        GameRegistry.addShapelessRecipe(
            new ItemStack(ModItems.employmentContract),
            Items.paper,
            new ItemStack(Items.dye, 1, 0),
            Items.emerald);
        GameRegistry.addShapelessRecipe(
            new ItemStack(ModItems.managementContract),
            Items.paper,
            new ItemStack(Items.dye, 1, 0),
            Blocks.emerald_block);
        GameRegistry.addShapelessRecipe(new ItemStack(ModItems.pinkSlip), Items.paper, new ItemStack(Items.dye, 1, 9));
        GameRegistry.addRecipe(
            new ItemStack(ModBlocks.breakRoomCounter),
            "SSS",
            "PCP",
            "PPP",
            'S',
            Blocks.stone_slab,
            'P',
            Blocks.planks,
            'C',
            Items.cauldron);
    }

    private static void registerMainframe(final ItemStack logic) {
        final ItemStack engineering = AEApi.instance()
            .definitions()
            .materials()
            .engProcessor()
            .maybeStack(1)
            .orNull();
        final long bits = GTModHandler.RecipeBits.BITS;
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModBlocks.casing, 2, Casing.MAINFRAME.meta()),
            bits,
            new Object[] { "PhP", "PFP", "PwP", 'P', OrePrefixes.plate.get(Materials.Steel), 'F',
                OrePrefixes.frameGt.get(Materials.Steel) });
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModBlocks.casing, 1, Casing.TAPE_WINDOW.meta()),
            bits,
            new Object[] { "GRG", "GCG", 'G', "paneGlass", 'R', ItemComponent.Kind.TAPE_REEL.stack(1), 'C',
                new ItemStack(ModBlocks.casing, 1, Casing.MAINFRAME.meta()) });
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModBlocks.terminalConcentrator),
            bits,
            new Object[] { "ECE", "PMP", "ECE", 'E', OrePrefixes.circuit.get(Materials.EV), 'C',
                OrePrefixes.cableGt01.get(Materials.Aluminium), 'P', engineering, 'M',
                new ItemStack(ModBlocks.casing, 1, Casing.MAINFRAME.meta()) });
        GTModHandler.addCraftingRecipe(
            ModMetaTileEntities.mainframe,
            bits,
            new Object[] { "CPC", "LHL", "CTC", 'C', OrePrefixes.circuit.get(Materials.EV), 'P', engineering, 'L',
                logic, 'H', ItemList.Hull_EV, 'T', ItemComponent.Kind.TAPE_REEL.stack(1) });
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModBlocks.casing, 2, Casing.TUBE.meta()),
            bits,
            new Object[] { "GPG", "PFP", "GPG", 'G', "blockGlass", 'P', OrePrefixes.plate.get(Materials.StainlessSteel),
                'F', OrePrefixes.frameGt.get(Materials.StainlessSteel) });
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModBlocks.casing, 1, Casing.BACKBONE.meta()),
            bits,
            new Object[] { "WCW", "CFC", "WCW", 'W', OrePrefixes.wireGt04.get(Materials.Platinum), 'C',
                OrePrefixes.circuit.get(Materials.IV), 'F', new ItemStack(ModBlocks.casing, 1, Casing.TUBE.meta()) });
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModBlocks.casing, 1, Casing.DISH.meta()),
            bits,
            new Object[] { "PPP", " S ", " C ", 'P', OrePrefixes.plate.get(Materials.Aluminium), 'S',
                ItemList.Sensor_IV, 'C', new ItemStack(ModBlocks.casing, 1, Casing.TUBE.meta()) });
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModBlocks.routerPort),
            bits,
            new Object[] { "CEC", "ETE", "CEC", 'C', OrePrefixes.cableGt01.get(Materials.Platinum), 'E',
                ItemList.Emitter_IV, 'T', new ItemStack(ModBlocks.casing, 1, Casing.TUBE.meta()) });
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModBlocks.gigKiosk),
            bits,
            new Object[] { "SPS", "PCP", "SPS", 'S', ItemList.Sensor_HV, 'P',
                OrePrefixes.plate.get(Materials.Polyethylene), 'C', OrePrefixes.circuit.get(Materials.HV) });
        GTModHandler.addCraftingRecipe(
            ModMetaTileEntities.internet,
            bits,
            new Object[] { "CBC", "EHE", "CBC", 'C', OrePrefixes.circuit.get(Materials.IV), 'B',
                new ItemStack(ModBlocks.casing, 1, Casing.BACKBONE.meta()), 'E', ItemList.Emitter_IV, 'H',
                ItemList.Hull_IV });
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModItems.requisitionUpgrade),
            bits,
            new Object[] { "CPC", "RER", "CPC", 'C', OrePrefixes.circuit.get(Materials.EV), 'P', engineering, 'R',
                ItemList.Robot_Arm_EV, 'E', new ItemStack(Items.emerald) });
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModBlocks.dockingPad),
            bits,
            new Object[] { "PPP", "RMR", 'P', OrePrefixes.plate.get(Materials.StainlessSteel), 'R',
                ItemList.Robot_Arm_EV, 'M', new ItemStack(ModBlocks.casing, 1, Casing.MAINFRAME.meta()) });
        GTModHandler.addCraftingRecipe(
            ItemComponent.Kind.ACCELERATOR_CARD.stack(1),
            bits,
            new Object[] { "CPC", "PBP", "CPC", 'C', OrePrefixes.circuit.get(Materials.LuV), 'P',
                OrePrefixes.plate.get(Materials.Silicon), 'B', ItemList.Circuit_Board_Elite });
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModBlocks.acceleratorRack),
            bits,
            new Object[] { "PCP", "P P", "PCP", 'P', OrePrefixes.plate.get(Materials.Aluminium), 'C',
                OrePrefixes.cableGt01.get(Materials.Platinum) });
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModBlocks.casing, 4, Casing.DATA_HALL.meta()),
            bits,
            new Object[] { "PPP", "PFP", "PPP", 'P', OrePrefixes.plate.get(Materials.Aluminium), 'F',
                OrePrefixes.frameGt.get(Materials.Aluminium) });
        GTModHandler.addCraftingRecipe(
            new ItemStack(ModBlocks.casing, 4, Casing.RAISED_FLOOR.meta()),
            bits,
            new Object[] { "PPP", "R R", "PPP", 'P', OrePrefixes.plate.get(Materials.Aluminium), 'R',
                OrePrefixes.pipeMedium.get(Materials.StainlessSteel) });
        GTModHandler.addCraftingRecipe(
            ModMetaTileEntities.dataCenter,
            bits,
            new Object[] { "CAC", "AHA", "CAC", 'C', OrePrefixes.circuit.get(Materials.LuV), 'A',
                ItemComponent.Kind.ACCELERATOR_CARD.stack(1), 'H', ItemList.Hull_LuV });
        GTModHandler.addCraftingRecipe(
            ItemComponent.Kind.DISK_ARRAY.stack(1),
            bits,
            new Object[] { "HHH", "HCH", "HHH", 'H', ItemComponent.Kind.HIGH_DENSITY_TAPE_REEL.stack(1), 'C',
                OrePrefixes.circuit.get(Materials.IV) });
        GTModHandler.addCraftingRecipe(
            ItemComponent.Kind.TAPE_REEL.stack(2),
            bits,
            new Object[] { "PSP", "SMS", "PSP", 'P', OrePrefixes.plate.get(Materials.Polyethylene), 'S',
                OrePrefixes.foil.get(Materials.Iron), 'M', OrePrefixes.stick.get(Materials.SteelMagnetic) });
        GTModHandler.addCraftingRecipe(
            ItemComponent.Kind.HIGH_DENSITY_TAPE_REEL.stack(1),
            bits,
            new Object[] { "PSP", "SMS", "PSP", 'P', OrePrefixes.plate.get(Materials.Polytetrafluoroethylene), 'S',
                OrePrefixes.foil.get(Materials.Chrome), 'M', OrePrefixes.stick.get(Materials.NeodymiumMagnetic) });
        GTModHandler.addCraftingRecipe(
            ItemComponent.Kind.PUNCH_CARD.stack(1),
            bits,
            new Object[] { "PP", "PP", 'P', new ItemStack(Items.paper) });
        GTModHandler.addCraftingRecipe(
            ItemComponent.Kind.BLANK_DATA_TAPE.stack(4),
            bits,
            new Object[] { "PMP", 'P', OrePrefixes.plate.get(Materials.Polyethylene), 'M',
                OrePrefixes.stick.get(Materials.SteelMagnetic) });
    }
}
