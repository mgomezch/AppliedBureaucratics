package com.gtnewhorizons.appliedbureaucratics.compute;

/// A facility's capacities: planning steps per second and dispatch operations per second, from its workforce
/// split and its soft factors. See Note [The compute model], wrinkle (W1) for the floor.
public record FacilityOutput(double planningRate, double dispatchRate, double softFactor) {

    public static final FacilityOutput OFF = new FacilityOutput(0, 0, 0);

    /// @param split the facility's workforce, split by its planning share
    /// @param morale its morale multiplier ([Morale#multiplier])
    /// @param paper whether it has paper for planning
    /// @param crunch whether it is crunching
    /// @param dispatchBonus multiplier on dispatch from furniture, such as a desk bell
    /// @param planningBonus multiplier on planning from furniture or hardware
    public static FacilityOutput compute(final Workforce.Split split, final double morale, final boolean paper,
        final boolean crunch, final double dispatchBonus, final double planningBonus) {
        final ComputeSettings settings = ComputeSettings.get();
        if (split.facilityFte() <= 0) {
            return OFF;
        }
        double soft = morale * (crunch ? settings.crunchFactor : 1);
        soft = Math.max(settings.floor, soft);
        final double paperFactor = paper ? 1 : settings.noPaperFactor;
        final double planningSoft = Math.max(settings.floor, soft * paperFactor);
        return new FacilityOutput(
            split.planningFte() * settings.kPlan * planningSoft * planningBonus,
            split.dispatchFte() * settings.kDisp * soft * dispatchBonus,
            soft);
    }
}
