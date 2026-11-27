package com.gtnewhorizons.appliedbureaucratics.facility.mainframe;

import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizons.appliedbureaucratics.block.ABTextures;
import com.gtnewhorizons.appliedbureaucratics.block.BlockABCasing.Casing;
import com.gtnewhorizons.appliedbureaucratics.block.ModBlocks;
import com.gtnewhorizons.appliedbureaucratics.compute.ComputeSettings;
import com.gtnewhorizons.appliedbureaucratics.compute.FacilityOutput;
import com.gtnewhorizons.appliedbureaucratics.compute.Morale;
import com.gtnewhorizons.appliedbureaucratics.compute.MoraleTarget;
import com.gtnewhorizons.appliedbureaucratics.compute.OrgChart;
import com.gtnewhorizons.appliedbureaucratics.compute.OrgUnit;
import com.gtnewhorizons.appliedbureaucratics.compute.Payroll;
import com.gtnewhorizons.appliedbureaucratics.compute.Workforce;
import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;
import com.gtnewhorizons.appliedbureaucratics.config.ItemList;
import com.gtnewhorizons.appliedbureaucratics.facility.Amenities;
import com.gtnewhorizons.appliedbureaucratics.facility.Bottleneck;
import com.gtnewhorizons.appliedbureaucratics.facility.MTEComputeFacility;
import com.gtnewhorizons.appliedbureaucratics.facility.ManagerUpkeep;
import com.gtnewhorizons.appliedbureaucratics.facility.Workstations;
import com.gtnewhorizons.appliedbureaucratics.facility.breakroom.TileBreakRoomCounter;
import com.gtnewhorizons.appliedbureaucratics.facility.internet.MTEInternet;
import com.gtnewhorizons.appliedbureaucratics.item.ItemComponent;
import com.gtnewhorizons.appliedbureaucratics.workforce.Employment;
import com.gtnewhorizons.appliedbureaucratics.workforce.ManagerKind;

import appeng.me.cluster.implementations.HostedCraftingCPUCluster;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.internal.Server;

/*-
Note [The Mainframe] (generated)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The EV facility (design document, section 6.2): a GregTech multiblock (MTEMainframe) on the ME network with one job
slot, and an office around it.

The machine is 7 wide, 4 high and 3 deep.  Its front has a row of Tape Reel Windows and four OpenComputers racks
beside the controller; its back has the hatches and exactly one Terminal Concentrator, an OC node.  The servers in
the racks are its hardware capacity, in FTE: 8 / 16 / 32 for a tier 1 / 2 / 3 server (64 for a creative one).
Office FTE above the hardware is wasted, and each open maintenance issue takes 10 % of the hardware.

The office is freeform.  A terminal is an OC screen on the concentrator's network (TileTerminalConcentrator), with a
contracted worker seated within 2 blocks of it; a screen of tier 1 / 2 / 3 makes its worker worth 1 / 1.25 / 1.5 FTE.
A manager's office is a seat with a BiblioCraft desk and bell next to it, within 24 blocks of the controller; the org
chart (OrgChart) is built from where everyone sits, with leads and one level of directors.

The input buses hold everything the Mainframe uses: tape reels for Filing (counted, not consumed), paper and punch
cards for planning, emeralds for wages, ender pearls and gold for managers, and blank data tapes, which become Work
Logs in the output bus, one per 1000 operations dispatched (the Data Center's training data).

Wrinkles:
  (W1) The ME node is made ready whether or not the machine runs, so its job slot stays on the network and a job in
       it keeps its items while the machine is off; it only stops being granted operations.
  (W2) Racks are found by StructureLib's tile adder, which is also called where no tile is; racks without servers
       count as racks with no hardware.
*/

/// The Mainframe. See Note [The Mainframe].
public class MTEMainframe extends MTEComputeFacility<MTEMainframe> implements ISurvivalConstructable {

    private static final String MAIN = "main";
    private static IStructureDefinition<MTEMainframe> STRUCTURE;

    /// Front face (z = 0), middle and back, each top row first.
    private static final String[][] SHAPE = { { "CCCCCCC", "CTTTTTC", "CRR~RRC", "CCCCCCC" },
        { "CCCCCCC", "CCCCCCC", "C--C--C", "CCCCCCC" }, { "CCCCCCC", "CCCCCCC", "CCCXCCC", "CCCCCCC" } };

    private static final int EU_WORKING = 1920;
    private static final int EU_IDLE = 480;

    private final Morale morale = new Morale();
    private final Payroll payroll = new Payroll();
    private final ManagerUpkeep upkeep = new ManagerUpkeep();
    private int casings;
    private final List<Rack> racks = new ArrayList<>();
    private @Nullable TileTerminalConcentrator concentrator;
    private double hardwareFte;
    private int servers;
    private int terminals;
    private int workers;
    private OrgChart.Result org = new OrgChart.Result(new ArrayList<>(), 0, 0, 0, 0);
    private int amenities;
    private boolean crowded;
    private int managerPenalty;
    private boolean butcher;
    private int blacksmiths;
    private long filing;
    private boolean paper;
    private double sheetsOwed;
    private long opsSinceLog;
    private boolean coffee;
    private boolean party;
    private FacilityOutput output = FacilityOutput.OFF;
    private Bottleneck bottleneck = Bottleneck.NO_STRUCTURE;
    /// The Internet this Mainframe is linked to, and when it last said so. See Note [The Internet].
    private @Nullable MTEInternet link;
    private long linkedAt = -1000;

    public MTEMainframe(final int id, final String name, final String regionalName) {
        super(id, name, regionalName);
        this.slots.setSlotCount(1);
    }

    public MTEMainframe(final String name) {
        super(name);
        this.slots.setSlotCount(1);
    }

    @Override
    public MetaTileEntity newMetaEntity(final IGregTechTileEntity tile) {
        return new MTEMainframe(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Compute Facility, Mainframe")
            .addInfo("Nobody ever got fired for buying one.")
            .addInfo("One AE2 job slot, run by an office of villagers at OpenComputers terminals")
            .addInfo("Hardware capacity from the servers in its racks: 8 / 16 / 32 FTE per tier 1 / 2 / 3 server")
            .addInfo("Terminals: OC screens on the Terminal Concentrator's network, a contracted worker seated by each")
            .addInfo("Managers: a seat with a desk and a bell, within 24 blocks")
            .addInfo("Input bus: tape reels (Filing), paper or punch cards, emeralds, ender pearls, gold, data tapes")
            .addInfo("Output bus: Work Logs")
            .addSeparator()
            .beginStructureBlock(7, 4, 3, false)
            .addController("Front, bottom middle row")
            .addCasingInfoMin("Mainframe Casing", 30, false)
            .addOtherStructurePart("Tape Reel Window", "Front, second row")
            .addOtherStructurePart("OpenComputers Rack", "Front, beside the controller, 4")
            .addOtherStructurePart("Terminal Concentrator", "Back, middle of the second row from the bottom")
            .addEnergyHatch("Any casing", 1)
            .addMaintenanceHatch("Any casing", 1)
            .addInputBus("Any casing", 1)
            .addOutputBus("Any casing", 1)
            .toolTipFinisher("Applied Bureaucratics");
        return tt;
    }

    @Override
    public ITexture[] getTexture(final IGregTechTileEntity tile, final ForgeDirection side, final ForgeDirection facing,
        final int colorIndex, final boolean active, final boolean redstone) {
        final ITexture casing = Textures.BlockIcons.getCasingTextureForId(Casing.MAINFRAME.textureIndex());
        if (side == facing) {
            return new ITexture[] { casing,
                TextureFactory.of(active ? ABTextures.MAINFRAME_FRONT_ACTIVE : ABTextures.MAINFRAME_FRONT) };
        }
        return new ITexture[] { casing };
    }

    @Override
    public IStructureDefinition<MTEMainframe> getStructureDefinition() {
        if (STRUCTURE == null) {
            final Block rack = GameRegistry.findBlock("OpenComputers", "rack");
            final Block casing = ModBlocks.casing;
            STRUCTURE = StructureDefinition.<MTEMainframe>builder()
                .addShape(MAIN, SHAPE)
                .addElement(
                    'C',
                    buildHatchAdder(MTEMainframe.class).atLeast(Energy, Maintenance, InputBus, OutputBus)
                        .casingIndex(Casing.MAINFRAME.textureIndex())
                        .hint(1)
                        .buildAndChain(
                            StructureUtility.onElementPass(
                                t -> t.casings++,
                                StructureUtility.ofBlock(casing, Casing.MAINFRAME.meta()))))
                .addElement('T', StructureUtility.ofBlock(casing, Casing.TAPE_WINDOW.meta()))
                .addElement(
                    'R',
                    StructureUtility.ofChain(
                        StructureUtility.ofTileAdder(MTEMainframe::addRack, rack != null ? rack : casing, 0),
                        StructureUtility.ofBlock(rack != null ? rack : casing, 0)))
                .addElement(
                    'X',
                    StructureUtility.ofTileAdder(MTEMainframe::addConcentrator, ModBlocks.terminalConcentrator, 0))
                .addElement('-', StructureUtility.isAir())
                .build();
        }
        return STRUCTURE;
    }

    private boolean addRack(final @Nullable TileEntity tile) {
        if (tile instanceof Rack rack) {
            this.racks.add(rack);
            return true;
        }
        return false;
    }

    private boolean addConcentrator(final @Nullable TileEntity tile) {
        if (tile instanceof TileTerminalConcentrator c && this.concentrator == null) {
            this.concentrator = c;
            c.setOwner(this);
            return true;
        }
        return false;
    }

    @Override
    public void checkMachine(final IGregTechTileEntity tile, final ItemStack stack, final List<StructureError> errors) {
        this.mEnergyHatches.clear();
        this.mInputBusses.clear();
        this.mOutputBusses.clear();
        this.mMaintenanceHatches.clear();
        this.casings = 0;
        this.racks.clear();
        this.concentrator = null;
        if (!this.checkPiece(MAIN, 3, 2, 0, errors)) {
            return;
        }
        this.checkCasingMin(errors, this.casings, 30);
        this.checkHasEnergyHatch(errors);
        this.checkOneMaintenanceHatch(errors);
        this.checkHasInputBus(errors);
        if (errors.isEmpty()) {
            this.checkMaintenance();
        }
    }

    @Override
    public boolean checkMachine(final IGregTechTileEntity tile, final ItemStack stack) {
        return this.checkMachine_TT(tile);
    }

    @Override
    public void construct(final ItemStack stack, final boolean hintsOnly) {
        this.buildPiece(MAIN, stack, hintsOnly, 3, 2, 0);
    }

    @Override
    public int survivalConstruct(final ItemStack stack, final int budget, final ISurvivalBuildEnvironment env) {
        if (this.mMachine) {
            return -1;
        }
        return this.survivialBuildPiece(MAIN, stack, 3, 2, 0, budget, env, false, true);
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        this.mMaxProgresstime = 20;
        this.mEfficiency = 10000 - (this.getIdealStatus() - this.getRepairStatus()) * 1000;
        this.mEfficiencyIncrease = 10000;
        this.mEUt = -(this.slots.anyBusy() ? EU_WORKING : EU_IDLE);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    // The facility.

    @Override
    protected void facilityTick() {
        this.slots.setSlotCount(this.isLinked() ? 0 : 1);
        if (this.ticks % 100 == 0 && this.mMachine) {
            this.surveyOffice();
        }
        if (this.ticks % 20 == 0) {
            this.recompute();
        }
        if (this.ticks % 1200 == 0 && this.ticks > 0) {
            this.morale.drift(this.moraleTarget(), 1, this.butcher ? 0.5 : 1);
            this.upkeep.tick(1200, this::takeFromInputs);
        }
    }

    /// Links this Mainframe to `internet` until it stops saying so: its job slot is replaced by the Internet's and
    /// its planning goes through it. See Note [The Internet].
    public void linkTo(final MTEInternet internet) {
        this.link = internet;
        this.linkedAt = this.world()
            .getTotalWorldTime();
    }

    public boolean isLinked() {
        return this.link != null && this.world() != null
            && this.world()
                .getTotalWorldTime() - this.linkedAt < 60;
    }

    /// What the office produces, whether or not it is linked.
    public FacilityOutput getOutput() {
        return this.output;
    }

    public long getFiling() {
        return this.filing;
    }

    @Override
    protected double planningShare() {
        final MTEInternet internet = this.link;
        return this.isLinked() && internet != null ? internet.planningShareForMembers() : super.planningShare();
    }

    private void surveyOffice() {
        final World world = this.getBaseMetaTileEntity()
            .getWorld();
        final IGregTechTileEntity tile = this.getBaseMetaTileEntity();
        this.hardwareFte = 0;
        this.servers = 0;
        this.blacksmiths = 0;
        for (final Rack rack : this.racks) {
            for (int slot = 0; slot < 4; slot++) {
                if (rack.getMountable(slot) instanceof Server server) {
                    this.servers++;
                    this.hardwareFte += switch (server.tier()) {
                        case 0 -> 8;
                        case 1 -> 16;
                        case 2 -> 32;
                        default -> 64;
                    };
                }
            }
        }
        final List<OrgChart.Worker> staff = new ArrayList<>();
        final Set<Workstations.Seated> counted = new HashSet<>();
        int amenitySum = 0;
        int crowdedSeats = 0;
        boolean hasButcher = false;
        final List<TileTerminalConcentrator.Terminal> screens = this.concentrator != null
            ? this.concentrator.getTerminals()
            : new ArrayList<>();
        for (final TileTerminalConcentrator.Terminal screen : screens) {
            Workstations.Seated best = null;
            for (final Workstations.Seated seated : Workstations
                .within(world, screen.x() + 0.5, screen.y() + 0.5, screen.z() + 0.5, 2.5)) {
                if (seated.employment()
                    .getRole() == Employment.Role.WORKER && !counted.contains(seated)
                    && (best == null || seated.distanceSq(screen.x() + 0.5, screen.y() + 0.5, screen.z() + 0.5)
                        < best.distanceSq(screen.x() + 0.5, screen.y() + 0.5, screen.z() + 0.5))) {
                    best = seated;
                }
            }
            if (best == null) {
                continue;
            }
            counted.add(best);
            final double fte = switch (screen.tier()) {
                case 2 -> 1.25;
                case 3 -> 1.5;
                default -> 1.0;
            };
            staff.add(new OrgChart.Worker(best.x(), best.y(), best.z(), fte, Workstations.affinity(best.mob())));
            amenitySum += Amenities.score(world, best.x(), best.y(), best.z());
            if (Amenities.crowded(world, best.x(), best.y(), best.z())) {
                crowdedSeats++;
            }
            final int profession = Workstations.profession(best.mob());
            hasButcher |= profession == 4;
            if (profession == 3) {
                this.blacksmiths++;
            }
        }
        final List<OrgChart.Manager> managers = new ArrayList<>();
        final List<ManagerUpkeep.Upkept> kinds = new ArrayList<>();
        final java.util.EnumSet<ManagerKind> present = java.util.EnumSet.noneOf(ManagerKind.class);
        for (final Workstations.Seated seated : Workstations
            .within(world, tile.getXCoord() + 0.5, tile.getYCoord() + 0.5, tile.getZCoord() + 0.5, 24)) {
            final ManagerKind kind = ManagerKind.of(seated.mob());
            if (seated.employment()
                .getRole() != Employment.Role.MANAGER || kind == null
                || !isManagerOffice(world, seated)) {
                continue;
            }
            kinds.add(
                new ManagerUpkeep.Upkept(
                    kind,
                    seated.employment()
                        .getEmployer()));
            managers.add(
                new OrgChart.Manager(
                    seated.x(),
                    seated.y(),
                    seated.z(),
                    kind.efficiency,
                    kind.roles,
                    this.upkeep.isPaid(kind)));
            present.add(kind);
        }
        int penalty = 0;
        for (final ManagerKind kind : present) {
            penalty += kind.moralePenalty;
        }
        this.upkeep.setManagers(kinds);
        this.org = OrgChart.build(staff, managers, 2);
        this.terminals = screens.size();
        this.workers = staff.size();
        this.amenities = staff.isEmpty() ? 0 : amenitySum / staff.size();
        this.crowded = crowdedSeats * 2 > staff.size() && !staff.isEmpty();
        this.managerPenalty = penalty;
        this.butcher = hasButcher;
    }

    private void recompute() {
        final World world = this.getBaseMetaTileEntity()
            .getWorld();
        final IGregTechTileEntity tile = this.getBaseMetaTileEntity();
        this.filing = 0;
        this.paper = false;
        if (this.mMachine) {
            for (final ItemStack stack : this.getStoredInputs()) {
                final ItemComponent.Kind kind = ItemComponent.Kind.of(stack);
                if (kind != null && kind.filing > 0) {
                    this.filing += kind.filing * stack.stackSize;
                } else
                    if (stack != null && (ItemList.PAPER.matches(stack) || ItemComponent.Kind.PUNCH_CARD.is(stack))) {
                        this.paper = true;
                    }
            }
            this.filing = Math.min(this.filing, ABConfig.mainframeFilingCap);
            this.takePaper();
        }
        this.slots.setFiling(this.filing);
        final TileBreakRoomCounter counter = world == null ? null
            : TileBreakRoomCounter.near(world, tile.getXCoord(), tile.getYCoord(), tile.getZCoord());
        this.coffee = counter != null && counter.serveCoffee(this.workers, 20);
        this.party = counter != null && this.workers > 0 && counter.party();
        if (this.concentrator != null) {
            this.concentrator.setPowered(this.isRunning());
        }
        if (!this.mMachine) {
            this.setOff(Bottleneck.NO_STRUCTURE);
            return;
        }
        if (!this.isRunning()) {
            this.setOff(Bottleneck.NO_POWER);
            this.showScreens();
            return;
        }
        if (this.workers == 0) {
            this.setOff(Bottleneck.NO_WORKER);
            this.showScreens();
            return;
        }
        if (this.hardwareFte <= 0) {
            this.setOff(Bottleneck.NO_HARDWARE);
            this.showScreens();
            return;
        }
        this.payroll.accrue(this.workers, 20);
        this.payWages();
        final Workforce.Split raw = Workforce.split(this.org.roots(), this.planningShare());
        final int issues = this.getIdealStatus() - this.getRepairStatus();
        final double hardware = this.hardwareFte * (1 + 0.05 * Math.min(4, this.blacksmiths))
            * Math.max(0, 1 - 0.1 * issues);
        final double scale = raw.facilityFte() > hardware ? hardware / raw.facilityFte() : 1;
        final Workforce.Split split = new Workforce.Split(
            raw.facilityFte() * scale,
            raw.planningFte() * scale,
            raw.dispatchFte() * scale);
        this.output = FacilityOutput.compute(split, this.morale.multiplier(), this.paper, this.isCrunching(), 1, 1);
        this.slots.setDispatchRate(this.output.dispatchRate());
        this.bottleneck = scale < 1 ? Bottleneck.NO_HARDWARE
            : !this.paper ? Bottleneck.NO_PAPER
                : !this.payroll.isPaid() ? Bottleneck.UNPAID
                    : this.morale.get() < 40 ? Bottleneck.LOW_MORALE
                        : this.filing <= 0 ? Bottleneck.NO_FILING : Bottleneck.NONE;
        this.showScreens();
    }

    private void setOff(final Bottleneck why) {
        this.output = FacilityOutput.OFF;
        this.slots.setDispatchRate(0);
        this.bottleneck = why;
    }

    private boolean isCrunching() {
        return this.getBaseMetaTileEntity()
            .getRedstone();
    }

    private double moraleTarget() {
        return new MoraleTarget(
            this.payroll.isPaid(),
            this.amenities,
            this.coffee,
            this.party,
            this.crowded,
            Amenities.smog(
                this.getBaseMetaTileEntity()
                    .getWorld(),
                this.getBaseMetaTileEntity()
                    .getXCoord(),
                this.getBaseMetaTileEntity()
                    .getZCoord()),
            this.managerPenalty,
            this.isCrunching()).value();
    }

    private void payWages() {
        final long due = this.payroll.emeraldsDue();
        if (due > 0) {
            this.payroll.pay(this.takeFromInputs(s -> s.getItem() == Items.emerald, (int) Math.min(64, due)));
        }
    }

    private void takePaper() {
        int owed = (int) Math.floor(this.sheetsOwed);
        if (owed <= 0) {
            return;
        }
        final int cards = this.takeFromInputs(ItemComponent.Kind.PUNCH_CARD::is, owed / 4);
        owed -= cards * 4;
        final int sheets = this.takeFromInputs(ItemList.PAPER::matches, owed);
        this.sheetsOwed -= cards * 4 + sheets;
    }

    private void showScreens() {
        if (this.concentrator == null) {
            return;
        }
        final List<String> lines = new ArrayList<>();
        if (!this.isRunning()) {
            lines.add("");
        } else if (this.slots.anyBusy()) {
            lines.add("== REQUISITION SYSTEM 2.1 ==   [F3] SUBMIT  [F10] LOGOFF");
            for (final HostedCraftingCPUCluster cpu : this.slots.getCPUs()) {
                if (cpu.isBusy() && cpu.getFinalMultiOutput() != null) {
                    lines.add(
                        "JOB: " + cpu.getFinalMultiOutput()
                            .getStackSize()
                            + " x "
                            + cpu.getFinalMultiOutput()
                                .getDisplayName());
                }
            }
            lines.add(
                String.format(
                    "DISPATCH: %.1f OPS/S   PLANNING: %.1f STEPS/S",
                    this.output.dispatchRate(),
                    this.output.planningRate()));
            lines.add("STATUS: " + net.minecraft.util.StatCollector.translateToLocal(this.bottleneck.key()));
        } else {
            lines.add("SOLITAIRE                                   SCORE: 4096");
            lines.add("[K♠] [ ] [7♥] [ ] [ ] [ ] [ ]    [A♣] [A♦] [ ] [ ]");
            lines.add("     [Q♦] [6♠]");
            lines.add("          [5♥]");
        }
        this.concentrator.show(lines);
    }

    /// A linked Mainframe plans through its Internet, which reports the planning instead.
    @Override
    public double getPlanningRate() {
        return this.isLinked() ? 0 : this.output.planningRate();
    }

    @Override
    public boolean isStaffed() {
        return true;
    }

    @Override
    public void planningCharged(final double steps) {
        this.sheetsOwed += steps / ComputeSettings.get().stepsPerSheet;
    }

    @Override
    public List<String> statusLines() {
        final List<String> lines = new ArrayList<>();
        lines.add(net.minecraft.util.StatCollector.translateToLocal(this.bottleneck.key()));
        lines.add(
            String.format(
                java.util.Locale.ROOT,
                "  planning %.2f steps/s, dispatch %.2f ops/s, filing %d/%d B, morale %.0f, %s, worker staff, slots busy %d%s",
                this.output.planningRate(),
                this.output.dispatchRate(),
                this.slots.getUsedFiling(),
                this.slots.getFiling(),
                this.morale.get(),
                this.payroll.isPaid() ? "paid" : "unpaid",
                this.slots.getBusyCount(),
                this.isCrunching() ? ", crunch" : ""));
        lines.add(
            String.format(
                java.util.Locale.ROOT,
                "  office: %d terminals, %d workers, %d leads, %d directors, %d unmanaged, hardware %.0f FTE, fte %.2f, racks %d servers %d",
                this.terminals,
                this.workers,
                this.org.leads(),
                this.org.directors(),
                this.org.unmanaged(),
                this.hardwareFte,
                OrgUnit.facilityFte(this.org.roots()),
                this.racks.size(),
                this.servers));
        return lines;
    }

    @Override
    public String[] getInfoData() {
        final List<String> lines = new ArrayList<>(java.util.Arrays.asList(super.getInfoData()));
        lines.addAll(this.statusLines());
        return lines.toArray(new String[0]);
    }

    @Override
    public void operationsUsed(final HostedCraftingCPUCluster cpu, final int used) {
        this.opsSinceLog += used;
        if (this.opsSinceLog >= 1000) {
            this.opsSinceLog -= 1000;
            if (this.takeFromInputs(ItemComponent.Kind.BLANK_DATA_TAPE::is, 1) == 1) {
                this.addOutputPartial(ItemComponent.Kind.WORK_LOG.stack(1));
            }
        }
    }

    @Override
    public @Nullable String getHostedCPUName(final HostedCraftingCPUCluster cpu) {
        return "Big Iron";
    }

    // Saving.

    @Override
    public void saveNBTData(final NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setDouble("morale", this.morale.get());
        tag.setLong("wageTicks", this.payroll.getWorkerTicks());
        tag.setLong("wagesOwed", this.payroll.getOwed());
        tag.setLong("wagesPrepaid", this.payroll.getPrepaid());
        tag.setDouble("sheetsOwed", this.sheetsOwed);
        tag.setLong("opsSinceLog", this.opsSinceLog);
        this.upkeep.writeToNBT(tag);
    }

    @Override
    public void loadNBTData(final NBTTagCompound tag) {
        super.loadNBTData(tag);
        this.morale.set(tag.hasKey("morale") ? tag.getDouble("morale") : Morale.START);
        this.payroll.load(tag.getLong("wageTicks"), tag.getLong("wagesOwed"), tag.getLong("wagesPrepaid"));
        this.sheetsOwed = tag.getDouble("sheetsOwed");
        this.opsSinceLog = tag.getLong("opsSinceLog");
        this.upkeep.readFromNBT(tag);
    }
}
