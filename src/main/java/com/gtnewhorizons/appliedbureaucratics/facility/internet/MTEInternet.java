package com.gtnewhorizons.appliedbureaucratics.facility.internet;

import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
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
import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;
import com.gtnewhorizons.appliedbureaucratics.facility.Bottleneck;
import com.gtnewhorizons.appliedbureaucratics.facility.MTEComputeFacility;
import com.gtnewhorizons.appliedbureaucratics.facility.Workstations;
import com.gtnewhorizons.appliedbureaucratics.facility.mainframe.MTEMainframe;
import com.gtnewhorizons.appliedbureaucratics.item.ItemComponent;
import com.gtnewhorizons.appliedbureaucratics.workforce.Employment;
import com.gtnewhorizons.appliedbureaucratics.workforce.ManagerKind;

import appeng.me.cluster.implementations.HostedCraftingCPUCluster;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;

/*-
Note [The Internet] (generated)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The IV facility (design document, section 6.3): "It's Web Scale!"  A 5 x 5 tower, 9 high (MTEInternet): steel frame
corners, Tube Casing walls, a Backbone Core up the middle and a Satellite Dish on top, which must see the sky.

Federation.  A Router Port in the wall is an OC node of its own (TileRouterPort), so the networks of different ports
never merge.  Run OC cable from a port to a Mainframe's terminal network: a port whose network holds exactly one
Terminal Concentrator links that concentrator's Mainframe.  Up to 8.

Pooling.  Linked Mainframes are one facility.  Every second the Internet calls MTEMainframe.linkTo on each; a linked
Mainframe gives up its own job slot (FacilitySlots shrink it when idle) and reports no planning of its own to the
network, and the Internet reports the sum of theirs, with no committee penalty between them.  The Internet has one
job slot per linked Mainframe, and their dispatch, pooled.  Its Filing is the Mainframes' plus the Disk Arrays in its
own input buses.

Sign-off.  Without an executive, one job may use at most the Filing of the largest single Mainframe
(FacilitySlots.setJobCap).  An executive (a demon, the only kind, at a seat with a desk and a bell within 6 blocks of
the controller: the boardroom) lets a job span the whole pool.

The gig economy.  Up to 4 Gig Kiosks within 32 blocks enrol the uncontracted villagers near them.  Each is 0.5 FTE of
dispatch only, there about half the time, and paid per operation from the input buses: an emerald pays for
ABConfig.gigOpsPerEmerald operations.  Unpaid, they log off.

Wrinkles:
  (W1) A link lasts 3 seconds unless renewed (MTEMainframe.isLinked), so a Mainframe whose port or Internet goes away
       takes its slot back on its own.
  (W2) The Mainframes plan when the Internet's slots are idle, not their own (planningShareForMembers).
*/

/// The Internet. See Note [The Internet].
public class MTEInternet extends MTEComputeFacility<MTEInternet> implements ISurvivalConstructable {

    private static final String MAIN = "main";
    private static IStructureDefinition<MTEInternet> STRUCTURE;
    private static final String[] FRONT = { "FWWWF", "FWWWF", "FWWWF", "FWWWF", "FWWWF", "FWWWF", "FWWWF", "FWWWF",
        "FW~WF" };
    private static final String[] SIDE = { "WWWWW", "W---W", "W---W", "W---W", "W---W", "W---W", "W---W", "W---W",
        "WWWWW" };
    private static final String[] CORE = { "WWDWW", "W-B-W", "W-B-W", "W-B-W", "W-B-W", "W-B-W", "W-B-W", "W-B-W",
        "WWWWW" };
    private static final String[] BACK = { "FWWWF", "FWWWF", "FWWWF", "FWWWF", "FWWWF", "FWWWF", "FWWWF", "FWWWF",
        "FWWWF" };
    private static final String[][] SHAPE = { FRONT, SIDE, CORE, SIDE, BACK };
    private static final long EU_PER_AMP = 8192;
    public static final int MAX_MAINFRAMES = 8;

    private final List<TileRouterPort> ports = new ArrayList<>();
    private final List<MTEMainframe> linked = new ArrayList<>();
    private final Random random = new Random();
    private int tubes;
    private boolean sky;
    private boolean executive;
    private long filing;
    private long jobCap;
    private double planning;
    private double dispatch;
    private double gigFte;
    private double gigDispatch;
    private int gigEnrolled;
    private double gigOpsOwed;
    private boolean gigPaid = true;
    private Bottleneck bottleneck = Bottleneck.NO_STRUCTURE;

    public MTEInternet(final int id, final String name, final String regionalName) {
        super(id, name, regionalName);
    }

    public MTEInternet(final String name) {
        super(name);
    }

    @Override
    public MetaTileEntity newMetaEntity(final IGregTechTileEntity tile) {
        return new MTEInternet(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Compute Facility, Internet")
            .addInfo("It's Web Scale! It is a series of tubes.")
            .addInfo("Links up to 8 Mainframes through Router Ports: OC cable to each Mainframe's terminal network")
            .addInfo("Linked Mainframes plan as one, pool their dispatch, and give the Internet one job slot each")
            .addInfo("Without an executive in the boardroom, a job may use one Mainframe's Filing at most")
            .addInfo("Boardroom: a demon at a seat with a desk and a bell within 6 blocks")
            .addInfo("Gig Kiosks within 32 blocks enrol villagers: 0.5 FTE of dispatch each, paid per operation")
            .addInfo("Input bus: Disk Arrays (Filing), emeralds (gig pay)")
            .addSeparator()
            .beginStructureBlock(5, 9, 5, false)
            .addController("Front, bottom middle")
            .addCasingInfoMin("Tube Casing", 40, false)
            .addOtherStructurePart("Steel Frame Box", "Corners")
            .addOtherStructurePart("Backbone Core", "Up the middle, 7")
            .addOtherStructurePart("Satellite Dish", "Top middle; needs open sky")
            .addOtherStructurePart("Router Port", "Any wall, 1 to 8")
            .addEnergyHatch("Any wall", 1)
            .addMaintenanceHatch("Any wall", 1)
            .addInputBus("Any wall", 1)
            .toolTipFinisher("Applied Bureaucratics");
        return tt;
    }

    @Override
    public ITexture[] getTexture(final IGregTechTileEntity tile, final ForgeDirection side, final ForgeDirection facing,
        final int colorIndex, final boolean active, final boolean redstone) {
        final ITexture casing = Textures.BlockIcons.getCasingTextureForId(Casing.TUBE.textureIndex());
        if (side == facing) {
            return new ITexture[] { casing,
                TextureFactory.of(active ? ABTextures.MAINFRAME_FRONT_ACTIVE : ABTextures.MAINFRAME_FRONT) };
        }
        return new ITexture[] { casing };
    }

    @Override
    public IStructureDefinition<MTEInternet> getStructureDefinition() {
        if (STRUCTURE == null) {
            STRUCTURE = StructureDefinition.<MTEInternet>builder()
                .addShape(MAIN, SHAPE)
                .addElement('F', ofFrame(Materials.Steel))
                .addElement(
                    'W',
                    StructureUtility.ofChain(
                        StructureUtility.ofTileAdder(MTEInternet::addPort, ModBlocks.routerPort, 0),
                        buildHatchAdder(MTEInternet.class).atLeast(Energy, Maintenance, InputBus, OutputBus)
                            .casingIndex(Casing.TUBE.textureIndex())
                            .hint(1)
                            .buildAndChain(
                                StructureUtility.onElementPass(
                                    t -> t.tubes++,
                                    StructureUtility.ofBlock(ModBlocks.casing, Casing.TUBE.meta())))))
                .addElement('B', StructureUtility.ofBlock(ModBlocks.casing, Casing.BACKBONE.meta()))
                .addElement('D', StructureUtility.ofBlock(ModBlocks.casing, Casing.DISH.meta()))
                .addElement('-', StructureUtility.isAir())
                .build();
        }
        return STRUCTURE;
    }

    private boolean addPort(final @Nullable TileEntity tile) {
        if (tile instanceof TileRouterPort port && this.ports.size() < MAX_MAINFRAMES) {
            this.ports.add(port);
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
        this.tubes = 0;
        this.ports.clear();
        if (!this.checkPiece(MAIN, 2, 8, 0, errors)) {
            return;
        }
        this.checkCasingMin(errors, this.tubes, 40);
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
        this.buildPiece(MAIN, stack, hintsOnly, 2, 8, 0);
    }

    @Override
    public int survivalConstruct(final ItemStack stack, final int budget, final ISurvivalBuildEnvironment env) {
        if (this.mMachine) {
            return -1;
        }
        return this.survivialBuildPiece(MAIN, stack, 2, 8, 0, budget, env, false, true);
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        this.mMaxProgresstime = 20;
        this.mEfficiency = 10000 - (this.getIdealStatus() - this.getRepairStatus()) * 1000;
        this.mEfficiencyIncrease = 10000;
        this.mEUt = (int) -(EU_PER_AMP * (2 + this.linked.size()) * 9 / 10);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    protected int structureReach() {
        return 10;
    }

    @Override
    protected void facilityTick() {
        if (this.ticks % 20 == 0) {
            this.recompute();
        }
    }

    /// The share of their FTE the linked Mainframes spend planning, from the Internet's slots. See wrinkle (W2) of
    /// Note [The Internet].
    public double planningShareForMembers() {
        return this.planningShare();
    }

    private void recompute() {
        this.linked.clear();
        if (this.mMachine) {
            final Set<MTEMainframe> found = new LinkedHashSet<>();
            for (final TileRouterPort port : this.ports) {
                final MTEMainframe mainframe = port.linkedMainframe();
                if (mainframe != null && !port.isInvalid()) {
                    found.add(mainframe);
                }
            }
            this.linked.addAll(found);
            for (final MTEMainframe mainframe : this.linked) {
                mainframe.linkTo(this);
            }
            this.sky = this.dishSeesSky();
        }
        this.slots.setSlotCount(this.linked.size());
        if (!this.mMachine) {
            this.setOff(Bottleneck.NO_STRUCTURE);
            return;
        }
        if (!this.isRunning()) {
            this.setOff(Bottleneck.NO_POWER);
            return;
        }
        if (!this.sky) {
            this.setOff(Bottleneck.NO_STRUCTURE);
            return;
        }
        double planning = 0;
        double dispatch = 0;
        long filing = this.countInputs(ItemComponent.Kind.DISK_ARRAY::is, s -> ItemComponent.Kind.DISK_ARRAY.filing);
        long largest = 0;
        for (final MTEMainframe mainframe : this.linked) {
            planning += mainframe.getOutput()
                .planningRate();
            dispatch += mainframe.getOutput()
                .dispatchRate();
            filing += mainframe.getFiling();
            largest = Math.max(largest, mainframe.getFiling());
        }
        this.executive = this.hasExecutive();
        this.updateGig();
        this.planning = planning;
        this.dispatch = dispatch + this.gigDispatch;
        this.filing = Math.min(filing, ABConfig.internetFilingCap);
        this.jobCap = this.executive ? this.filing : largest;
        this.slots.setFiling(this.filing);
        this.slots.setJobCap(this.jobCap);
        this.slots.setDispatchRate(this.dispatch);
        this.bottleneck = this.linked.isEmpty() ? Bottleneck.NO_WORKER : Bottleneck.NONE;
    }

    private void setOff(final Bottleneck why) {
        this.planning = 0;
        this.dispatch = 0;
        this.slots.setDispatchRate(0);
        this.bottleneck = why;
    }

    /// The Satellite Dish is at the top middle, 8 above and 2 behind the controller.
    private boolean dishSeesSky() {
        final ForgeDirection back = this.getBaseMetaTileEntity()
            .getFrontFacing()
            .getOpposite();
        final int x = this.x() + 2 * back.offsetX;
        final int y = this.y() + 8;
        final int z = this.z() + 2 * back.offsetZ;
        return this.world()
            .canBlockSeeTheSky(x, y + 1, z);
    }

    private boolean hasExecutive() {
        for (final Workstations.Seated seated : Workstations
            .within(this.world(), this.x() + 0.5, this.y() + 0.5, this.z() + 0.5, 6)) {
            if (seated.employment()
                .getRole() == Employment.Role.MANAGER && ManagerKind.of(seated.mob()) == ManagerKind.DEMON
                && isManagerOffice(this.world(), seated)) {
                return true;
            }
        }
        return false;
    }

    private void updateGig() {
        int enrolled = 0;
        int kiosks = 0;
        for (final TileGigKiosk kiosk : TileGigKiosk.in(this.world())) {
            if (kiosks >= 4) {
                break;
            }
            if (!kiosk.isInvalid() && kiosk.getDistanceFrom(this.x(), this.y(), this.z()) <= 32 * 32) {
                kiosks++;
                enrolled += kiosk.getEnrolled();
            }
        }
        this.gigEnrolled = enrolled;
        // Each gig worker is there about half the time.
        int available = 0;
        for (int i = 0; i < enrolled; i++) {
            if (this.random.nextBoolean()) {
                available++;
            }
        }
        if (this.gigOpsOwed >= ABConfig.gigOpsPerEmerald) {
            final int due = (int) (this.gigOpsOwed / ABConfig.gigOpsPerEmerald);
            final int paid = this.takeFromInputs(s -> s.getItem() == Items.emerald, due);
            this.gigOpsOwed -= (double) paid * ABConfig.gigOpsPerEmerald;
            this.gigPaid = paid == due;
        }
        this.gigFte = this.gigPaid ? available * 0.5 : 0;
        this.gigDispatch = this.gigFte * com.gtnewhorizons.appliedbureaucratics.compute.ComputeSettings.get().kDisp;
    }

    @Override
    public void operationsUsed(final HostedCraftingCPUCluster cpu, final int used) {
        if (this.dispatch > 0 && this.gigDispatch > 0) {
            this.gigOpsOwed += used * this.gigDispatch / this.dispatch;
        }
    }

    @Override
    public double getPlanningRate() {
        return this.planning;
    }

    @Override
    public boolean isStaffed() {
        return true;
    }

    @Override
    public boolean couldPlan() {
        if (!this.isRunning() || !this.sky) {
            return false;
        }
        for (final MTEMainframe mainframe : this.linked) {
            if (mainframe.officeCouldPlan()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void planningCharged(final double steps) {
        if (this.planning <= 0) {
            return;
        }
        for (final MTEMainframe mainframe : this.linked) {
            mainframe.planningCharged(
                steps * mainframe.getOutput()
                    .planningRate() / this.planning);
        }
    }

    @Override
    public @Nullable String getHostedCPUName(final HostedCraftingCPUCluster cpu) {
        final int i = new ArrayList<>(this.slots.getCPUs()).indexOf(cpu);
        return String.format(Locale.ROOT, "shard-%02d.internet", Math.max(0, i) + 1);
    }

    @Override
    public List<String> statusLines() {
        final List<String> lines = new ArrayList<>();
        lines.add(StatCollector.translateToLocal(this.bottleneck.key()));
        lines.add(
            String.format(
                Locale.ROOT,
                "  planning %.2f steps/s, dispatch %.2f ops/s, filing %d/%d B, job cap %d B, slots %d, slots busy %d",
                this.planning,
                this.dispatch,
                this.slots.getUsedFiling(),
                this.filing,
                this.jobCap,
                this.slots.getCPUs()
                    .size(),
                this.slots.getBusyCount()));
        lines.add(
            String.format(
                Locale.ROOT,
                "  internet: %d ports, %d mainframes, executive %s, sky %s, gig %d enrolled %.1f FTE%s",
                this.ports.size(),
                this.linked.size(),
                this.executive ? "yes" : "no",
                this.sky ? "yes" : "no",
                this.gigEnrolled,
                this.gigFte,
                this.gigPaid ? "" : " (unpaid, logged off)"));
        return lines;
    }

    @Override
    public String[] getInfoData() {
        final List<String> lines = new ArrayList<>(java.util.Arrays.asList(super.getInfoData()));
        lines.addAll(this.statusLines());
        return lines.toArray(new String[0]);
    }

    @Override
    public void saveNBTData(final NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setDouble("gigOpsOwed", this.gigOpsOwed);
    }

    @Override
    public void loadNBTData(final NBTTagCompound tag) {
        super.loadNBTData(tag);
        this.gigOpsOwed = tag.getDouble("gigOpsOwed");
    }
}
