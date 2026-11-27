package com.gtnewhorizons.appliedbureaucratics.facility.desk;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.compute.Affinity;
import com.gtnewhorizons.appliedbureaucratics.compute.ComputeSettings;
import com.gtnewhorizons.appliedbureaucratics.compute.FacilityOutput;
import com.gtnewhorizons.appliedbureaucratics.compute.Morale;
import com.gtnewhorizons.appliedbureaucratics.compute.MoraleTarget;
import com.gtnewhorizons.appliedbureaucratics.compute.OrgUnit;
import com.gtnewhorizons.appliedbureaucratics.compute.Payroll;
import com.gtnewhorizons.appliedbureaucratics.compute.Workforce;
import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;
import com.gtnewhorizons.appliedbureaucratics.config.ItemList;
import com.gtnewhorizons.appliedbureaucratics.facility.Amenities;
import com.gtnewhorizons.appliedbureaucratics.facility.Bottleneck;
import com.gtnewhorizons.appliedbureaucratics.facility.FacilitySlots;
import com.gtnewhorizons.appliedbureaucratics.facility.IComputeFacility;
import com.gtnewhorizons.appliedbureaucratics.facility.breakroom.TileBreakRoomCounter;
import com.gtnewhorizons.appliedbureaucratics.integration.ae2.IComputeGrid;
import com.gtnewhorizons.appliedbureaucratics.workforce.Employment;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.WorldCoord;
import appeng.me.cluster.implementations.HostedCraftingCPUCluster;
import appeng.me.cluster.implementations.ICraftingCPUHost;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;

/// The Requisition Tray: the ME node and the one job slot of a Desk, the HV facility. The rest of the Desk is
/// furniture found around it ([DeskSurvey]): a seat with a worker on it, a desk next to the seat, a typewriter for
/// planning, a bell for dispatch, and bookcases for Filing. A player sitting on the seat is the worker ("founder
/// mode"). See Note [The compute model] and Note [Facility job slots].
public class TileRequisitionTray extends TileEntity
    implements IGridProxyable, ICraftingCPUHost, IComputeFacility, ISidedInventory {

    public static final int SLOT_PAPER = 0;
    public static final int SLOT_PAYROLL = 1;
    private static final int[] SLOTS = { SLOT_PAPER, SLOT_PAYROLL };

    private final AENetworkProxy proxy = new AENetworkProxy(this, "proxy", null, true);
    private final FacilitySlots slots = new FacilitySlots(this);
    private final Morale morale = new Morale();
    private final Payroll payroll = new Payroll();
    private final ItemStack[] inventory = new ItemStack[2];
    private boolean ready;
    private int ticks;
    private DeskSurvey survey = DeskSurvey.NOTHING;
    private FacilityOutput output = FacilityOutput.OFF;
    private Bottleneck bottleneck = Bottleneck.NO_POWER;
    private boolean paper;
    private boolean crunch;
    private Double pinnedShare;
    private Boolean crunchOverride;
    private boolean coffee;
    private boolean party;
    /// Sheets of paper owed for planning done and not yet taken from the paper slot.
    private double sheetsOwed;

    public TileRequisitionTray() {
        this.proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
        this.proxy.setIdlePowerUsage(2);
        this.slots.setSlotCount(1);
    }

    // The facility.

    @Override
    public void updateEntity() {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }
        if (!this.ready) {
            this.ready = true;
            this.proxy.onReady();
        }
        final IGridNode node = this.proxy.getNode();
        if (this.slots.hasSavedJobs() && node != null && node.getGrid() != null) {
            this.slots.restore();
        }
        if (this.ticks % 100 == 0) {
            this.survey = DeskSurvey.of(this.worldObj, this.xCoord, this.yCoord, this.zCoord);
        }
        if (this.ticks % 20 == 0) {
            this.recompute();
        }
        if (this.ticks % 1200 == 0 && this.ticks > 0) {
            this.morale.drift(this.moraleTarget(), 1);
        }
        this.ticks++;
        this.slots.tick();
    }

    private boolean isPowered() {
        final IGridNode node = this.proxy.getNode();
        return node != null && node.isActive();
    }

    private void recompute() {
        this.slots.setFiling(this.survey.filing());
        this.paper = this.takePaper();
        this.crunch = this.crunchOverride != null ? this.crunchOverride
            : this.worldObj.isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord);
        final TileBreakRoomCounter counter = TileBreakRoomCounter
            .near(this.worldObj, this.xCoord, this.yCoord, this.zCoord);
        final int staff = this.survey.hasWorker() ? 1 : 0;
        this.coffee = counter != null && counter.serveCoffee(staff, 20);
        this.party = counter != null && staff > 0 && counter.party();
        if (!this.isPowered()) {
            this.setOff(Bottleneck.NO_POWER);
            return;
        }
        if (!this.survey.seat()) {
            this.setOff(Bottleneck.NO_SEAT);
            return;
        }
        if (!this.survey.hasWorker()) {
            this.setOff(Bottleneck.NO_WORKER);
            return;
        }
        if (!this.survey.desk()) {
            this.setOff(Bottleneck.NO_DESK);
            return;
        }
        final EntityLivingBase worker = this.survey.worker();
        if (worker != null) {
            this.payroll.accrue(1, 20);
            this.payWages();
        }
        final List<OrgUnit> roots = Collections.singletonList(OrgUnit.worker(1, affinity(worker)));
        final double share = this.survey.typewriter() ? this.planningShare() : 0;
        final Workforce.Split split = Workforce.split(roots, share);
        this.output = FacilityOutput.compute(
            split,
            this.morale.multiplier(),
            this.paper,
            this.crunch,
            this.survey.bell() ? ABConfig.deskBellBonus : 1,
            1);
        this.slots.setDispatchRate(this.output.dispatchRate());
        this.bottleneck = this.findBottleneck();
    }

    private void setOff(final Bottleneck why) {
        this.output = FacilityOutput.OFF;
        this.slots.setDispatchRate(0);
        this.bottleneck = why;
    }

    private Bottleneck findBottleneck() {
        if (!this.survey.typewriter()) {
            return Bottleneck.NO_TYPEWRITER;
        }
        if (!this.paper) {
            return Bottleneck.NO_PAPER;
        }
        if (this.survey.worker() != null && !this.payroll.isPaid()) {
            return Bottleneck.UNPAID;
        }
        if (this.morale.get() < 40) {
            return Bottleneck.LOW_MORALE;
        }
        if (this.survey.filing() <= 0) {
            return Bottleneck.NO_FILING;
        }
        return Bottleneck.NONE;
    }

    /// The part of the worker's time spent planning: all of it while the slot is idle, none while it is busy and
    /// nothing is being planned on the network, a quarter while both.
    private double planningShare() {
        if (this.pinnedShare != null) {
            return this.pinnedShare;
        }
        if (!this.slots.anyBusy()) {
            return 1;
        }
        final IComputeGrid compute = this.computeGrid();
        return compute != null && compute.hasPlansInProgress() ? 0.25 : 0;
    }

    private static Affinity affinity(final @Nullable EntityLivingBase worker) {
        if (worker instanceof EntityVillager villager) {
            return switch (villager.getProfession()) {
                case 0 -> Affinity.DISPATCH;
                case 1 -> Affinity.PLANNING;
                case 2 -> Affinity.EITHER;
                default -> Affinity.NONE;
            };
        }
        return Affinity.NONE;
    }

    private double moraleTarget() {
        final int[] seat = this.survey.seatAt();
        final boolean crowded = seat != null && Amenities.crowded(this.worldObj, seat[0], seat[1], seat[2]);
        return new MoraleTarget(
            this.isPaid(),
            this.survey.amenities(),
            this.coffee,
            this.party,
            crowded,
            Amenities.smog(this.worldObj, this.xCoord, this.zCoord),
            0,
            this.crunch).value();
    }

    private void payWages() {
        final long due = this.payroll.emeraldsDue();
        if (due <= 0) {
            return;
        }
        final ItemStack wages = this.inventory[SLOT_PAYROLL];
        if (wages == null || wages.getItem() != Items.emerald) {
            return;
        }
        final int paid = (int) Math.min(due, wages.stackSize);
        this.decrStackSize(SLOT_PAYROLL, paid);
        this.payroll.pay(paid);
    }

    /// Takes the sheets owed from the paper slot, and says whether there is paper.
    private boolean takePaper() {
        final ItemStack paper = this.inventory[SLOT_PAPER];
        final int owed = (int) Math.floor(this.sheetsOwed);
        if (owed > 0 && paper != null && ItemList.PAPER.matches(paper)) {
            final int taken = Math.min(owed, paper.stackSize);
            this.decrStackSize(SLOT_PAPER, taken);
            this.sheetsOwed -= taken;
        }
        final ItemStack left = this.inventory[SLOT_PAPER];
        return left != null && ItemList.PAPER.matches(left) && left.stackSize > 0;
    }

    private @Nullable IComputeGrid computeGrid() {
        final IGridNode node = this.proxy.getNode();
        final IGrid grid = node != null ? node.getGrid() : null;
        return grid != null ? grid.getCache(IComputeGrid.class) : null;
    }

    @Override
    public double getPlanningRate() {
        return this.output.planningRate();
    }

    @Override
    public boolean isStaffed() {
        return true;
    }

    @Override
    public boolean couldPlan() {
        return this.output != FacilityOutput.OFF && this.survey.typewriter();
    }

    @Override
    public void setPinnedPlanningShare(final Double share) {
        this.pinnedShare = share == null ? null : Math.max(0, Math.min(1, share));
    }

    @Override
    public void setCrunchOverride(final Boolean crunch) {
        this.crunchOverride = crunch;
    }

    @Override
    public void planningCharged(final double steps) {
        this.sheetsOwed += steps / ComputeSettings.get().stepsPerSheet;
        this.markDirty();
    }

    // What the GUI and tooltips show.

    public FacilityOutput getOutput() {
        return this.output;
    }

    public Bottleneck getBottleneck() {
        return this.bottleneck;
    }

    public FacilitySlots getSlots() {
        return this.slots;
    }

    public Morale getMorale() {
        return this.morale;
    }

    public DeskSurvey getSurvey() {
        return this.survey;
    }

    @Override
    public java.util.List<String> statusLines() {
        return java.util.Arrays.asList(
            net.minecraft.util.StatCollector.translateToLocal(this.bottleneck.key()),
            String.format(
                java.util.Locale.ROOT,
                "  planning %.2f steps/s, dispatch %.2f ops/s, filing %d/%d B, morale %.0f, %s, worker %s, slots busy %d%s%s%s",
                this.output.planningRate(),
                this.output.dispatchRate(),
                this.slots.getUsedFiling(),
                this.slots.getFiling(),
                this.morale.get(),
                this.isPaid() ? "paid" : "unpaid",
                this.survey.worker() != null ? "employee" : this.survey.founder() != null ? "founder" : "none",
                this.slots.getBusyCount(),
                this.crunch ? ", crunch" : "",
                this.coffee ? ", coffee" : "",
                this.party ? ", party" : ""));
    }

    public boolean isCrunching() {
        return this.crunch;
    }

    public boolean hasCoffee() {
        return this.coffee;
    }

    public boolean isPartying() {
        return this.party;
    }

    public boolean isPaid() {
        return this.survey.worker() == null || this.payroll.isPaid();
    }

    /// Ends the facility's jobs and returns their items, when the block is broken.
    public void onBroken() {
        this.slots.dispose();
    }

    // ICraftingCPUHost.

    @Override
    public Collection<? extends HostedCraftingCPUCluster> getHostedCPUs() {
        return this.slots.getCPUs();
    }

    @Override
    public World getHostWorld() {
        return this.worldObj;
    }

    @Override
    public WorldCoord getHostLocation() {
        return new WorldCoord(this.xCoord, this.yCoord, this.zCoord);
    }

    @Override
    public int grantOperations(final HostedCraftingCPUCluster cpu) {
        return this.slots.grant(cpu);
    }

    @Override
    public void markHostDirty() {
        this.markDirty();
    }

    @Override
    public @Nullable String getHostedCPUName(final HostedCraftingCPUCluster cpu) {
        final EntityLivingBase worker = this.survey.worker();
        final Employment employment = worker != null ? Employment.of(worker) : null;
        if (employment != null) {
            return employment.getTitle() + " " + employment.getName();
        }
        return "Requisition Tray";
    }

    // The ME node.

    @Override
    public AENetworkProxy getProxy() {
        return this.proxy;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public void gridChanged() {}

    @Override
    public IGridNode getGridNode(final ForgeDirection dir) {
        return this.proxy.getNode();
    }

    @Override
    public AECableType getCableConnectionType(final ForgeDirection dir) {
        return AECableType.SMART;
    }

    @Override
    public void securityBreak() {
        this.worldObj.func_147480_a(this.xCoord, this.yCoord, this.zCoord, true);
    }

    @Override
    public IGridNode getActionableNode() {
        return this.proxy.getNode();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        this.ready = false;
        this.proxy.onChunkUnload();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.ready = false;
        this.proxy.invalidate();
    }

    @Override
    public void validate() {
        super.validate();
        this.ready = false;
    }

    // Saving.

    @Override
    public void writeToNBT(final NBTTagCompound tag) {
        super.writeToNBT(tag);
        this.proxy.writeToNBT(tag);
        this.slots.writeToNBT(tag);
        tag.setDouble("morale", this.morale.get());
        tag.setLong("wageTicks", this.payroll.getWorkerTicks());
        tag.setLong("wagesOwed", this.payroll.getOwed());
        tag.setLong("wagesPrepaid", this.payroll.getPrepaid());
        tag.setDouble("sheetsOwed", this.sheetsOwed);
        final NBTTagList items = new NBTTagList();
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] != null) {
                final NBTTagCompound item = new NBTTagCompound();
                item.setByte("slot", (byte) i);
                this.inventory[i].writeToNBT(item);
                items.appendTag(item);
            }
        }
        tag.setTag("items", items);
    }

    @Override
    public void readFromNBT(final NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.proxy.readFromNBT(tag);
        this.slots.readFromNBT(tag);
        this.morale.set(tag.hasKey("morale") ? tag.getDouble("morale") : Morale.START);
        this.payroll.load(tag.getLong("wageTicks"), tag.getLong("wagesOwed"), tag.getLong("wagesPrepaid"));
        this.sheetsOwed = tag.getDouble("sheetsOwed");
        final NBTTagList items = tag.getTagList("items", 10);
        for (int i = 0; i < items.tagCount(); i++) {
            final NBTTagCompound item = items.getCompoundTagAt(i);
            final int slot = item.getByte("slot");
            if (slot >= 0 && slot < this.inventory.length) {
                this.inventory[slot] = ItemStack.loadItemStackFromNBT(item);
            }
        }
    }

    // The paper and payroll slots, which buses and AE2 can fill.

    @Override
    public int getSizeInventory() {
        return this.inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        return this.inventory[slot];
    }

    @Override
    public ItemStack decrStackSize(final int slot, final int count) {
        final ItemStack stack = this.inventory[slot];
        if (stack == null) {
            return null;
        }
        final ItemStack taken = stack.splitStack(Math.min(count, stack.stackSize));
        if (stack.stackSize <= 0) {
            this.inventory[slot] = null;
        }
        this.markDirty();
        return taken;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(final int slot) {
        return null;
    }

    @Override
    public void setInventorySlotContents(final int slot, final ItemStack stack) {
        this.inventory[slot] = stack;
        this.markDirty();
    }

    @Override
    public String getInventoryName() {
        return "appliedbureaucratics.requisition_tray";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(final EntityPlayer player) {
        return this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) == this
            && player.getDistanceSq(this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5) <= 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
        return slot == SLOT_PAPER ? ItemList.PAPER.matches(stack) : stack.getItem() == Items.emerald;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(final int side) {
        return SLOTS;
    }

    @Override
    public boolean canInsertItem(final int slot, final ItemStack stack, final int side) {
        return this.isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canExtractItem(final int slot, final ItemStack stack, final int side) {
        return false;
    }
}
