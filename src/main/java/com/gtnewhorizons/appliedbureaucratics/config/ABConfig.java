package com.gtnewhorizons.appliedbureaucratics.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.gtnewhorizons.appliedbureaucratics.compute.ComputeSettings;
import com.gtnewhorizons.appliedbureaucratics.integration.bibliocraft.Furniture;

/// Every number of the mod. Defaults are the design document's. See Note [The compute model].
public final class ABConfig {

    /// Off: no compute policy is registered, AE2 is stock, and facilities are just extra CPUs.
    public static boolean enableComputeRequirement = true;
    /// A network with a legacy crafting CPU (one built before this mod) plans without metering.
    public static boolean legacyPlanningUnmetered = true;
    /// Crafting CPU blocks placed while the policy is active are metered.
    public static boolean meterNewCraftingBlocks = true;
    /// What metered and legacy CPU blocks are worth, and the co-processing cap of a metered cluster.
    public static double meteredCpuFactor = 0.25;
    public static double legacyCpuFactor = 1.0;
    public static int meteredCoProcessorCap = 64;
    /// Planning steps per second each stock cluster without legacy blocks adds to its network.
    public static double meteredClusterPlanningRate = 8;
    /// Contracted mobs on shift take no damage.
    public static boolean workersInvulnerable = true;
    /// Bosses that show no boss bar but may direct.
    public static String[] bossClasses = { "twilightforest.entity.boss.EntityTFNaga",
        "twilightforest.entity.boss.EntityTFLich", "twilightforest.entity.boss.EntityTFMinoshroom",
        "twilightforest.entity.boss.EntityTFHydra", "twilightforest.entity.boss.EntityTFKnightPhantom",
        "twilightforest.entity.boss.EntityTFUrGhast", "twilightforest.entity.boss.EntityTFYetiAlpha",
        "twilightforest.entity.boss.EntityTFSnowQueen" };
    /// How far a signed contract reaches for its worker.
    public static double assignRange = 32;

    // The Desk.
    public static int deskBytesPerBook = 64;
    public static int deskBytesPerBookshelf = 192;
    public static long deskFilingCap = 8192;
    public static double deskBellBonus = 1.25;
    public static int deskSeatRange = 2;
    public static int deskBookcaseRange = 3;
    // The Mainframe.
    public static long mainframeFilingCap = 1 << 20;

    // The Internet.
    public static long internetFilingCap = 16L << 20;
    public static int gigOpsPerEmerald = 512;

    // The Data Center.
    public static int dataCenterMaxLength = 31;
    public static int dataCenterSlots = 8;
    public static long dataCenterFiling = 64L << 20;
    public static double dataCenterFtePerCard = 0.128;
    public static int dataCenterPollution = 2000;

    // The break room.
    public static double breakRoomRange = 16;
    public static int coffeeWorkerDays = 4;

    private ABConfig() {}

    public static void load(final File file) {
        final Configuration c = new Configuration(file);
        final ComputeSettings s = new ComputeSettings();
        final String model = "compute";
        s.kPlan = c.get(model, "kPlan", s.kPlan, "Planning steps per second per FTE.")
            .getDouble();
        s.kDisp = c.get(model, "kDisp", s.kDisp, "Dispatch operations per second per FTE.")
            .getDouble();
        s.floor = c.get(model, "floor", s.floor, "Lowest fraction of output soft requirements can leave.")
            .getDouble();
        s.noPaperFactor = c.get(model, "noPaperFactor", s.noPaperFactor, "Planning output without paper.")
            .getDouble();
        s.stepsPerSheet = c.get(model, "stepsPerSheet", s.stepsPerSheet, "Planning steps per sheet of paper.")
            .getInt();
        s.affinityBonus = c.get(model, "affinityBonus", s.affinityBonus, "A worker in its affine department.")
            .getDouble();
        s.eitherAffinityBonus = c.get(model, "eitherAffinityBonus", s.eitherAffinityBonus, "Priests.")
            .getDouble();
        s.playerReservation = c
            .get(model, "playerReservation", s.playerReservation, "Share of planning reserved for players' plans.")
            .getDouble();
        s.standingOrderMemory = c
            .get(model, "standingOrderMemory", s.standingOrderMemory, "Recent requests a network remembers.")
            .getInt();
        s.standingOrderCharge = c
            .get(model, "standingOrderCharge", s.standingOrderCharge, "What a repeated request is charged.")
            .getDouble();
        s.creditSeconds = c
            .get(model, "creditSeconds", s.creditSeconds, "Seconds of its rate a network may bank as credit.")
            .getDouble();
        s.wageUnitsPerEmerald = c
            .get(model, "wageUnitsPerEmerald", s.wageUnitsPerEmerald, "Wage units an emerald pays.")
            .getInt();
        s.crunchFactor = c.get(model, "crunchFactor", s.crunchFactor, "Output while crunching.")
            .getDouble();
        ComputeSettings.set(s);

        final String ae2 = "ae2";
        enableComputeRequirement = c
            .get(ae2, "enableComputeRequirement", enableComputeRequirement, "Off: AE2 is stock.")
            .getBoolean();
        legacyPlanningUnmetered = c
            .get(ae2, "legacyPlanningUnmetered", legacyPlanningUnmetered, "Networks with legacy CPUs plan freely.")
            .getBoolean();
        meterNewCraftingBlocks = c
            .get(ae2, "meterNewCraftingBlocks", meterNewCraftingBlocks, "Meter crafting CPU blocks placed from now.")
            .getBoolean();
        meteredCpuFactor = c.get(ae2, "meteredCpuFactor", meteredCpuFactor, "What a metered CPU block is worth.")
            .getDouble();
        legacyCpuFactor = c.get(ae2, "legacyCpuFactor", legacyCpuFactor, "What a legacy CPU block is worth.")
            .getDouble();
        meteredCoProcessorCap = c
            .get(ae2, "meteredCoProcessorCap", meteredCoProcessorCap, "Co-processing cap of a metered cluster.")
            .getInt();
        meteredClusterPlanningRate = c
            .get(ae2, "meteredClusterPlanningRate", meteredClusterPlanningRate, "Planning a stock cluster adds.")
            .getDouble();

        final String workforce = "workforce";
        workersInvulnerable = c
            .get(workforce, "workersInvulnerable", workersInvulnerable, "Workers on shift take no damage.")
            .getBoolean();
        assignRange = c.get(workforce, "assignRange", assignRange, "How far a signed contract reaches.")
            .getDouble();
        bossClasses = c.get(workforce, "bossClasses", bossClasses, "Mob classes that may be directors.")
            .getStringList();

        final String desk = "desk";
        deskBytesPerBook = c.get(desk, "bytesPerBook", deskBytesPerBook, "Filing per book in a bookcase.")
            .getInt();
        deskBytesPerBookshelf = c.get(desk, "bytesPerBookshelf", deskBytesPerBookshelf, "Filing per vanilla bookshelf.")
            .getInt();
        deskFilingCap = c.get(desk, "filingCap", (int) deskFilingCap, "A desk's Filing cap.")
            .getInt();
        deskBellBonus = c.get(desk, "bellBonus", deskBellBonus, "Dispatch with a desk bell.")
            .getDouble();
        mainframeFilingCap = c.get("mainframe", "filingCap", (int) mainframeFilingCap, "A Mainframe's Filing cap.")
            .getInt();
        internetFilingCap = c.get("internet", "filingCap", (int) internetFilingCap, "The Internet's Filing cap.")
            .getInt();
        gigOpsPerEmerald = c.get("internet", "gigOpsPerEmerald", gigOpsPerEmerald, "Gig operations an emerald pays.")
            .getInt();
        final String dc = "datacenter";
        dataCenterMaxLength = c.get(dc, "maxLength", dataCenterMaxLength, "Longest hall, end walls included.")
            .getInt();
        dataCenterSlots = c.get(dc, "slots", dataCenterSlots, "Job slots a hall.")
            .getInt();
        dataCenterFiling = c.get(dc, "filing", (int) dataCenterFiling, "Filing a hall, in bytes.")
            .getInt();
        dataCenterFtePerCard = c.get(dc, "ftePerCard", dataCenterFtePerCard, "Synthetic FTE per Accelerator Card.")
            .getDouble();
        dataCenterPollution = c.get(dc, "pollution", dataCenterPollution, "Pollution a second at full load.")
            .getInt();
        breakRoomRange = c.get("breakroom", "range", breakRoomRange, "How far a Break Room Counter serves.")
            .getDouble();
        coffeeWorkerDays = c
            .get("breakroom", "coffeeWorkerDays", coffeeWorkerDays, "Worker-days one cup of coffee lasts.")
            .getInt();
        for (final ItemList list : ItemList.values()) {
            list.configure(
                c.get("items", list.name(), list.getNames(), "Items for " + list.name() + ": name or name:meta.")
                    .getStringList());
        }

        for (final Furniture role : Furniture.values()) {
            role.configure(
                c.get("furniture", role.name(), role.getNames(), "Registry names of " + role.name() + " blocks.")
                    .getStringList());
        }
        if (c.hasChanged()) {
            c.save();
        }
    }
}
