package com.gtnewhorizons.appliedbureaucratics.item;

import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModItems {

    public static Item employmentContract;
    public static Item signedContract;
    public static Item managementContract;
    public static Item pinkSlip;
    public static Item component;

    private ModItems() {}

    public static void init() {
        employmentContract = register(new ItemAB("employment_contract"), "employment_contract");
        signedContract = register(new ItemSignedContract("signed_contract"), "signed_contract");
        managementContract = register(new ItemAB("management_contract"), "management_contract");
        pinkSlip = register(new ItemAB("pink_slip"), "pink_slip");
        component = register(new ItemComponent(), "component");
    }

    private static Item register(final Item item, final String name) {
        GameRegistry.registerItem(item, name);
        return item;
    }
}
