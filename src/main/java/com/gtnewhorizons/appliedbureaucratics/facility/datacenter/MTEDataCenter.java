package com.gtnewhorizons.appliedbureaucratics.facility.datacenter;

import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.Muffler;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizons.appliedbureaucratics.block.ABTextures;
import com.gtnewhorizons.appliedbureaucratics.block.BlockABCasing.Casing;
import com.gtnewhorizons.appliedbureaucratics.block.ModBlocks;
import com.gtnewhorizons.appliedbureaucratics.compute.ComputeSettings;
import com.gtnewhorizons.appliedbureaucratics.compute.FacilityOutput;
import com.gtnewhorizons.appliedbureaucratics.compute.OrgUnit;
import com.gtnewhorizons.appliedbureaucratics.compute.Training;
import com.gtnewhorizons.appliedbureaucratics.compute.Workforce;
import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;
import com.gtnewhorizons.appliedbureaucratics.facility.Bottleneck;
import com.gtnewhorizons.appliedbureaucratics.facility.MTEComputeFacility;
import com.gtnewhorizons.appliedbureaucratics.item.ItemComponent;

import appeng.me.cluster.implementations.HostedCraftingCPUCluster;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.pollution.Pollution;

/*-
Note [The Data Center] (generated)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The LuV facility (design document, section 6.4): "We are pleased to announce a reduction in headcount."  One hall,
15 wide and 7 high, between two end walls, 7 to 31 long (ABConfig.dataCenterMaxLength); the controller is in the
middle of the front end wall.  Each slice of the hall is

    W W W W W W W W W W W W W W W        W  Data Hall Casing, or a hatch
    W . . . . . . . . . . . . . W        A  Accelerator Rack (TileAcceleratorRack, 4 card slots)
    W A A . A A . A . A A . A A W        .  air: the aisles
    W A A . A A . A . A A . A A W        F  Raised Floor Casing; coolant runs under it
    W A A . A A . A . A A . A A W
    F F F F F F F F F F F F F F F
    W W W W W W W W W W W W W W W

and MTEDataCenter checks slices one by one behind the front wall until it finds the back wall.  The rows are
symmetric, so a hall reads the same whichever way it faces.

Capacity is synthetic FTE: ABConfig.dataCenterFtePerCard per Accelerator Card in its racks (about 400 FTE for a full 31
long hall), with no staff, wages or morale; it plans without a committee penalty.  8 job slots and 64 M of Filing a hall.

Upkeep, each scaled by load (busy slots, or plans on the network):
  Power        4 A LuV at full load, a tenth of that idle.
  Coolant      1000 L/s of water from its input hatches, hot water out; distilled water counts double.  Short of
               it, the hall throttles to the floor.
  Pollution    2000 a second at full load, added with Pollution.addPollution; it needs a muffler.

Training (Training): output scales with T from 25 % to 100 %.  Work Logs from the input buses train it, 1/1024 each,
and it drifts back towards 0.6 by 2 % a day unless fed.  A plan the Data Center calculated most of is re-verified with
probability 20 % x (1 - T), charged as its steps again (ComputePolicy.planningEnded): the plan itself is always the
right one.  At T >= 0.95 the controller turns 16 Accelerator Cards and 256 Work Logs from its input buses into
Converged Model Weights over five minutes, the key to the Singularity.
*/

/// The AI Data Center. See Note [The Data Center].
public class MTEDataCenter extends MTEComputeFacility<MTEDataCenter> implements ISurvivalConstructable {

    private static final String FRONT = "front";
    private static final String SLICE = "slice";
    private static final String BACK = "back";
    private static IStructureDefinition<MTEDataCenter> STRUCTURE;

    private static final String END_ROW = "EEEEEEEEEEEEEEE";
    private static final String[][] FRONT_SHAPE = {
        { END_ROW, END_ROW, END_ROW, "EEEEEEE~EEEEEEE", END_ROW, END_ROW, END_ROW } };
    private static final String[][] BACK_SHAPE = { { END_ROW, END_ROW, END_ROW, END_ROW, END_ROW, END_ROW, END_ROW } };
    private static final String[][] SLICE_SHAPE = { { "WWWWWWWWWWWWWWW", "W-------------W", "WAA-AA-A-AA-AAW",
        "WAA-AA-A-AA-AAW", "WAA-AA-A-AA-AAW", "FFFFFFFFFFFFFFF", "WWWWWWWWWWWWWWW" } };

    private static final long EU_FULL = 4 * 32768;
    private static final int WATER_PER_SECOND = 1000;
    private static final int WEIGHTS_TICKS = 6000;

    private final List<TileAcceleratorRack> racks = new ArrayList<>();
    private int length;
    private final Training training = new Training();
    private int cards;
    private double load;
    private boolean cooled;
    private FacilityOutput output = FacilityOutput.OFF;
    private Bottleneck bottleneck = Bottleneck.NO_STRUCTURE;
    private int weightsProgress = -1;
    private long dayTicks;

    public MTEDataCenter(final int id, final String name, final String regionalName) {
        super(id, name, regionalName);
    }

    public MTEDataCenter(final String name) {
        super(name);
    }

    @Override
    public MetaTileEntity newMetaEntity(final IGregTechTileEntity tile) {
        return new MTEDataCenter(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Compute Facility, AI Data Center")
            .addInfo("We are pleased to announce a reduction in headcount.")
            .addInfo("8 AE2 job slots and 64 M of Filing; no staff")
            .addInfo("Synthetic FTE from the Accelerator Cards in its racks")
            .addInfo("Needs 1000 L/s of water (distilled counts double); pollutes 2000 a second at full load")
            .addInfo("Trained on Work Logs from the offices it replaces; the model drifts unless fed")
            .addInfo("Scheduled to be carbon neutral by a date.")
            .addSeparator()
            .beginVariableStructureBlock(15, 15, 7, 7, 7, ABConfig.dataCenterMaxLength, false)
            .addController("Front end wall, middle")
            .addCasingInfoMin("Data Hall Casing", 100, false)
            .addOtherStructurePart("Accelerator Rack", "The rack rows")
            .addOtherStructurePart("Raised Floor Casing", "The floor under the racks")
            .addEnergyHatch("Any Data Hall Casing", 1)
            .addMaintenanceHatch("Any Data Hall Casing", 1)
            .addInputHatch("Any Data Hall Casing: water", 1)
            .addOutputHatch("Any Data Hall Casing: hot water", 1)
            .addInputBus("Any Data Hall Casing: Work Logs", 1)
            .addMufflerHatch("Roof", 1)
            .toolTipFinisher("Applied Bureaucratics");
        return tt;
    }

    @Override
    public ITexture[] getTexture(final IGregTechTileEntity tile, final ForgeDirection side, final ForgeDirection facing,
        final int colorIndex, final boolean active, final boolean redstone) {
        final ITexture casing = Textures.BlockIcons.getCasingTextureForId(Casing.DATA_HALL.textureIndex());
        if (side == facing) {
            return new ITexture[] { casing,
                TextureFactory.of(active ? ABTextures.MAINFRAME_FRONT_ACTIVE : ABTextures.MAINFRAME_FRONT) };
        }
        return new ITexture[] { casing };
    }

    private static <T> IStructureElement<MTEDataCenter> hallCasing(final Casing casing) {
        return buildHatchAdder(MTEDataCenter.class)
            .atLeast(Energy, Maintenance, InputHatch, OutputHatch, InputBus, OutputBus, Muffler)
            .casingIndex(Casing.DATA_HALL.textureIndex())
            .hint(1)
            .buildAndChain(StructureUtility.ofBlock(ModBlocks.casing, casing.meta()));
    }

    @Override
    public IStructureDefinition<MTEDataCenter> getStructureDefinition() {
        if (STRUCTURE == null) {
            STRUCTURE = StructureDefinition.<MTEDataCenter>builder()
                .addShape(FRONT, FRONT_SHAPE)
                .addShape(SLICE, SLICE_SHAPE)
                .addShape(BACK, BACK_SHAPE)
                .addElement('E', hallCasing(Casing.DATA_HALL))
                .addElement('W', hallCasing(Casing.DATA_HALL))
                .addElement('F', StructureUtility.ofBlock(ModBlocks.casing, Casing.RAISED_FLOOR.meta()))
                .addElement(
                    'A',
                    StructureUtility.ofChain(
                        StructureUtility.ofTileAdder(MTEDataCenter::addRack, ModBlocks.acceleratorRack, 0),
                        StructureUtility.ofBlock(ModBlocks.acceleratorRack, 0)))
                .addElement('-', StructureUtility.isAir())
                .build();
        }
        return STRUCTURE;
    }

    private boolean addRack(final @Nullable TileEntity tile) {
        if (tile instanceof TileAcceleratorRack rack) {
            this.racks.add(rack);
            return true;
        }
        return false;
    }

    @Override
    public void checkMachine(final IGregTechTileEntity tile, final ItemStack stack, final List<StructureError> errors) {
        this.mEnergyHatches.clear();
        this.mInputBusses.clear();
        this.mOutputBusses.clear();
        this.mInputHatches.clear();
        this.mOutputHatches.clear();
        this.mMaintenanceHatches.clear();
        this.mMufflerHatches.clear();
        this.racks.clear();
        this.length = 0;
        if (!this.checkPiece(FRONT, 7, 3, 0, errors)) {
            return;
        }
        // Slices behind the front wall until the back wall, at least 5 of them.
        final int maxSlices = Math.max(5, ABConfig.dataCenterMaxLength - 2);
        int slice = 1;
        final List<StructureError> ignored = new ArrayList<>();
        while (slice <= maxSlices && !(slice > 5 && this.checkPiece(BACK, 7, 3, -slice, ignored))) {
            ignored.clear();
            if (!this.checkPiece(SLICE, 7, 3, -slice, errors)) {
                return;
            }
            slice++;
        }
        if (slice > maxSlices && !this.checkPiece(BACK, 7, 3, -slice, errors)) {
            return;
        }
        this.length = slice + 1;
        this.checkHasEnergyHatch(errors);
        this.checkOneMaintenanceHatch(errors);
        this.checkHasInputHatch(errors);
        this.checkHasInputBus(errors);
        if (this.mMufflerHatches.isEmpty()) {
            this.checkHasMufflerHatch(errors);
        }
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
        final int slices = Math.max(5, Math.min(ABConfig.dataCenterMaxLength - 2, stack.stackSize + 4));
        this.buildPiece(FRONT, stack, hintsOnly, 7, 3, 0);
        for (int i = 1; i <= slices; i++) {
            this.buildPiece(SLICE, stack, hintsOnly, 7, 3, -i);
        }
        this.buildPiece(BACK, stack, hintsOnly, 7, 3, -slices - 1);
    }

    @Override
    public int survivalConstruct(final ItemStack stack, final int budget, final ISurvivalBuildEnvironment env) {
        if (this.mMachine) {
            return -1;
        }
        final int slices = Math.max(5, Math.min(ABConfig.dataCenterMaxLength - 2, stack.stackSize + 4));
        int built = this.survivialBuildPiece(FRONT, stack, 7, 3, 0, budget, env, false, true);
        if (built >= 0) {
            return built;
        }
        for (int i = 1; i <= slices; i++) {
            built = this.survivialBuildPiece(SLICE, stack, 7, 3, -i, budget, env, false, true);
            if (built >= 0) {
                return built;
            }
        }
        return this.survivialBuildPiece(BACK, stack, 7, 3, -slices - 1, budget, env, false, true);
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        this.mMaxProgresstime = 20;
        this.mEfficiency = 10000 - (this.getIdealStatus() - this.getRepairStatus()) * 1000;
        this.mEfficiencyIncrease = 10000;
        this.mEUt = (int) -Math.max(EU_FULL / 10, EU_FULL * this.load);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    protected int structureReach() {
        return ABConfig.dataCenterMaxLength + 1;
    }

    @Override
    protected void facilityTick() {
        this.slots.setSlotCount(this.mMachine ? ABConfig.dataCenterSlots : 0);
        if (this.ticks % 20 == 0) {
            this.recompute();
        }
        if (this.weightsProgress >= 0 && this.isRunning() && ++this.weightsProgress >= WEIGHTS_TICKS) {
            this.weightsProgress = -1;
            this.addOutputPartial(ItemComponent.Kind.CONVERGED_MODEL_WEIGHTS.stack(1));
        }
    }

    private void recompute() {
        this.cards = 0;
        for (final TileAcceleratorRack rack : this.racks) {
            if (!rack.isInvalid()) {
                this.cards += rack.cards();
            }
        }
        final boolean busy = this.slots.anyBusy() || this.computeGrid() != null && this.computeGrid()
            .hasPlansInProgress();
        this.load = busy ? 1 : 0.1;
        if (!this.mMachine) {
            this.setOff(Bottleneck.NO_STRUCTURE);
            return;
        }
        if (!this.isRunning()) {
            this.setOff(Bottleneck.NO_POWER);
            return;
        }
        this.train();
        this.cooled = this.cool();
        if (busy) {
            Pollution.addPollution(this.getBaseMetaTileEntity(), (int) (ABConfig.dataCenterPollution * this.load));
        }
        if (this.cards == 0) {
            this.setOff(Bottleneck.NO_HARDWARE);
            return;
        }
        final double fte = this.cards * ABConfig.dataCenterFtePerCard;
        final Workforce.Split split = Workforce.split(
            java.util.Collections
                .singletonList(OrgUnit.worker(fte, com.gtnewhorizons.appliedbureaucratics.compute.Affinity.NONE)),
            this.planningShare());
        final double factor = this.cooled ? this.training.outputFactor() : ComputeSettings.get().floor;
        this.output = FacilityOutput.compute(split, factor, true, false, 1, 1);
        this.slots.setFiling(ABConfig.dataCenterFiling);
        this.slots.setDispatchRate(this.output.dispatchRate());
        this.bottleneck = !this.cooled ? Bottleneck.NO_COOLANT
            : this.training.get() < 0.95 ? Bottleneck.UNTRAINED : Bottleneck.NONE;
    }

    private void setOff(final Bottleneck why) {
        this.output = FacilityOutput.OFF;
        this.slots.setDispatchRate(0);
        this.bottleneck = why;
    }

    /// Feeds Work Logs, lets the model drift once a day, and starts Converged Model Weights when it can.
    private void train() {
        this.training.feed(this.takeFromInputs(ItemComponent.Kind.WORK_LOG::is, 16));
        this.dayTicks += 20;
        if (this.dayTicks >= 24000) {
            this.dayTicks -= 24000;
            this.training.day();
        }
        if (this.weightsProgress < 0 && this.training.get() >= 0.95
            && this.countInputs(ItemComponent.Kind.ACCELERATOR_CARD::is, s -> 1) >= 16
            && this.countInputs(ItemComponent.Kind.WORK_LOG::is, s -> 1) >= 256) {
            this.takeFromInputs(ItemComponent.Kind.ACCELERATOR_CARD::is, 16);
            this.takeFromInputs(ItemComponent.Kind.WORK_LOG::is, 256);
            this.weightsProgress = 0;
        }
    }

    /// Drains a second of coolant, at the load, and returns it hot. Distilled water counts double.
    private boolean cool() {
        final int needed = (int) Math.ceil(WATER_PER_SECOND * this.load);
        final FluidStack distilled = FluidRegistry.getFluidStack("ic2distilledwater", (needed + 1) / 2);
        if (distilled != null && this.depleteInput(distilled)) {
            this.returnHot(needed);
            return true;
        }
        final FluidStack water = FluidRegistry.getFluidStack("water", needed);
        if (water != null && this.depleteInput(water)) {
            this.returnHot(needed);
            return true;
        }
        return false;
    }

    private void returnHot(final int amount) {
        final FluidStack hot = FluidRegistry.getFluidStack("ic2hotwater", amount);
        if (hot != null) {
            this.addOutput(hot);
        }
    }

    public Training getTraining() {
        return this.training;
    }

    @Override
    public double getPlanningRate() {
        return this.output.planningRate();
    }

    @Override
    public boolean isStaffed() {
        return false;
    }

    @Override
    public boolean couldPlan() {
        return this.output != FacilityOutput.OFF;
    }

    @Override
    public double hallucinationChance() {
        return this.training.hallucinationChance();
    }

    @Override
    public @Nullable String getHostedCPUName(final HostedCraftingCPUCluster cpu) {
        final int i = new ArrayList<>(this.slots.getCPUs()).indexOf(cpu);
        return String.format(Locale.ROOT, "Hall A, rack %d", Math.max(0, i) + 1);
    }

    @Override
    public List<String> statusLines() {
        final List<String> lines = new ArrayList<>();
        lines.add(StatCollector.translateToLocal(this.bottleneck.key()));
        lines.add(
            String.format(
                Locale.ROOT,
                "  planning %.2f steps/s, dispatch %.2f ops/s, filing %d/%d B, slots %d, slots busy %d",
                this.output.planningRate(),
                this.output.dispatchRate(),
                this.slots.getUsedFiling(),
                this.slots.getFiling(),
                this.slots.getCPUs()
                    .size(),
                this.slots.getBusyCount()));
        lines.add(
            String.format(
                Locale.ROOT,
                "  datacenter: length %d, %d racks, %d cards, training %.3f, cooled %s, load %.1f, weights %s",
                this.length,
                this.racks.size(),
                this.cards,
                this.training.get(),
                this.cooled ? "yes" : "no",
                this.load,
                this.weightsProgress < 0 ? "idle" : this.weightsProgress * 100 / WEIGHTS_TICKS + " %"));
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
        tag.setDouble("training", this.training.get());
        tag.setInteger("weightsProgress", this.weightsProgress);
        tag.setLong("dayTicks", this.dayTicks);
    }

    @Override
    public void loadNBTData(final NBTTagCompound tag) {
        super.loadNBTData(tag);
        this.training.set(tag.getDouble("training"));
        this.weightsProgress = tag.hasKey("weightsProgress") ? tag.getInteger("weightsProgress") : -1;
        this.dayTicks = tag.getLong("dayTicks");
    }
}
