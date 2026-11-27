package com.gtnewhorizons.appliedbureaucratics.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.compute.PlanningPool;
import com.gtnewhorizons.appliedbureaucratics.facility.IComputeFacility;
import com.gtnewhorizons.appliedbureaucratics.facility.desk.TileRequisitionTray;
import com.gtnewhorizons.appliedbureaucratics.integration.ae2.IComputeGrid;

import appeng.api.AEApi;
import appeng.api.config.CraftingMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.compute.ICraftingPlanningJob;
import appeng.api.networking.security.PlayerSource;
import appeng.crafting.v2.CraftingJobV2;
import appeng.helpers.IInterfaceHost;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cluster.implementations.ICraftingCPUHost;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.registry.GameData;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/// `/ab status` reports the nearest Desk and its network's compute; `/ab craft <item> [count] [meta]` asks that
/// network to plan and start a craft, as the craft confirm screen's Queue button does; `/ab debug pattern ...`
/// encodes a processing pattern into an inventory, for in-world tests.
public class CommandAB extends CommandBase {

    @Override
    public String getCommandName() {
        return "ab";
    }

    @Override
    public String getCommandUsage(final ICommandSender sender) {
        return "/ab status [x y z] | /ab craft <item> [count] [meta] | /ab debug pattern <x> <y> <z> <slot> <in> <inCount> <out> <outCount> | /ab debug ocitem <x> <y> <z> <slot> <name>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(final ICommandSender sender) {
        return true;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(final ICommandSender sender, final String[] args) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "status", "craft", "debug") : null;
    }

    @Override
    public void processCommand(final ICommandSender sender, final String[] args) {
        if (args.length == 0) {
            throw new WrongUsageException(this.getCommandUsage(sender));
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> this.status(
                sender,
                args.length >= 4
                    ? nearest(
                        sender.getEntityWorld(),
                        new ChunkCoordinates(
                            parseInt(sender, args[1]),
                            parseInt(sender, args[2]),
                            parseInt(sender, args[3])))
                    : nearest(sender));
            case "craft" -> this.craft(sender, Arrays.copyOfRange(args, 1, args.length));
            case "debug" -> {
                if (!sender.canCommandSenderUseCommand(2, "ab")) {
                    throw new WrongUsageException("/ab debug is for operators.");
                }
                this.debug(sender, Arrays.copyOfRange(args, 1, args.length));
            }
            default -> throw new WrongUsageException(this.getCommandUsage(sender));
        }
    }

    private static void say(final ICommandSender sender, final String text) {
        sender.addChatMessage(new ChatComponentText(text));
    }

    /// A facility found near a player: what to call it, where it is, and the facility itself.
    private record Found(String name, int x, int y, int z, IComputeFacility facility, ICraftingCPUHost host) {}

    private static @Nullable Found nearest(final ICommandSender sender) {
        return nearest(sender.getEntityWorld(), sender.getPlayerCoordinates());
    }

    private static @Nullable Found nearest(final World world, final ChunkCoordinates at) {
        Found best = null;
        double bestDistance = 16 * 16;
        for (final Object o : world.loadedTileEntityList) {
            final TileEntity te = (TileEntity) o;
            if (te.isInvalid()) {
                continue;
            }
            Found found = null;
            if (te instanceof TileRequisitionTray tray) {
                found = new Found("Desk", te.xCoord, te.yCoord, te.zCoord, tray, tray);
            } else
                if (te instanceof IGregTechTileEntity gt && gt.getMetaTileEntity() instanceof IComputeFacility facility
                    && facility instanceof ICraftingCPUHost host) {
                        found = new Found(
                            gt.getMetaTileEntity()
                                .getLocalName(),
                            te.xCoord,
                            te.yCoord,
                            te.zCoord,
                            facility,
                            host);
                    }
            if (found != null) {
                final double d = at.getDistanceSquared(te.xCoord, te.yCoord, te.zCoord);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = found;
                }
            }
        }
        return best;
    }

    private static @Nullable IGrid gridOf(final Found found) {
        final IGridNode node = found.host()
            .getActionableNode();
        return node != null ? node.getGrid() : null;
    }

    private void status(final ICommandSender sender, final @Nullable Found found) {
        if (found == null) {
            say(sender, "No facility within 16 blocks.");
            return;
        }
        final List<String> lines = found.facility()
            .statusLines();
        say(
            sender,
            String.format(
                Locale.ROOT,
                "%s at %d %d %d: %s",
                found.name(),
                found.x(),
                found.y(),
                found.z(),
                lines.isEmpty() ? "" : lines.get(0)));
        for (int i = 1; i < lines.size(); i++) {
            say(sender, lines.get(i));
        }
        final IGrid grid = gridOf(found);
        if (grid != null) {
            final IComputeGrid compute = grid.getCache(IComputeGrid.class);
            final PlanningPool<ICraftingPlanningJob> pool = compute.getPlanningPool();
            say(
                sender,
                String.format(
                    Locale.ROOT,
                    "  network: %d facilities, planning %.2f steps/s, credit %.1f, %d plans%s",
                    compute.getFacilities()
                        .size(),
                    pool.getRate(),
                    pool.getCredit(),
                    pool.planCount(),
                    pool.isUnmetered() ? ", unmetered (legacy CPUs)" : ""));
        }
    }

    private void craft(final ICommandSender sender, final String[] args) {
        if (!(sender instanceof EntityPlayer player) || args.length < 1) {
            throw new WrongUsageException("/ab craft <item> [count] [meta], as a player near a Desk");
        }
        final Item item = (Item) GameData.getItemRegistry()
            .getObject(args[0]);
        if (item == null) {
            throw new WrongUsageException("No item " + args[0]);
        }
        final int count = args.length > 1 ? parseIntWithMin(sender, args[1], 1) : 1;
        final int meta = args.length > 2 ? parseInt(sender, args[2]) : 0;
        final Found found = nearest(sender);
        final IGrid grid = found != null ? gridOf(found) : null;
        if (grid == null || !(grid.getCache(ICraftingGrid.class) instanceof CraftingGridCache crafting)) {
            say(sender, "No networked facility within 16 blocks.");
            return;
        }
        final PlayerSource source = new PlayerSource(player, found.host());
        final Object job = crafting.beginCraftingJob(
            player.worldObj,
            grid,
            source,
            Platform.convertStack(AEItemStack.create(new ItemStack(item, count, meta))),
            CraftingMode.STANDARD,
            false,
            null);
        if (job instanceof CraftingJobV2<?>planning && crafting.getPlannedCrafts()
            .startWhenPlanned(planning, source, false)) {
            say(sender, "Queued: " + count + " " + args[0] + ". It starts when it is planned.");
        } else {
            say(sender, "The plan could not be queued.");
        }
    }

    private void debug(final ICommandSender sender, final String[] args) {
        if (args.length == 6 && "ocitem".equals(args[0])) {
            this.debugOcItem(sender, args);
            return;
        }
        if (args.length < 9 || !"pattern".equals(args[0])) {
            throw new WrongUsageException(this.getCommandUsage(sender));
        }
        final int x = parseInt(sender, args[1]);
        final int y = parseInt(sender, args[2]);
        final int z = parseInt(sender, args[3]);
        final int slot = parseInt(sender, args[4]);
        final Item in = (Item) GameData.getItemRegistry()
            .getObject(args[5]);
        final Item out = (Item) GameData.getItemRegistry()
            .getObject(args[7]);
        if (in == null || out == null) {
            throw new WrongUsageException("Unknown item");
        }
        final TileEntity te = sender.getEntityWorld()
            .getTileEntity(x, y, z);
        // An interface's patterns are not its IInventory, which is its storage.
        final Object target = te instanceof IInterfaceHost host ? host.getPatterns() : te;
        if (!(target instanceof IInventory inventory) || slot < 0 || slot >= inventory.getSizeInventory()) {
            throw new WrongUsageException("No inventory slot " + slot + " at " + x + " " + y + " " + z);
        }
        final ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .maybeStack(1)
            .orNull();
        final NBTTagCompound tag = new NBTTagCompound();
        final NBTTagList inputs = new NBTTagList();
        inputs.appendTag(new ItemStack(in, parseInt(sender, args[6])).writeToNBT(new NBTTagCompound()));
        final NBTTagList outputs = new NBTTagList();
        outputs.appendTag(new ItemStack(out, parseInt(sender, args[8])).writeToNBT(new NBTTagCompound()));
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        pattern.setTagCompound(tag);
        inventory.setInventorySlotContents(slot, pattern);
        say(sender, "Pattern set.");
    }

    /// `/ab debug ocitem <x> <y> <z> <slot> <name>` puts OpenComputers' item `name` (as its API names it: `server1`,
    /// `screen1`) into an inventory slot, since OC's items are sub-items whose damage values the API hides.
    private void debugOcItem(final ICommandSender sender, final String[] args) {
        final TileEntity te = sender.getEntityWorld()
            .getTileEntity(parseInt(sender, args[1]), parseInt(sender, args[2]), parseInt(sender, args[3]));
        final int slot = parseInt(sender, args[4]);
        if (!(te instanceof IInventory inventory) || slot < 0 || slot >= inventory.getSizeInventory()) {
            throw new WrongUsageException("No inventory slot " + slot);
        }
        final li.cil.oc.api.detail.ItemInfo info = li.cil.oc.api.Items.get(args[5]);
        if (info == null) {
            throw new WrongUsageException("No OpenComputers item " + args[5]);
        }
        inventory.setInventorySlotContents(slot, info.createItemStack(1));
        say(sender, "Item set.");
    }
}
