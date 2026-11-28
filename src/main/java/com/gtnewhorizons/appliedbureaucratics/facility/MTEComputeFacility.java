package com.gtnewhorizons.appliedbureaucratics.facility;

import java.util.Collection;
import java.util.EnumSet;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.integration.ae2.IComputeGrid;
import com.gtnewhorizons.appliedbureaucratics.integration.bibliocraft.Furniture;
import com.gtnewhorizons.appliedbureaucratics.util.Cadence;

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
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;

/// What every multiblock facility shares: its ME node, its job slots ([FacilitySlots], Note [Facility job slots]),
/// saving them, and taking things from its input buses. See wrinkle (W1) of Note [The Mainframe]: the node is made
/// ready whether or not the machine runs.
public abstract class MTEComputeFacility<T extends MTEEnhancedMultiBlockBase<T>> extends MTEEnhancedMultiBlockBase<T>
    implements IGridProxyable, ICraftingCPUHost, IComputeFacility {

    private @Nullable AENetworkProxy proxy;
    protected final FacilitySlots slots = new FacilitySlots(this);
    /// Starts anywhere in a minute, so facilities loaded together do their periodic work on different ticks.
    protected int ticks = java.util.concurrent.ThreadLocalRandom.current()
        .nextInt(1200);
    private final Cadence structureCheck = new Cadence(Cadence.LARGE);
    private @Nullable Double pinnedShare;
    private @Nullable Boolean crunchOverride;

    protected MTEComputeFacility(final int id, final String name, final String regionalName) {
        super(id, name, regionalName);
    }

    protected MTEComputeFacility(final String name) {
        super(name);
    }

    public boolean isFormed() {
        return this.mMachine;
    }

    protected boolean isRunning() {
        return this.mMachine && this.getBaseMetaTileEntity()
            .isActive();
    }

    public FacilitySlots getSlots() {
        return this.slots;
    }

    protected World world() {
        return this.getBaseMetaTileEntity()
            .getWorld();
    }

    protected int x() {
        return this.getBaseMetaTileEntity()
            .getXCoord();
    }

    protected int y() {
        return this.getBaseMetaTileEntity()
            .getYCoord();
    }

    protected int z() {
        return this.getBaseMetaTileEntity()
            .getZCoord();
    }

    /// How far from the controller the structure may reach, in blocks.
    protected int structureReach() {
        return 8;
    }

    /// Runs on the server every tick, after the ME node and the job slots are looked after.
    protected abstract void facilityTick();

    @Override
    public void onPostTick(final IGregTechTileEntity tile, final long tick) {
        super.onPostTick(tile, tick);
        if (!tile.isServerSide()) {
            return;
        }
        final AENetworkProxy proxy = this.getProxy();
        if (!proxy.isReady()) {
            proxy.onReady();
        }
        final IGridNode node = proxy.getNode();
        if (this.slots.hasSavedJobs() && node != null && node.getGrid() != null) {
            this.slots.restore();
        }
        // GregTech re-checks a structure when a machine-block update reaches the controller, which does not reliably
        // happen for blocks far from it in our large structures (a Singularity pylon); check every 10 s as well. Only
        // while every chunk the structure may reach is loaded: a check over an unloaded chunk fails, and GregTech then
        // switches the machine off until a player turns it on again.
        final int r = this.structureReach();
        if (this.structureCheck.tick() && this.world()
            .checkChunksExist(this.x() - r, this.y() - r, this.z() - r, this.x() + r, this.y() + r, this.z() + r)) {
            this.onMachineBlockUpdate();
        }
        this.facilityTick();
        this.ticks++;
        this.slots.tick();
    }

    /// The part of a facility's FTE spent planning: all of it while no slot is busy, none while one is and nothing is
    /// being planned on the network, a quarter while both.
    protected double planningShare() {
        if (this.pinnedShare != null) {
            return this.pinnedShare;
        }
        if (!this.slots.anyBusy()) {
            return 1;
        }
        final IComputeGrid compute = this.computeGrid();
        return compute != null && compute.hasPlansInProgress() ? 0.25 : 0;
    }

    @Override
    public void setPinnedPlanningShare(final @Nullable Double share) {
        this.pinnedShare = share == null ? null : Math.max(0, Math.min(1, share));
    }

    @Override
    public void setCrunchOverride(final @Nullable Boolean crunch) {
        this.crunchOverride = crunch;
    }

    /// Whether the facility crunches: by OpenComputers' override if set, else by a redstone signal.
    protected boolean isCrunching() {
        return this.crunchOverride != null ? this.crunchOverride
            : this.getBaseMetaTileEntity()
                .getRedstone();
    }

    protected @Nullable IComputeGrid computeGrid() {
        final IGridNode node = this.getProxy()
            .getNode();
        final IGrid grid = node != null ? node.getGrid() : null;
        return grid != null ? grid.getCache(IComputeGrid.class) : null;
    }

    /// Takes up to `count` of the items matching `what` from the input buses, and says how many it took.
    public int takeFromInputs(final Predicate<ItemStack> what, final int count) {
        int taken = 0;
        for (final ItemStack stack : this.getStoredInputs()) {
            if (taken >= count) {
                break;
            }
            if (stack != null && what.test(stack)) {
                final int n = Math.min(count - taken, stack.stackSize);
                final ItemStack toTake = stack.copy();
                toTake.stackSize = n;
                if (this.depleteInput(toTake)) {
                    taken += n;
                }
            }
        }
        return taken;
    }

    /// Counts the items matching `what` in the input buses, weighted by `weight`.
    protected long countInputs(final Predicate<ItemStack> what,
        final java.util.function.ToLongFunction<ItemStack> weight) {
        long sum = 0;
        for (final ItemStack stack : this.getStoredInputs()) {
            if (stack != null && what.test(stack)) {
                sum += weight.applyAsLong(stack) * stack.stackSize;
            }
        }
        return sum;
    }

    /// Whether the seat of `seated` is a manager's office: a BiblioCraft desk and bell within a block.
    public static boolean isManagerOffice(final World world, final Workstations.Seated seated) {
        boolean desk = false;
        boolean bell = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    final Block block = world.getBlock(seated.x() + dx, seated.y() + dy, seated.z() + dz);
                    desk |= Furniture.DESK.matches(block);
                    bell |= Furniture.BELL.matches(block);
                }
            }
        }
        return desk && bell;
    }

    // ICraftingCPUHost.

    @Override
    public Collection<? extends HostedCraftingCPUCluster> getHostedCPUs() {
        return this.slots.getCPUs();
    }

    @Override
    public World getHostWorld() {
        return this.world();
    }

    @Override
    public WorldCoord getHostLocation() {
        return new WorldCoord(this.x(), this.y(), this.z());
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
    public void onRemoval() {
        this.slots.dispose();
        super.onRemoval();
    }

    // The ME node.

    @Override
    public AENetworkProxy getProxy() {
        if (this.proxy == null) {
            this.proxy = new AENetworkProxy(this, "proxy", this.getStackForm(1), true);
            this.proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            this.proxy.setIdlePowerUsage(4);
            this.proxy.setValidSides(EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN)));
        }
        return this.proxy;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this.world(), this.x(), this.y(), this.z());
    }

    @Override
    public void gridChanged() {}

    @Override
    public IGridNode getGridNode(final ForgeDirection dir) {
        return this.getProxy()
            .getNode();
    }

    @Override
    public AECableType getCableConnectionType(final ForgeDirection dir) {
        return AECableType.SMART;
    }

    @Override
    public void securityBreak() {
        this.getBaseMetaTileEntity()
            .disableWorking();
    }

    @Override
    public IGridNode getActionableNode() {
        return this.getProxy()
            .getNode();
    }

    @Override
    public boolean supportsVoidProtection() {
        return false;
    }

    // Saving.

    @Override
    public void saveNBTData(final NBTTagCompound tag) {
        super.saveNBTData(tag);
        final NBTTagCompound proxyTag = new NBTTagCompound();
        this.getProxy()
            .writeToNBT(proxyTag);
        tag.setTag("proxy", proxyTag);
        this.slots.writeToNBT(tag);
    }

    @Override
    public void loadNBTData(final NBTTagCompound tag) {
        super.loadNBTData(tag);
        if (tag.hasKey("proxy")) {
            this.getProxy()
                .readFromNBT(tag.getCompoundTag("proxy"));
        }
        this.slots.readFromNBT(tag);
    }
}
