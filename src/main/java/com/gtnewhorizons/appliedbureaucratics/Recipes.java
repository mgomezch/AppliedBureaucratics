package com.gtnewhorizons.appliedbureaucratics;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.appliedbureaucratics.block.ModBlocks;
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
}
