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
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.compute.PlanningPool;
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
import appeng.me.cache.CraftingGridCache;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.registry.GameData;

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
        return "/ab status | /ab craft <item> [count] [meta] | /ab debug pattern <x> <y> <z> <slot> <in> <inCount> <out> <outCount>";
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
            case "status" -> this.status(sender);
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

    private static @Nullable TileRequisitionTray nearestTray(final ICommandSender sender) {
        final World world = sender.getEntityWorld();
        final ChunkCoordinates at = sender.getPlayerCoordinates();
        TileRequisitionTray best = null;
        double bestDistance = 16 * 16;
        for (final Object o : world.loadedTileEntityList) {
            if (o instanceof TileRequisitionTray tray && !tray.isInvalid()) {
                final double d = at.getDistanceSquared(tray.xCoord, tray.yCoord, tray.zCoord);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = tray;
                }
            }
        }
        return best;
    }

    private static @Nullable IGrid gridOf(final TileRequisitionTray tray) {
        final IGridNode node = tray.getActionableNode();
        return node != null ? node.getGrid() : null;
    }

    private void status(final ICommandSender sender) {
        final TileRequisitionTray tray = nearestTray(sender);
        if (tray == null) {
            say(sender, "No Requisition Tray within 16 blocks.");
            return;
        }
        say(
            sender,
            String.format(
                Locale.ROOT,
                "Desk at %d %d %d: %s",
                tray.xCoord,
                tray.yCoord,
                tray.zCoord,
                StatCollector.translateToLocal(
                    tray.getBottleneck()
                        .key())));
        say(
            sender,
            String.format(
                Locale.ROOT,
                "  planning %.2f steps/s, dispatch %.2f ops/s, filing %d/%d B, morale %.0f, %s, worker %s, slots busy %d",
                tray.getOutput()
                    .planningRate(),
                tray.getOutput()
                    .dispatchRate(),
                tray.getSlots()
                    .getUsedFiling(),
                tray.getSlots()
                    .getFiling(),
                tray.getMorale()
                    .get(),
                tray.isPaid() ? "paid" : "unpaid",
                tray.getSurvey()
                    .worker() != null ? "employee"
                        : tray.getSurvey()
                            .founder() != null ? "founder" : "none",
                tray.getSlots()
                    .getBusyCount()));
        final IGrid grid = gridOf(tray);
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
        final TileRequisitionTray tray = nearestTray(sender);
        final IGrid grid = tray != null ? gridOf(tray) : null;
        if (grid == null || !(grid.getCache(ICraftingGrid.class) instanceof CraftingGridCache crafting)) {
            say(sender, "No networked Requisition Tray within 16 blocks.");
            return;
        }
        final PlayerSource source = new PlayerSource(player, tray);
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
        if (!(te instanceof IInventory inventory) || slot < 0 || slot >= inventory.getSizeInventory()) {
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
}
