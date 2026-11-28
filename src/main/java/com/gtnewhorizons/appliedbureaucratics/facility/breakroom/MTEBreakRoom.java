package com.gtnewhorizons.appliedbureaucratics.facility.breakroom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.fluid.FluidStackTank;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.gtnewhorizons.appliedbureaucratics.compute.CoffeeLog;
import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;
import com.gtnewhorizons.appliedbureaucratics.config.ItemList;
import com.gtnewhorizons.appliedbureaucratics.integration.bibliocraft.Furniture;
import com.gtnewhorizons.appliedbureaucratics.util.Cadence;
import com.gtnewhorizons.appliedbureaucratics.workforce.Employment;
import com.gtnewhorizons.appliedbureaucratics.workforce.EntityWorkstationMount;
import com.gtnewhorizons.appliedbureaucratics.workforce.Shift;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicTank;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.render.TextureFactory;

/*-
Note [Break rooms] (generated)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Topic.  Workers like a break room, and they like a microkitchen more.  A break room raises the morale of the
workers it serves, the more so the better its kitchen and what it serves; it also sends them on breaks, which cost
their facility their work while they are away.  The design means to reward an actual office building: many workers,
on a few floors, sharing a few well-stocked, well-furnished break rooms.

Design.  A break room is a GregTech single-block machine, MTEBreakRoom, of three kinds (BreakRoomKind):

  Water Cooler            no power        its own chunk   under an Iron Tanks tank of water; 250 mL a visit; +5
  Coffee Machine          HV, 4096 EU     3 by 3 chunks   coffee beans; sugar, cocoa powder and milk if it has them
  Snack Vending Machine   EV, 16384 EU    5 by 5 chunks   sandwiches; +4, and +2 per kind in stock, up to 6 kinds

The power is per visit, not per tick: an idle break room uses none.

It is open while it is enabled, powered if it needs power, and stocked (Status says why not).  Party food in it
throws a pizza party for a day, +20, as soon as it serves anyone.

What it serves is used up a visit at a time: when a worker on a break arrives (MTEBreakRoom.visit, from
Breaks.drive), a Water Cooler pours 250 mL from the tank above it, a Snack Vending Machine sells one sandwich of a
kind it has at random, and a Coffee Machine makes a cup: one coffee bean, and one each of sugar and cocoa powder if
it has them, and milk if it has any, 100 mL from its tank or a soy milk item, one or the other at random if it has
both.  The cup's extras, 0 to 3, go with the worker (Shift.cups) to its facility, whose CoffeeLog keeps a moving
average of them: the coffee's bonus, 4 and 2 per extra, is the facility's, not the worker's or the machine's.

The machine finds its workers, not the other way round: every ABConfig.breakRoomScanTicks (2 min, on its own
Cadence: Note [Spatial checks are staggered]) it asks the world for the workstation mounts (EntityWorkstationMount)
in its chunks, within ABConfig.breakRoomVerticalReach (4) blocks of its own height, and binds each worker on shift
there whose seat is within ABConfig.breakRoomMaxDistance (48) blocks across.  A worker's Shift holds its Binding:
where the break room is, its kind, its kitchen's points and quality, what it serves is worth, whether it is open and
partying, and until when the binding holds (two scans and a bit).  A worker in reach of two break rooms keeps the
better one.  The vertical reach means a break room serves its own floor: a tower needs one a floor.

At the same scan the machine looks at its kitchen (Kitchen.around): 4 points per kind of kitchen fixture (Cooking
for Blockheads' sinks, fridges, ovens and the rest), up to 20, and one per plant, up to 5.  Quality is the points
out of 25.

A facility (TileRequisitionTray, MTEMainframe) adds the average bonus of its seated workers' break rooms to its
morale target (BreakRoomBindings.bonus; MoraleTarget.breakRoom, capped at 40): the kitchen's points, and the Water
Cooler's 5, the facility's coffee, or the Snack Vending Machine's sandwiches.  A party at any of them is +20.

Breaks.  A worker bound to an open break room takes breaks (Breaks.whileSeated, from its mount's tick).  How often
and how long is BreakSchedule: unmanaged, 30 s every 5 min, both stretched by up to a quarter at the best kitchen;
managed (its Mainframe has a paid manager, recorded in Shift.managedUntil at each office survey), 20 s every
7 min, whatever the kitchen.  On a break the worker leaves its seat, walks to the break room, is served, wanders
within ABConfig.breakRoomWander (6) blocks of it, and walks back and sits down (Breaks.drive).  While it is away
its facility does without it, and says so: "on a break".

Wrinkles.

  (W1) A worker on a break is off its mount, so its own AI would wake up.  MixinEntityLivingBase_Shift runs
       Breaks.drive in place of updateAITasks while Employment.isOnBreak: our walk, the navigator and the move,
       look and jump helpers, and none of its AI tasks.  Employment.isAtWork (on shift or on a break) keeps it
       invulnerable, and keeps the Singularity from studying it.
  (W2) The break in progress is saved with the worker (Shift.Break, in Employment's tag), so a worker saved
       mid-break resumes it on load.  Its binding, schedule, management and undelivered cups are not saved: the
       next scan and survey restore the first three, and its first break after a load comes at a random point of
       its interval.
  (W3) A walk that has not arrived in a minute is given up on, and the worker put where it was going: a break
       room the workers cannot reach costs their walk, not the worker.
  (W4) The seat's mount removes itself a second after its rider leaves.  Back from its break, the worker gets a
       new one (WorkforceActions.sit), unless someone else took the seat or it is gone: then it stands about,
       off shift, until it is assigned again.
  (W5) A facility cannot see an absent worker.  Breaks keeps, per world, the seats whose workers are away
       (Breaks.isAway, Breaks.awayNear), refreshed every tick of the walk and forgotten 5 s after it ends.  While
       its workers are away, a facility keeps the break room bonus it last saw.
  (W6) Iron Tanks' tanks stack, BuildCraft-style: the Water Cooler drains the tank block on top of it, through
       its IFluidHandler, which draws on the whole stack.
  (W7) A BiblioCraft seat sits down any mob that walks onto it, and the way back to a worker's seat often goes
       over seats.  A worker riding anything but its EntityWorkstationMount is still on its break
       (Employment.isOnBreak), and Breaks.drive gets it off again at once.
*/
/// A break room machine. See Note [Break rooms].
public class MTEBreakRoom extends MTEBasicTank {

    private static final int FIRST_SUPPLY = 3;
    private static final int SUPPLY_SLOTS = 9;

    /// Why a break room is closed, or that it is open.
    public enum Status {

        OPEN,
        DISABLED,
        NO_POWER,
        NO_TANK,
        NO_WATER,
        NO_BEANS,
        NO_SANDWICHES;

        public String key() {
            return "ab.breakroom.status." + this.name()
                .toLowerCase();
        }
    }

    private final BreakRoomKind kind;
    private final Cadence scan = new Cadence(ABConfig.breakRoomScanTicks);
    private final Cadence check = new Cadence(20);
    private Status status = Status.DISABLED;
    private boolean powered;
    private Kitchen kitchen = Kitchen.BARE;
    /// Workers bound at the last scan.
    private int workers;
    private long partyUntil;
    /// Visits served, for the GUI.
    private int served;

    public MTEBreakRoom(final BreakRoomKind kind) {
        super(
            kind.id,
            "basicmachine.ab." + kind.name,
            kind.regionalName,
            kind.tier,
            FIRST_SUPPLY + SUPPLY_SLOTS,
            (String) null,
            TextureFactory.of(kind.front(false)),
            TextureFactory.of(kind.front(true)));
        this.kind = kind;
    }

    private MTEBreakRoom(final MTEBreakRoom prototype) {
        super(
            prototype.mName,
            prototype.mTier,
            FIRST_SUPPLY + SUPPLY_SLOTS,
            prototype.mDescriptionArray,
            prototype.mTextures);
        this.kind = prototype.kind;
    }

    @Override
    public IMetaTileEntity newMetaEntity(final IGregTechTileEntity tile) {
        return new MTEBreakRoom(this);
    }

    public BreakRoomKind getKind() {
        return this.kind;
    }

    public Status getStatus() {
        return this.status;
    }

    public Kitchen getKitchen() {
        return this.kitchen;
    }

    public int getWorkers() {
        return this.workers;
    }

    public int getServed() {
        return this.served;
    }

    /// Makes the next scan come on the next tick, for `/ab debug breakroom`.
    public void hurryScan() {
        this.scan.hurry();
        this.check.hurry();
    }

    public boolean isPartying() {
        final World world = this.getBaseMetaTileEntity()
            .getWorld();
        return world != null && world.getTotalWorldTime() < this.partyUntil;
    }

    /// What it serves is worth: a Water Cooler's water, a Snack Vending Machine's sandwiches by their variety; 0 for
    /// a Coffee Machine, whose coffee's worth is its facility's (CoffeeLog).
    public int stockBonus() {
        return switch (this.kind) {
            case WATER_COOLER -> BreakRoomKind.WATER_BONUS;
            case COFFEE_MACHINE -> 0;
            case SNACK_MACHINE -> BreakRoomKind.SNACK_PLAIN
                + BreakRoomKind.PER_SANDWICH * Math.min(BreakRoomKind.SANDWICH_KINDS, this.sandwichKinds());
        };
    }

    @Override
    public String[] getDescription() {
        final List<String> lines = new ArrayList<>();
        lines.add(StatCollector.translateToLocal("ab.breakroom." + this.kind.name + ".desc"));
        lines.add(
            StatCollector.translateToLocalFormatted(
                "ab.breakroom.serves",
                1 + 2 * this.kind.chunkRadius,
                1 + 2 * this.kind.chunkRadius,
                ABConfig.breakRoomVerticalReach));
        lines.add(StatCollector.translateToLocal("ab.breakroom." + this.kind.name + ".supplies"));
        lines.add(StatCollector.translateToLocalFormatted("ab.breakroom.kitchen", Kitchen.MAX));
        lines.add(
            this.kind.isPowered() ? StatCollector.translateToLocalFormatted("ab.breakroom.power", this.kind.euPerVisit)
                : StatCollector.translateToLocal("ab.breakroom.nopower"));
        lines.add(StatCollector.translateToLocal("ab.breakroom.party"));
        return lines.toArray(new String[0]);
    }

    // Running.

    @Override
    public void onPostTick(final IGregTechTileEntity tile, final long tick) {
        super.onPostTick(tile, tick);
        if (!tile.isServerSide()) {
            return;
        }
        if (this.check.tick()) {
            this.powered = !this.kind.isPowered() || tile.isUniversalEnergyStored(this.kind.euPerVisit);
            this.status = this.computeStatus(tile);
            tile.setActive(this.status == Status.OPEN);
        }
        if (this.scan.tick()) {
            this.survey(tile);
        }
    }

    private Status computeStatus(final IGregTechTileEntity tile) {
        if (!tile.isAllowedToWork()) {
            return Status.DISABLED;
        }
        if (!this.powered) {
            return Status.NO_POWER;
        }
        return switch (this.kind) {
            case WATER_COOLER -> {
                final IFluidHandler tank = this.tankAbove(tile);
                yield tank == null ? Status.NO_TANK : this.drainWater(tank, false) ? Status.OPEN : Status.NO_WATER;
            }
            case COFFEE_MACHINE -> this.find(ItemList.COFFEE_BEANS) >= 0 ? Status.OPEN : Status.NO_BEANS;
            case SNACK_MACHINE -> this.find(ItemList.SANDWICHES) >= 0 ? Status.OPEN : Status.NO_SANDWICHES;
        };
    }

    /// The Iron Tanks tank on top of a Water Cooler, if there is one. See wrinkle (W6) of Note [Break rooms].
    private IFluidHandler tankAbove(final IGregTechTileEntity tile) {
        final World world = tile.getWorld();
        final int x = tile.getXCoord();
        final int y = tile.getYCoord() + 1;
        final int z = tile.getZCoord();
        if (!Furniture.WATER_TANK.matches(world.getBlock(x, y, z))) {
            return null;
        }
        final TileEntity te = world.getTileEntity(x, y, z);
        return te instanceof IFluidHandler handler ? handler : null;
    }

    private boolean drainWater(final IFluidHandler tank, final boolean doDrain) {
        final FluidStack drained = tank
            .drain(ForgeDirection.DOWN, new FluidStack(FluidRegistry.WATER, BreakRoomKind.WATER_PER_VISIT), doDrain);
        return drained != null && drained.amount >= BreakRoomKind.WATER_PER_VISIT;
    }

    /// Serves `worker`, come on its break: a cup of water, a cup of coffee or a sandwich. Says whether there was
    /// anything to serve. See Note [Break rooms].
    public boolean visit(final EntityLivingBase worker) {
        final IGregTechTileEntity tile = this.getBaseMetaTileEntity();
        if (this.status != Status.OPEN) {
            return false;
        }
        if (this.kind.isPowered() && !tile.decreaseStoredEnergyUnits(this.kind.euPerVisit, false)) {
            return false;
        }
        final boolean served = switch (this.kind) {
            case WATER_COOLER -> {
                final IFluidHandler tank = this.tankAbove(tile);
                yield tank != null && this.drainWater(tank, true);
            }
            case COFFEE_MACHINE -> this.brew(worker);
            case SNACK_MACHINE -> this.sellSandwich();
        };
        if (served) {
            this.served++;
        }
        return served;
    }

    /// A cup of coffee: a bean, then sugar, cocoa powder and milk if there are any. Its extras go to the worker's
    /// facility.
    private boolean brew(final EntityLivingBase worker) {
        if (!this.take(ItemList.COFFEE_BEANS)) {
            return false;
        }
        int extras = 0;
        if (this.take(ItemList.SUGAR)) {
            extras++;
        }
        if (this.take(ItemList.COCOA_POWDER)) {
            extras++;
        }
        final boolean milk = this.mFluid != null && this.mFluid.amount >= BreakRoomKind.MILK_PER_CUP;
        final boolean soy = this.find(ItemList.SOY_MILK) >= 0;
        if (milk || soy) {
            if (milk && (!soy || ThreadLocalRandom.current()
                .nextBoolean())) {
                this.drain(BreakRoomKind.MILK_PER_CUP, true);
            } else {
                this.take(ItemList.SOY_MILK);
            }
            extras++;
        }
        final Employment employment = Employment.of(worker);
        if (employment != null) {
            employment.shift().cups++;
            employment.shift().cupExtras += extras;
        }
        return true;
    }

    /// One sandwich, of a kind in stock at random.
    private boolean sellSandwich() {
        final List<Integer> slots = new ArrayList<>();
        for (int i = FIRST_SUPPLY; i < FIRST_SUPPLY + SUPPLY_SLOTS; i++) {
            final ItemStack stack = this.getBaseMetaTileEntity()
                .getStackInSlot(i);
            if (stack != null && ItemList.SANDWICHES.matches(stack)) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) {
            return false;
        }
        this.getBaseMetaTileEntity()
            .decrStackSize(
                slots.get(
                    ThreadLocalRandom.current()
                        .nextInt(slots.size())),
                1);
        return true;
    }

    private int sandwichKinds() {
        final Set<Long> kinds = new HashSet<>();
        for (int i = FIRST_SUPPLY; i < FIRST_SUPPLY + SUPPLY_SLOTS; i++) {
            final ItemStack stack = this.getBaseMetaTileEntity()
                .getStackInSlot(i);
            if (stack != null && ItemList.SANDWICHES.matches(stack)) {
                kinds.add(((long) Item.getIdFromItem(stack.getItem()) << 16) | stack.getItemDamage());
            }
        }
        return kinds.size();
    }

    /// Binds the workers in reach, and looks at the kitchen. See Note [Break rooms].
    private void survey(final IGregTechTileEntity tile) {
        final World world = tile.getWorld();
        final int x = tile.getXCoord();
        final int y = tile.getYCoord();
        final int z = tile.getZCoord();
        final long now = world.getTotalWorldTime();
        this.kitchen = Kitchen.around(world, x, y, z);
        final int r = this.kind.chunkRadius;
        final int v = ABConfig.breakRoomVerticalReach;
        final int cx = x >> 4;
        final int cz = z >> 4;
        final List<?> mounts = world.getEntitiesWithinAABB(
            EntityWorkstationMount.class,
            AxisAlignedBB
                .getBoundingBox((cx - r) << 4, y - v, (cz - r) << 4, (cx + r + 1) << 4, y + v + 1, (cz + r + 1) << 4));
        final int dimension = world.provider.dimensionId;
        final Shift.Binding mine = new Shift.Binding(
            dimension,
            x,
            y,
            z,
            this.kind,
            this.kitchen.points(),
            this.stockBonus(),
            this.kitchen.quality(),
            this.status == Status.OPEN,
            this.isPartying(),
            now + 2L * ABConfig.breakRoomScanTicks + 200);
        final int max = ABConfig.breakRoomMaxDistance;
        int bound = 0;
        for (final Object o : mounts) {
            final EntityWorkstationMount mount = (EntityWorkstationMount) o;
            if (mount.isDead || !(mount.riddenByEntity instanceof EntityLivingBase worker)) {
                continue;
            }
            final Employment employment = Employment.of(worker);
            if (employment == null || employment.getRole() != Employment.Role.WORKER
                || Math.abs(mount.getSeatX() - x) > max
                || Math.abs(mount.getSeatZ() - z) > max) {
                continue;
            }
            final Shift shift = employment.shift();
            final Shift.Binding current = shift.room;
            if (current == null || current.isAt(dimension, x, y, z)
                || !current.isValid(dimension, now)
                || !stillThere(world, current)
                || worth(mine) > worth(current)) {
                shift.room = mine;
                bound++;
            }
        }
        this.workers = bound;
        if (bound > 0 && this.status == Status.OPEN && !this.isPartying() && this.take(ItemList.PARTY_FOOD)) {
            this.partyUntil = now + 24000;
            // The party starts now, not at the next scan.
            this.scan.hurry();
        }
    }

    /// Whether the break room `room` is bound to is still there; if its chunk is not loaded, it is assumed to be.
    private static boolean stillThere(final World world, final Shift.Binding room) {
        if (room.dimension() != world.provider.dimensionId || !world.blockExists(room.x(), room.y(), room.z())) {
            return true;
        }
        return world.getTileEntity(room.x(), room.y(), room.z()) instanceof IGregTechTileEntity tile
            && tile.getMetaTileEntity() instanceof MTEBreakRoom;
    }

    /// What a break room is worth to a worker choosing between two, not knowing its facility's coffee: black coffee
    /// with one extra.
    private static int worth(final Shift.Binding room) {
        if (!room.open()) {
            return -1;
        }
        return room.kitchen()
            + (room.kind() == BreakRoomKind.COFFEE_MACHINE ? CoffeeLog.PLAIN + CoffeeLog.PER_EXTRA : room.stockBonus());
    }

    private int find(final ItemList list) {
        final IGregTechTileEntity tile = this.getBaseMetaTileEntity();
        for (int i = FIRST_SUPPLY; i < FIRST_SUPPLY + SUPPLY_SLOTS; i++) {
            final ItemStack stack = tile.getStackInSlot(i);
            if (stack != null && list.matches(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean take(final ItemList list) {
        final int slot = this.find(list);
        if (slot < 0) {
            return false;
        }
        this.getBaseMetaTileEntity()
            .decrStackSize(slot, 1);
        return true;
    }

    // Saving.

    @Override
    public void saveNBTData(final NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setLong("partyUntil", this.partyUntil);
        tag.setInteger("served", this.served);
    }

    @Override
    public void loadNBTData(final NBTTagCompound tag) {
        super.loadNBTData(tag);
        this.partyUntil = tag.getLong("partyUntil");
        this.served = tag.getInteger("served");
    }

    // Power, fluid and items.

    @Override
    public boolean isElectric() {
        return this.kind.isPowered();
    }

    @Override
    public boolean isEnetInput() {
        return this.kind.isPowered();
    }

    @Override
    public boolean isInputFacing(final ForgeDirection side) {
        return this.kind.isPowered() && side != this.getBaseMetaTileEntity()
            .getFrontFacing();
    }

    @Override
    public long maxEUInput() {
        return this.kind.isPowered() ? GTValues.V[this.mTier] : 0;
    }

    @Override
    public long maxEUStore() {
        return this.kind.isPowered() ? GTValues.V[this.mTier] * 1000L : 0;
    }

    @Override
    public long maxAmperesIn() {
        return this.kind.isPowered() ? 2 : 0;
    }

    @Override
    public boolean isFacingValid(final ForgeDirection facing) {
        return facing.offsetY == 0;
    }

    @Override
    public boolean doesFillContainers() {
        return false;
    }

    @Override
    public boolean doesEmptyContainers() {
        return this.kind.hasTank();
    }

    @Override
    public boolean canTankBeFilled() {
        return this.kind.hasTank();
    }

    @Override
    public boolean canTankBeEmptied() {
        return false;
    }

    @Override
    public boolean isFluidInputAllowed(final FluidStack fluid) {
        final Fluid milk = FluidRegistry.getFluid("milk");
        return this.kind.hasTank() && fluid != null && milk != null && fluid.getFluid() == milk;
    }

    @Override
    public int getCapacity() {
        return this.kind.hasTank() ? 16000 : 0;
    }

    private boolean accepts(final ItemStack stack) {
        return stack != null && this.kind.accepts(stack);
    }

    @Override
    public boolean allowPutStack(final IGregTechTileEntity tile, final int index, final ForgeDirection side,
        final ItemStack stack) {
        if (index == this.getInputSlot()) {
            return this.kind.hasTank() && super.allowPutStack(tile, index, side, stack);
        }
        return index >= FIRST_SUPPLY && this.accepts(stack);
    }

    @Override
    public boolean allowPullStack(final IGregTechTileEntity tile, final int index, final ForgeDirection side,
        final ItemStack stack) {
        return index == this.getOutputSlot();
    }

    // Looks.

    @Override
    public ITexture[][][] getTextureSet(final ITexture[] fronts) {
        final ITexture[][][] textures = new ITexture[3][17][];
        for (int color = -1; color < 16; color++) {
            final ITexture casing = Textures.BlockIcons.MACHINE_CASINGS[this.mTier][color + 1];
            textures[0][color + 1] = new ITexture[] { casing };
            textures[1][color + 1] = new ITexture[] { casing, fronts[0] };
            textures[2][color + 1] = new ITexture[] { casing, fronts[1] };
        }
        return textures;
    }

    @Override
    public ITexture[] getTexture(final IGregTechTileEntity tile, final ForgeDirection side, final ForgeDirection facing,
        final int colorIndex, final boolean active, final boolean redstone) {
        final int set = side != facing ? 0 : active ? 2 : 1;
        return this.mTextures[set][colorIndex + 1];
    }

    // The GUI.

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    public boolean onRightclick(final IGregTechTileEntity tile, final EntityPlayer player) {
        this.openGui(player);
        return true;
    }

    /// GregTech opens an MUI2 GUI only while its global switch is on, or when the machine forces it: this one has
    /// no other GUI.
    @Override
    protected boolean forceUseMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(final PosGuiData data, final PanelSyncManager sync, final UISettings settings) {
        sync.registerSlotGroup("supplies", SUPPLY_SLOTS);
        final IntSyncValue status = new IntSyncValue(() -> this.status.ordinal());
        final IntSyncValue workers = new IntSyncValue(() -> this.workers);
        final IntSyncValue fixtures = new IntSyncValue(() -> this.kitchen.fixtures());
        final IntSyncValue plants = new IntSyncValue(() -> this.kitchen.plants());
        final IntSyncValue stock = new IntSyncValue(this::stockBonus);
        final IntSyncValue party = new IntSyncValue(() -> this.isPartying() ? 1 : 0);
        final IntSyncValue served = new IntSyncValue(() -> this.served);
        sync.syncValue("status", status);
        sync.syncValue("workers", workers);
        sync.syncValue("fixtures", fixtures);
        sync.syncValue("plants", plants);
        sync.syncValue("stock", stock);
        sync.syncValue("party", party);
        sync.syncValue("served", served);

        final Flow lines = Flow.column()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .childPadding(2)
            .pos(8, 18)
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocal(Status.values()[status.getIntValue()].key())
                        + (party.getIntValue() != 0 ? StatCollector.translateToLocal("ab.breakroom.gui.party") : ""))
                    .asWidget())
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        "ab.breakroom.gui.workers",
                        workers.getIntValue(),
                        served.getIntValue()))
                    .asWidget())
            .child(IKey.dynamic(() -> {
                final Kitchen k = new Kitchen(fixtures.getIntValue(), plants.getIntValue());
                return StatCollector.translateToLocalFormatted(
                    "ab.breakroom.gui.kitchen",
                    k.fixtures(),
                    k.plants(),
                    k.points(),
                    Kitchen.MAX);
            })
                .asWidget())
            .child(
                IKey.dynamic(
                    () -> StatCollector
                        .translateToLocalFormatted("ab.breakroom.gui." + this.kind.name, stock.getIntValue()))
                    .asWidget());

        final Flow supplies = Flow.row()
            .coverChildren()
            .pos(7, 64);
        for (int i = 0; i < SUPPLY_SLOTS; i++) {
            supplies.child(
                new ItemSlot().slot(
                    new ModularSlot(this.inventoryHandler, FIRST_SUPPLY + i).slotGroup("supplies")
                        .filter(this::accepts)));
        }
        final ModularPanel panel = GTGuis.mteTemplatePanelBuilder(this, data, sync, settings)
            .build()
            .child(lines)
            .child(supplies);
        if (this.kind.hasTank()) {
            final FluidStackTank tank = new FluidStackTank(
                () -> this.mFluid,
                fluid -> this.mFluid = fluid,
                this::getCapacity);
            panel.child(
                new FluidSlot().syncHandler(new FluidSlotSyncHandler(tank).canDrainSlot(false))
                    .alwaysShowFull(false)
                    .pos(151, 18)
                    .size(18, 40));
        }
        return panel;
    }
}
