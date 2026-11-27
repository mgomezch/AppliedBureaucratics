package com.gtnewhorizons.appliedbureaucratics.facility;

import net.minecraft.item.ItemStack;

import com.gtnewhorizons.appliedbureaucratics.facility.datacenter.MTEDataCenter;
import com.gtnewhorizons.appliedbureaucratics.facility.internet.MTEInternet;
import com.gtnewhorizons.appliedbureaucratics.facility.mainframe.MTEMainframe;
import com.gtnewhorizons.appliedbureaucratics.facility.singularity.MTESingularity;

/// Our GregTech machines. IDs 32200 to 32249 are ours: GT5U leaves 32053 to 32736 free, and ModularNuclear uses
/// 32100 to 32193.
public final class ModMetaTileEntities {

    public static final int ID_MAINFRAME = 32200;
    public static final int ID_INTERNET = 32201;
    public static final int ID_DATA_CENTER = 32202;
    public static final int ID_SINGULARITY = 32203;

    public static ItemStack mainframe;
    public static ItemStack internet;
    public static ItemStack dataCenter;
    public static ItemStack singularity;

    private ModMetaTileEntities() {}

    public static void init() {
        mainframe = new MTEMainframe(ID_MAINFRAME, "multimachine.ab.mainframe", "Mainframe").getStackForm(1);
        internet = new MTEInternet(ID_INTERNET, "multimachine.ab.internet", "The Internet").getStackForm(1);
        singularity = new MTESingularity(ID_SINGULARITY, "multimachine.ab.singularity", "The Singularity")
            .getStackForm(1);
        dataCenter = new MTEDataCenter(ID_DATA_CENTER, "multimachine.ab.datacenter", "AI Data Center").getStackForm(1);
    }
}
