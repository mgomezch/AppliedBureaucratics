package com.gtnewhorizons.appliedbureaucratics.block;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;

/// GregTech icon containers for our machines' overlays, in `textures/blocks/`.
public final class ABTextures {

    public static final IIconContainer MAINFRAME_FRONT = Textures.BlockIcons
        .custom("appliedbureaucratics", "OVERLAY_FRONT_MAINFRAME");
    public static final IIconContainer MAINFRAME_FRONT_ACTIVE = Textures.BlockIcons
        .custom("appliedbureaucratics", "OVERLAY_FRONT_MAINFRAME_ACTIVE");

    public static final IIconContainer WATER_COOLER = Textures.BlockIcons
        .custom("appliedbureaucratics", "OVERLAY_FRONT_WATER_COOLER");
    public static final IIconContainer WATER_COOLER_ACTIVE = Textures.BlockIcons
        .custom("appliedbureaucratics", "OVERLAY_FRONT_WATER_COOLER_ACTIVE");
    public static final IIconContainer COFFEE_MACHINE = Textures.BlockIcons
        .custom("appliedbureaucratics", "OVERLAY_FRONT_COFFEE_MACHINE");
    public static final IIconContainer COFFEE_MACHINE_ACTIVE = Textures.BlockIcons
        .custom("appliedbureaucratics", "OVERLAY_FRONT_COFFEE_MACHINE_ACTIVE");
    /// The GTNH Vending Machine's front (MIT licensed), from its own mod's assets: the Snack Vending Machine is its
    /// office cousin.
    public static final IIconContainer VENDING_FRONT_OFF = Textures.BlockIcons
        .custom("vendingmachine", "vending_machine_front_off");
    public static final IIconContainer VENDING_FRONT_ON = Textures.BlockIcons
        .custom("vendingmachine", "vending_machine_front_on");

    private ABTextures() {}

    /// Loads the class, so the icons are registered with GregTech in time.
    public static void init() {}
}
