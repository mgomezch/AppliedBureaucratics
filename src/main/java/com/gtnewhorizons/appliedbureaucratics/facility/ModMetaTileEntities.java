package com.gtnewhorizons.appliedbureaucratics.facility;

import net.minecraft.item.ItemStack;

import com.gtnewhorizons.appliedbureaucratics.facility.mainframe.MTEMainframe;

/// Our GregTech machines. IDs 32200 to 32249 are ours: GT5U leaves 32053 to 32736 free, and ModularNuclear uses
/// 32100 to 32193.
public final class ModMetaTileEntities {

    public static final int ID_MAINFRAME = 32200;
    public static final int ID_INTERNET = 32201;
    public static final int ID_DATA_CENTER = 32202;
    public static final int ID_SINGULARITY = 32203;

    public static ItemStack mainframe;

    private ModMetaTileEntities() {}

    public static void init() {
        mainframe = new MTEMainframe(ID_MAINFRAME, "multimachine.ab.mainframe", "Mainframe").getStackForm(1);
    }
}
