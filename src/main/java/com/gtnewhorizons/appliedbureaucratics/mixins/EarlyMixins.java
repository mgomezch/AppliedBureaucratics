package com.gtnewhorizons.appliedbureaucratics.mixins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/// Mixins into Minecraft itself. See Note [Contracted mobs are held by mixins].
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EarlyMixins implements IEarlyMixinLoader, IFMLLoadingPlugin {

    @Override
    public String getMixinConfig() {
        return "mixins.appliedbureaucratics.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        final List<String> mixins = new ArrayList<>();
        mixins.add("MixinEntityLivingBase_Shift");
        mixins.add("MixinEntityEnderman_Shift");
        return mixins;
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
