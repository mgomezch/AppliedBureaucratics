package com.gtnewhorizons.appliedbureaucratics.workforce;

import org.jspecify.annotations.Nullable;

/// Implemented on every living entity by MixinEntityLivingBase_Shift, so a worker's employment is a field read and
/// not a map lookup on every tick. See Note [Contracted mobs are held by mixins].
public interface IEmployable {

    @Nullable
    Employment ab$getEmployment();

    void ab$setEmployment(@Nullable Employment employment);
}
