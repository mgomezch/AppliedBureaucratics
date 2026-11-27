package com.gtnewhorizons.appliedbureaucratics.block;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;

/// GregTech icon containers for our machines' overlays, in `textures/blocks/`.
public final class ABTextures {

    public static final IIconContainer MAINFRAME_FRONT = Textures.BlockIcons
        .custom("appliedbureaucratics", "OVERLAY_FRONT_MAINFRAME");
    public static final IIconContainer MAINFRAME_FRONT_ACTIVE = Textures.BlockIcons
        .custom("appliedbureaucratics", "OVERLAY_FRONT_MAINFRAME_ACTIVE");

    private ABTextures() {}

    /// Loads the class, so the icons are registered with GregTech in time.
    public static void init() {}
}
