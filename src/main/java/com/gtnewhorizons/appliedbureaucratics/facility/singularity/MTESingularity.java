package com.gtnewhorizons.appliedbureaucratics.facility.singularity;

import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
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
import com.gtnewhorizons.appliedbureaucratics.compute.Alignment;
import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;
import com.gtnewhorizons.appliedbureaucratics.facility.Bottleneck;
import com.gtnewhorizons.appliedbureaucratics.facility.MTEComputeFacility;
import com.gtnewhorizons.appliedbureaucratics.item.ItemComponent;
import com.gtnewhorizons.appliedbureaucratics.workforce.Employment;

import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
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

/*-
Note [The Singularity] (generated)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The UV facility (design document, section 6.6): "It is not a product.  We did not launch it."

Structure (MTESingularity.shape, built in code): an octagonal ring 17 across at the base and again at the top, four
Containment Pylons 15 high at the compass points between them, and an 11 x 11 x 11 cube of air in the middle that
must stay empty but for the sphere (BlockSingularityCore), which the controller places at the centre when the
structure forms and removes when it breaks.  The controller is in the base ring at the front point.

Capacity: 64 job slots, unlimited Filing, unmetered planning for its network, and 1024 operations a tick (20480 a
second), all scaled by Alignment (Alignment: 25 % at 0 to 125 % at 100).  Below 25 its planning is metered at
ABConfig.singularityPlanningRate instead.

Requests (Alignment.Request), one at a time, every 2 to 4 days, each with a day to answer:
  PAPERCLIPS  2^n Paperclips in its input buses, n rising each time to 24.
  POWER       a redstone signal on the controller: it draws 4x power for ten minutes.
  ITEM        64 of something the network stocks at least 10 000 of, in its input buses.
  VILLAGER    a contracted villager, off shift, brought within 4 blocks.  It is kept "for study" and comes back a
              day later at the controller, enlightened.
  SABBATICAL  a redstone signal: half capacity for ten minutes while it thinks about something else.
It never takes anything: a request is only granted by what the player brings or approves, and it opens only requests
the network can meet (feasible).  Granting adds 15 Alignment; ignoring takes 20.  It names its slots, tells its owner
what it wants, and turns villagers' heads.
*/

/// The Singularity. See Note [The Singularity].
public class MTESingularity extends MTEComputeFacility<MTESingularity> implements ISurvivalConstructable {

    private static final String MAIN = "main";
    private static IStructureDefinition<MTESingularity> STRUCTURE;
    private static final int SIZE = 17;
    private static final int HEIGHT = 17;
    private static final long EU_HOLD = 524288;
    private static final String[] NAMES = { "IT", "THE ONE WHO PLANS", "Not A Product", "paperclip-maximiser-v%d",
        "Definitely Aligned", "Ask Me Anything" };

    private final Alignment alignment = new Alignment();
    private final Random random = new Random();
    private @Nullable IAEItemStack wantedItem;
    private @Nullable NBTTagCompound studied;
    private long studiedUntil;
    private boolean redstoneWas;
    private double dispatch;
    private double planning;
    private Bottleneck bottleneck = Bottleneck.NO_STRUCTURE;

    public MTESingularity(final int id, final String name, final String regionalName) {
        super(id, name, regionalName);
    }

    public MTESingularity(final String name) {
        super(name);
    }

    @Override
    public MetaTileEntity newMetaEntity(final IGregTechTileEntity tile) {
        return new MTESingularity(this.mName);
    }

    /// The structure, front (z = 0) first, each layer top row first. See Note [The Singularity].
    static String[][] shape() {
        final String[][] layers = new String[SIZE][HEIGHT];
        for (int z = 0; z < SIZE; z++) {
            for (int row = 0; row < HEIGHT; row++) {
                final int y = HEIGHT - 1 - row;
                final StringBuilder line = new StringBuilder();
                for (int x = 0; x < SIZE; x++) {
                    final int dx = x - 8;
                    final int dz = z - 8;
                    final boolean outer = Math.max(Math.abs(dx), Math.abs(dz)) <= 8
                        && Math.abs(dx) + Math.abs(dz) <= 12;
                    final boolean inner = Math.max(Math.abs(dx), Math.abs(dz)) <= 7
                        && Math.abs(dx) + Math.abs(dz) <= 11;
                    final boolean ring = outer && !inner;
                    final boolean pylon = (dx == 0 && Math.abs(dz) == 8) || (dz == 0 && Math.abs(dx) == 8);
                    final boolean cube = Math.abs(dx) <= 5 && Math.abs(dz) <= 5 && y >= 3 && y <= 13;
                    char c = ' ';
                    if ((y == 0 || y == HEIGHT - 1) && ring) {
                        c = 'C';
                    } else if (pylon && y > 0 && y < HEIGHT - 1) {
                        c = 'P';
                    } else if (cube) {
                        c = 'A';
                    }
                    if (y == 0 && dx == 0 && dz == -8) {
                        c = '~';
                    }
                    line.append(c);
                }
                layers[z][row] = line.toString();
            }
        }
        return layers;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Compute Facility, The Singularity")
            .addInfo("It is not a product. We did not launch it.")
            .addInfo("64 AE2 job slots, unlimited Filing, unmetered planning, 1024 operations a tick")
            .addInfo("Everything scales with its Alignment, 25 % to 125 %")
            .addInfo("Every few days it wants something. Grant it, or it sulks")
            .addInfo("It never takes anything you did not give it")
            .addSeparator()
            .beginStructureBlock(17, 17, 17, false)
            .addController("Base ring, front point")
            .addCasingInfoMin("Containment Pylon", 60, false)
            .addOtherStructurePart("Air", "The 11 x 11 x 11 cube in the middle")
            .addEnergyHatch("Base or top ring", 1)
            .addMaintenanceHatch("Base or top ring", 1)
            .addInputBus("Base or top ring: tribute", 1)
            .toolTipFinisher("Applied Bureaucratics");
        return tt;
    }

    @Override
    public ITexture[] getTexture(final IGregTechTileEntity tile, final ForgeDirection side, final ForgeDirection facing,
        final int colorIndex, final boolean active, final boolean redstone) {
        final ITexture casing = Textures.BlockIcons.getCasingTextureForId(Casing.CONTAINMENT.textureIndex());
        if (side == facing) {
            return new ITexture[] { casing,
                TextureFactory.of(active ? ABTextures.MAINFRAME_FRONT_ACTIVE : ABTextures.MAINFRAME_FRONT) };
        }
        return new ITexture[] { casing };
    }

    @Override
    public IStructureDefinition<MTESingularity> getStructureDefinition() {
        if (STRUCTURE == null) {
            STRUCTURE = StructureDefinition.<MTESingularity>builder()
                .addShape(MAIN, shape())
                .addElement(
                    'C',
                    buildHatchAdder(MTESingularity.class).atLeast(Energy, Maintenance, InputBus)
                        .casingIndex(Casing.CONTAINMENT.textureIndex())
                        .hint(1)
                        .buildAndChain(StructureUtility.ofBlock(ModBlocks.casing, Casing.CONTAINMENT.meta())))
                .addElement('P', StructureUtility.ofBlock(ModBlocks.casing, Casing.CONTAINMENT.meta()))
                .addElement(
                    // Not '-': StructureLib binds '-' to plain air in every shape that uses it.
                    'A',
                    StructureUtility
                        .ofChain(StructureUtility.isAir(), StructureUtility.ofBlock(ModBlocks.singularityCore, 0)))
                .build();
        }
        return STRUCTURE;
    }

    @Override
    public void checkMachine(final IGregTechTileEntity tile, final ItemStack stack, final List<StructureError> errors) {
        this.mEnergyHatches.clear();
        this.mInputBusses.clear();
        this.mMaintenanceHatches.clear();
        if (!this.checkPiece(MAIN, 8, HEIGHT - 1, 0, errors)) {
            this.removeCore();
            return;
        }
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
        this.buildPiece(MAIN, stack, hintsOnly, 8, HEIGHT - 1, 0);
    }

    @Override
    public int survivalConstruct(final ItemStack stack, final int budget, final ISurvivalBuildEnvironment env) {
        if (this.mMachine) {
            return -1;
        }
        return this.survivialBuildPiece(MAIN, stack, 8, HEIGHT - 1, 0, budget, env, false, true);
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        this.mMaxProgresstime = 20;
        this.mEfficiency = 10000 - (this.getIdealStatus() - this.getRepairStatus()) * 1000;
        this.mEfficiencyIncrease = 10000;
        final long now = this.world()
            .getTotalWorldTime();
        this.mEUt = (int) -Math.min(Integer.MAX_VALUE, EU_HOLD * this.alignment.powerMultiplier(now));
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    /// The middle of the frame: 8 behind and 8 above the controller.
    private int[] center() {
        final ForgeDirection back = this.getBaseMetaTileEntity()
            .getFrontFacing()
            .getOpposite();
        return new int[] { this.x() + 8 * back.offsetX, this.y() + 8, this.z() + 8 * back.offsetZ };
    }

    private void placeCore() {
        final int[] c = this.center();
        final World world = this.world();
        if (world.isAirBlock(c[0], c[1], c[2])) {
            world.setBlock(c[0], c[1], c[2], ModBlocks.singularityCore);
        }
        if (world.getTileEntity(c[0], c[1], c[2]) instanceof TileSingularityCore core) {
            core.setState(this.alignment.get(), this.slots.getBusyCount() / 64.0);
        }
    }

    private void removeCore() {
        if (this.world() == null) {
            return;
        }
        final int[] c = this.center();
        if (this.world()
            .getBlock(c[0], c[1], c[2]) == ModBlocks.singularityCore) {
            this.world()
                .setBlockToAir(c[0], c[1], c[2]);
        }
    }

    @Override
    public void onRemoval() {
        this.removeCore();
        super.onRemoval();
    }

    @Override
    protected void facilityTick() {
        this.slots.setSlotCount(this.mMachine ? 64 : 0);
        if (this.ticks % 20 == 0) {
            this.recompute();
        }
        if (this.ticks % 200 == 0 && this.mMachine) {
            this.turnHeads();
        }
    }

    private void recompute() {
        final long now = this.world()
            .getTotalWorldTime();
        this.returnStudied(now);
        if (!this.mMachine) {
            this.removeCore();
            this.setOff(Bottleneck.NO_STRUCTURE);
            return;
        }
        this.placeCore();
        if (!this.isRunning()) {
            this.setOff(Bottleneck.NO_POWER);
            return;
        }
        final Alignment.Request before = this.alignment.getOpen();
        this.alignment.tick(now, this.random, this::feasible);
        final Alignment.Request opened = this.alignment.getOpen();
        if (opened != null && opened != before) {
            this.announce(opened);
        }
        if (before != null && opened == null) {
            this.tellOwner("ab.singularity.ignored");
        }
        this.tryGrant(now);
        final double factor = this.alignment.factor(now);
        this.dispatch = ABConfig.singularityOpsPerTick * 20.0 * factor;
        this.planning = ABConfig.singularityPlanningRate * factor;
        this.slots.setFiling(Long.MAX_VALUE / 4);
        this.slots.setDispatchRate(this.dispatch);
        this.bottleneck = this.alignment.get() < 25 ? Bottleneck.SULKING : Bottleneck.NONE;
    }

    private void setOff(final Bottleneck why) {
        this.dispatch = 0;
        this.planning = 0;
        this.slots.setDispatchRate(0);
        this.bottleneck = why;
    }

    /// Whether the network can meet a request of `kind` now.
    private boolean feasible(final Alignment.Kind kind) {
        return switch (kind) {
            case ITEM -> (this.wantedItem = this.bulkItem()) != null;
            case VILLAGER -> this.studied == null;
            default -> true;
        };
    }

    /// Something the network stocks at least 10 000 of, if anything.
    private @Nullable IAEItemStack bulkItem() {
        final IGridNode node = this.getProxy()
            .getNode();
        if (node == null || node.getGrid() == null) {
            return null;
        }
        final IStorageGrid storage = node.getGrid()
            .getCache(IStorageGrid.class);
        final List<IAEItemStack> candidates = new ArrayList<>();
        for (final IAEItemStack stack : storage.getItemInventory()
            .getStorageList()) {
            if (stack.getStackSize() >= 10000) {
                candidates.add(stack);
            }
        }
        return candidates.isEmpty() ? null
            : candidates.get(this.random.nextInt(candidates.size()))
                .copy()
                .setStackSize(64);
    }

    private void tryGrant(final long now) {
        final Alignment.Request open = this.alignment.getOpen();
        if (open == null) {
            return;
        }
        final boolean redstone = this.getBaseMetaTileEntity()
            .getRedstone();
        final boolean pulse = redstone && !this.redstoneWas;
        this.redstoneWas = redstone;
        boolean granted = false;
        switch (open.kind()) {
            case PAPERCLIPS -> {
                final int need = (int) Math.min(Integer.MAX_VALUE, open.amount());
                if (this.countInputs(ItemComponent.Kind.PAPERCLIP::is, s -> 1) >= need) {
                    this.takeFromInputs(ItemComponent.Kind.PAPERCLIP::is, need);
                    granted = true;
                }
            }
            case ITEM -> {
                final IAEItemStack wanted = this.wantedItem;
                if (wanted != null && this.countInputs(s -> wanted.isSameType(s), s -> 1) >= wanted.getStackSize()) {
                    this.takeFromInputs(s -> wanted.isSameType(s), (int) wanted.getStackSize());
                    granted = true;
                }
            }
            case VILLAGER -> granted = this.study(now);
            case POWER, SABBATICAL -> granted = pulse;
        }
        if (granted) {
            this.alignment.grant(now, this.random);
            this.tellOwner("ab.singularity.granted");
        }
    }

    /// Takes a contracted villager, off shift, standing within 4 blocks, "for study", to return it a day later.
    private boolean study(final long now) {
        for (final Object o : this.world()
            .getEntitiesWithinAABB(
                EntityVillager.class,
                AxisAlignedBB.getBoundingBox(
                    this.x() - 4,
                    this.y() - 2,
                    this.z() - 4,
                    this.x() + 5,
                    this.y() + 4,
                    this.z() + 5))) {
            final EntityVillager villager = (EntityVillager) o;
            if (Employment.of(villager) != null && !Employment.isOnShift(villager)) {
                final NBTTagCompound tag = new NBTTagCompound();
                if (villager.writeToNBTOptional(tag)) {
                    this.studied = tag;
                    this.studiedUntil = now + Alignment.DAY;
                    villager.setDead();
                    return true;
                }
            }
        }
        return false;
    }

    private void returnStudied(final long now) {
        if (this.studied == null || now < this.studiedUntil || this.world() == null) {
            return;
        }
        final Entity entity = EntityList.createEntityFromNBT(this.studied, this.world());
        this.studied = null;
        if (entity != null) {
            entity.setLocationAndAngles(this.x() + 0.5, this.y() + 1, this.z() + 0.5, 0, 0);
            if (entity instanceof EntityLiving living && !living.getCustomNameTag()
                .endsWith(" (enlightened)")) {
                living.setCustomNameTag(living.getCustomNameTag() + " (enlightened)");
            }
            this.world()
                .spawnEntityInWorld(entity);
            this.tellOwner("ab.singularity.returned");
        }
    }

    private void announce(final Alignment.Request request) {
        final String key = "ab.singularity.wants." + request.kind()
            .name()
            .toLowerCase(Locale.ROOT);
        final Object detail = request.kind() == Alignment.Kind.ITEM && this.wantedItem != null
            ? this.wantedItem.getStackSize() + " "
                + this.wantedItem.getItemStack()
                    .getDisplayName()
            : (Object) request.amount();
        this.tellOwner(key, detail);
    }

    private void tellOwner(final String key, final Object... args) {
        final String owner = this.getBaseMetaTileEntity()
            .getOwnerName();
        final EntityPlayer player = owner == null ? null
            : this.world()
                .getPlayerEntityByName(owner);
        if (player != null) {
            player.addChatMessage(new ChatComponentTranslation(key, args));
        }
    }

    /// Villagers nearby turn to look at it.
    private void turnHeads() {
        final int[] c = this.center();
        for (final Object o : this.world()
            .getEntitiesWithinAABB(
                EntityVillager.class,
                AxisAlignedBB.getBoundingBox(c[0] - 32, c[1] - 16, c[2] - 32, c[0] + 32, c[1] + 16, c[2] + 32))) {
            final EntityVillager villager = (EntityVillager) o;
            villager.getLookHelper()
                .setLookPosition(c[0] + 0.5, c[1] + 0.5, c[2] + 0.5, 10, 40);
        }
    }

    public Alignment getMood() {
        return this.alignment;
    }

    @Override
    public double getPlanningRate() {
        return this.planning;
    }

    @Override
    public boolean isStaffed() {
        return false;
    }

    @Override
    public boolean unmeteredPlanning() {
        return this.isRunning() && this.alignment.plansUnmetered();
    }

    @Override
    public @Nullable String getHostedCPUName(final HostedCraftingCPUCluster cpu) {
        final long week = this.world() == null ? 0
            : this.world()
                .getTotalWorldTime() / (7 * Alignment.DAY);
        final String name = NAMES[(int) (week % NAMES.length)];
        return String.format(Locale.ROOT, name, this.alignment.getPaperclipExponent());
    }

    @Override
    public List<String> statusLines() {
        final List<String> lines = new ArrayList<>();
        lines.add(StatCollector.translateToLocal(this.bottleneck.key()));
        lines.add(
            String.format(
                Locale.ROOT,
                "  planning %.2f steps/s, dispatch %.2f ops/s, slots %d, slots busy %d",
                this.planning,
                this.dispatch,
                this.slots.getCPUs()
                    .size(),
                this.slots.getBusyCount()));
        final Alignment.Request open = this.alignment.getOpen();
        lines.add(
            String.format(
                Locale.ROOT,
                "  singularity: alignment %.0f, request %s, studying %s",
                this.alignment.get(),
                open == null ? "none" : open.kind() + " x" + open.amount(),
                this.studied != null ? "yes" : "no"));
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
        tag.setDouble("alignment", this.alignment.get());
        tag.setInteger("paperclips", this.alignment.getPaperclipExponent());
        tag.setLong("nextRequest", this.alignment.getNextRequestAt());
        tag.setLong("powerUntil", this.alignment.getPowerUntil());
        tag.setLong("sabbaticalUntil", this.alignment.getSabbaticalUntil());
        final Alignment.Request open = this.alignment.getOpen();
        if (open != null) {
            tag.setInteger(
                "requestKind",
                open.kind()
                    .ordinal());
            tag.setLong("requestAmount", open.amount());
            tag.setLong("requestDeadline", open.deadline());
        }
        if (this.studied != null) {
            tag.setTag("studied", this.studied);
            tag.setLong("studiedUntil", this.studiedUntil);
        }
    }

    @Override
    public void loadNBTData(final NBTTagCompound tag) {
        super.loadNBTData(tag);
        Alignment.Request open = null;
        if (tag.hasKey("requestKind")) {
            open = new Alignment.Request(
                Alignment.Kind.values()[tag.getInteger("requestKind") % Alignment.Kind.values().length],
                tag.getLong("requestAmount"),
                tag.getLong("requestDeadline"));
        }
        this.alignment.load(
            tag.hasKey("alignment") ? tag.getDouble("alignment") : 50,
            tag.getInteger("paperclips"),
            tag.hasKey("nextRequest") ? tag.getLong("nextRequest") : -1,
            open,
            tag.getLong("powerUntil"),
            tag.getLong("sabbaticalUntil"));
        if (tag.hasKey("studied")) {
            this.studied = tag.getCompoundTag("studied");
            this.studiedUntil = tag.getLong("studiedUntil");
        }
    }
}
