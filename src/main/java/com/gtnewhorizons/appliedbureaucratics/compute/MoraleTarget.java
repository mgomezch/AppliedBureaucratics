package com.gtnewhorizons.appliedbureaucratics.compute;

/// The morale a facility drifts towards, from the design document's table (section 7.3). See Note [The compute
/// model].
///
/// @param paid whether wages are paid; true for a facility with no wages to pay
/// @param amenities the amenity bonus around the seats, averaged; capped at [#AMENITY_CAP]
/// @param coffee a stocked Break Room Counter within reach
/// @param party a pizza party in the last day
/// @param crowded more than one seat per 4 m² around the seats
/// @param pollution the chunk's pollution penalty, 0 to 20
/// @param managers the managers' morale penalties, as a positive number
/// @param crunch the facility is crunching
public record MoraleTarget(boolean paid, int amenities, boolean coffee, boolean party, boolean crowded, int pollution,
    int managers, boolean crunch) {

    public static final int BASE = 50;
    public static final int AMENITY_CAP = 40;

    public double value() {
        double target = BASE;
        target += this.paid ? 10 : -40;
        target += Math.min(AMENITY_CAP, Math.max(0, this.amenities));
        target += this.coffee ? 10 : 0;
        target += this.party ? 20 : 0;
        target -= this.crowded ? 10 : 0;
        target -= Math.max(0, Math.min(20, this.pollution));
        target -= Math.max(0, this.managers);
        target -= this.crunch ? 30 : 0;
        return Math.max(0, Math.min(100, target));
    }

    /// The pollution penalty for a chunk's pollution, from 5 at the smog threshold to 20 at four times it.
    public static int pollutionPenalty(final int pollution, final int smogThreshold) {
        if (smogThreshold <= 0 || pollution < smogThreshold) {
            return 0;
        }
        final double over = (double) pollution / smogThreshold;
        return (int) Math.round(Math.min(20, 5 + 5 * (over - 1)));
    }
}
