package com.gtnewhorizons.appliedbureaucratics.workforce;

import java.util.Random;
import java.util.UUID;

/// Names and job titles for workers, picked from the worker's UUID so they stay the same.
public final class WorkerNames {

    private static final String[] FIRST = { "Bartleby", "Agnes", "Horace", "Mildred", "Cuthbert", "Edna", "Ambrose",
        "Prudence", "Ignatius", "Winifred", "Barnaby", "Gertrude", "Thaddeus", "Ethel", "Mortimer", "Hortense",
        "Percival", "Dorcas", "Ebenezer", "Philippa", "Nigel", "Beryl", "Clement", "Muriel" };
    private static final String[] TITLE_PLANNING = { "Analyst", "Junior Analyst", "Senior Analyst",
        "Requisitions Clerk", "Forecasting Associate" };
    private static final String[] TITLE_DISPATCH = { "Dispatcher", "Logistics Associate", "Expeditor",
        "Fulfilment Specialist", "Shipping Clerk" };
    private static final String[] TITLE_OTHER = { "Clerk", "Junior Clerk", "Office Assistant", "Associate",
        "Copy Typist" };
    private static final String[] TITLE_MANAGER = { "Team Lead", "Line Manager", "Head of Synergy",
        "Associate Director", "Vice President of Alignment" };

    private WorkerNames() {}

    public static String name(final UUID id) {
        return FIRST[new Random(id.getLeastSignificantBits()).nextInt(FIRST.length)];
    }

    /// A title for a worker of villager profession `profession` (vanilla: 0 farmer, 1 librarian, 2 priest), or a
    /// manager's.
    public static String title(final UUID id, final int profession, final boolean manager) {
        final String[] titles;
        if (manager) {
            titles = TITLE_MANAGER;
        } else if (profession == 1) {
            titles = TITLE_PLANNING;
        } else if (profession == 0) {
            titles = TITLE_DISPATCH;
        } else {
            titles = TITLE_OTHER;
        }
        return titles[new Random(id.getMostSignificantBits()).nextInt(titles.length)];
    }
}
