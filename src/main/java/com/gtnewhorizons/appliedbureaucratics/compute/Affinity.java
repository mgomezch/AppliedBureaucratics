package com.gtnewhorizons.appliedbureaucratics.compute;

/// Which department a worker is better at, from its villager profession. See Note [The compute model].
public enum Affinity {

    /// Librarians: analysts.
    PLANNING,
    /// Farmers: logistics.
    DISPATCH,
    /// Priests: forecasting is a matter of faith.
    EITHER,
    NONE;

    /// The multiplier for a worker with this affinity counted in `department`.
    public double multiplier(final Department department) {
        final ComputeSettings settings = ComputeSettings.get();
        return switch (this) {
            case PLANNING -> department == Department.PLANNING ? settings.affinityBonus : 1;
            case DISPATCH -> department == Department.DISPATCH ? settings.affinityBonus : 1;
            case EITHER -> settings.eitherAffinityBonus;
            case NONE -> 1;
        };
    }

    public enum Department {
        PLANNING,
        DISPATCH
    }
}
