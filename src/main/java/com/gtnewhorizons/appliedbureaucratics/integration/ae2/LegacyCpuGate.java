package com.gtnewhorizons.appliedbureaucratics.integration.ae2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import com.gtnewhorizons.appliedbureaucratics.AppliedBureaucratics;
import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;

import appeng.api.AEApi;
import appeng.block.crafting.BlockCraftingUnit;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

/// Moves stock crafting CPU blocks to ZPM (design document, section 6.5): their crafting table recipes go, and their
/// assembler recipes need ZPM power. A block crafted only at a table gets an assembler recipe from the same
/// ingredients, at ZPM. Off with [ABConfig#gateStockCpuRecipes]. Blocks already in the world are untouched: they are
/// legacy, and keep their full value (see [ComputePolicy]).
public final class LegacyCpuGate {

    private LegacyCpuGate() {}

    private static boolean isCpuBlock(final ItemStack stack) {
        return stack != null && stack.getItem() != null
            && Block.getBlockFromItem(stack.getItem()) instanceof BlockCraftingUnit;
    }

    public static void apply() {
        if (!ABConfig.gateStockCpuRecipes) {
            return;
        }
        final List<ItemStack> tableOnly = new ArrayList<>();
        final List<List<ItemStack>> tableInputs = new ArrayList<>();
        @SuppressWarnings("unchecked")
        final Iterator<IRecipe> recipes = CraftingManager.getInstance()
            .getRecipeList()
            .iterator();
        int removedTable = 0;
        while (recipes.hasNext()) {
            final IRecipe recipe = recipes.next();
            final ItemStack output = recipe.getRecipeOutput();
            if (isCpuBlock(output)) {
                tableOnly.add(output.copy());
                tableInputs.add(ingredients(recipe));
                recipes.remove();
                removedTable++;
            }
        }
        final List<GTRecipe> gated = new ArrayList<>();
        for (final GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
            for (final ItemStack output : recipe.mOutputs) {
                if (isCpuBlock(output)) {
                    gated.add(recipe);
                    break;
                }
            }
        }
        RecipeMaps.assemblerRecipes.getBackend()
            .removeRecipes(gated);
        for (final GTRecipe recipe : gated) {
            final GTRecipe zpm = recipe.copy();
            zpm.mEUt = (int) Math.max(zpm.mEUt, TierEU.RECIPE_ZPM);
            RecipeMaps.assemblerRecipes.getBackend()
                .compileRecipe(zpm);
            for (int i = tableOnly.size() - 1; i >= 0; i--) {
                for (final ItemStack output : recipe.mOutputs) {
                    if (output != null && output.isItemEqual(tableOnly.get(i))) {
                        tableOnly.remove(i);
                        tableInputs.remove(i);
                        break;
                    }
                }
            }
        }
        int added = 0;
        for (int i = 0; i < tableOnly.size(); i++) {
            final List<ItemStack> inputs = tableInputs.get(i);
            if (inputs.isEmpty()) {
                continue;
            }
            GTValues.RA.stdBuilder()
                .itemInputs(inputs.toArray(new ItemStack[0]))
                .itemOutputs(tableOnly.get(i))
                .duration(20 * 20)
                .eut(TierEU.RECIPE_ZPM)
                .addTo(RecipeMaps.assemblerRecipes);
            added++;
        }
        AppliedBureaucratics.LOG.info(
            "Stock crafting CPU blocks moved to ZPM: {} table recipes removed, {} assembler recipes now ZPM, {} added.",
            removedTable,
            gated.size(),
            added);
        AEApi.instance();
    }

    /// The ingredients of a table recipe, merged by item, when every slot is one item; empty otherwise.
    private static List<ItemStack> ingredients(final IRecipe recipe) {
        final Object[] slots;
        if (recipe instanceof net.minecraft.item.crafting.ShapedRecipes shaped) {
            slots = shaped.recipeItems;
        } else if (recipe instanceof net.minecraftforge.oredict.ShapedOreRecipe ore) {
            slots = ore.getInput();
        } else if (recipe instanceof net.minecraftforge.oredict.ShapelessOreRecipe ore) {
            slots = ore.getInput()
                .toArray();
        } else if (recipe instanceof net.minecraft.item.crafting.ShapelessRecipes shapeless) {
            slots = shapeless.recipeItems.toArray();
        } else {
            return new ArrayList<>();
        }
        final List<ItemStack> merged = new ArrayList<>();
        for (final Object slot : slots) {
            ItemStack stack = null;
            if (slot instanceof ItemStack s) {
                stack = s;
            } else if (slot instanceof List<?>list && !list.isEmpty() && list.get(0) instanceof ItemStack s) {
                stack = s;
            } else if (slot != null) {
                return new ArrayList<>();
            }
            if (stack == null) {
                continue;
            }
            boolean found = false;
            for (final ItemStack m : merged) {
                if (m.isItemEqual(stack)) {
                    m.stackSize += 1;
                    found = true;
                    break;
                }
            }
            if (!found) {
                final ItemStack copy = stack.copy();
                copy.stackSize = 1;
                merged.add(copy);
            }
        }
        return merged;
    }
}
