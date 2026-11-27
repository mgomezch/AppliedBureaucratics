package com.gtnewhorizons.appliedbureaucratics.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public final class ABCreativeTab extends CreativeTabs {

    public static final ABCreativeTab INSTANCE = new ABCreativeTab();

    private ABCreativeTab() {
        super("appliedbureaucratics");
    }

    @Override
    public Item getTabIconItem() {
        return ModItems.employmentContract;
    }
}
