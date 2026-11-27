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
