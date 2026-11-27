package com.gtnewhorizons.appliedbureaucratics.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gtnewhorizons.appliedbureaucratics.AppliedBureaucratics;

/// An item of ours, named `appliedbureaucratics.<name>`, with the tooltip `item.appliedbureaucratics.<name>.tooltip`
/// split on `\n`.
public class ItemAB extends Item {

    public ItemAB(final String name) {
        this.setUnlocalizedName(AppliedBureaucratics.MODID + "." + name);
        this.setTextureName(AppliedBureaucratics.MODID + ":" + name);
        this.setCreativeTab(ABCreativeTab.INSTANCE);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(final ItemStack stack, final EntityPlayer player, final List lines,
        final boolean advanced) {
        final String key = this.getUnlocalizedName(stack) + ".tooltip";
        if (StatCollector.canTranslate(key)) {
            for (final String line : StatCollector.translateToLocal(key)
                .split("\\\\n")) {
                lines.add("§7§o" + line);
            }
        }
    }
}
