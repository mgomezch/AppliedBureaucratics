package com.gtnewhorizons.appliedbureaucratics.facility;

/// What limits a facility most, for the one plain line every facility leads with, and the sarcastic line after it.
/// The lines are `ab.bottleneck.<name>` and `ab.bottleneck.<name>.quip` in the lang file.
public enum Bottleneck {

    NONE,
    NO_POWER,
    NO_SEAT,
    NO_WORKER,
    NO_KEYBOARD,
    NO_DESK,
    NO_TYPEWRITER,
    NO_PAPER,
    UNPAID,
    LOW_MORALE,
    NO_FILING,
    NO_STRUCTURE,
    NO_HARDWARE,
    NO_COOLANT,
    UNTRAINED,
    SULKING;

    public String key() {
        return "ab.bottleneck." + this.name()
            .toLowerCase();
    }
}
