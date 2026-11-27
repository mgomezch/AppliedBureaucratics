package com.gtnewhorizons.appliedbureaucratics.mixins;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

/// Mixins into other mods. See Note [Contracted mobs are held by mixins].
@LateMixin
public class LateMixins implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.appliedbureaucratics.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return new ArrayList<>();
    }
}
