package com.gtnewhorizons.appliedbureaucratics.facility.desk;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.config.ABConfig;
import com.gtnewhorizons.appliedbureaucratics.config.ItemList;
import com.gtnewhorizons.appliedbureaucratics.facility.Amenities;
import com.gtnewhorizons.appliedbureaucratics.integration.bibliocraft.Furniture;
import com.gtnewhorizons.appliedbureaucratics.workforce.Employment;
import com.gtnewhorizons.appliedbureaucratics.workforce.EntityWorkstationMount;
import com.gtnewhorizons.appliedbureaucratics.workforce.WorkforceActions;

/// What a Requisition Tray finds around it: the seat and who sits on it, the desk furniture, the books, and the
/// amenities that make up a worker's morale. See the design document, section 6.1.
public record DeskSurvey(boolean seat, int @Nullable [] seatAt, @Nullable EntityLivingBase worker,
    @Nullable EntityPlayer founder, boolean desk, boolean typewriter, boolean bell, long filing, int amenities) {

    public static final DeskSurvey NOTHING = new DeskSurvey(false, null, null, null, false, false, false, 0, 0);

    public boolean hasWorker() {
        return this.worker != null || this.founder != null;
    }

    public static DeskSurvey of(final World world, final int x, final int y, final int z) {
        final int seatRange = ABConfig.deskSeatRange;
        int[] seat = null;
        EntityLivingBase worker = null;
        EntityPlayer founder = null;
        for (int dx = -seatRange; dx <= seatRange && worker == null && founder == null; dx++) {
            for (int dy = -seatRange; dy <= seatRange && worker == null && founder == null; dy++) {
                for (int dz = -seatRange; dz <= seatRange && worker == null && founder == null; dz++) {
                    final int sx = x + dx;
                    final int sy = y + dy;
                    final int sz = z + dz;
                    if (!Furniture.SEAT.matches(world.getBlock(sx, sy, sz))) {
                        continue;
                    }
                    if (seat == null) {
                        seat = new int[] { sx, sy, sz };
                    }
                    final EntityWorkstationMount mount = WorkforceActions.mountAt(world, sx, sy, sz);
                    if (mount != null && mount.riddenByEntity instanceof EntityLivingBase rider
                        && Employment.isOnShift(rider)) {
                        worker = rider;
                        seat = new int[] { sx, sy, sz };
                    } else {
                        founder = seatedPlayer(world, sx, sy, sz);
                        if (founder != null) {
                            seat = new int[] { sx, sy, sz };
                        }
                    }
                }
            }
        }
        if (seat == null) {
            return new DeskSurvey(false, null, null, null, false, false, false, filing(world, x, y, z), 0);
        }
        final int sx = seat[0];
        final int sy = seat[1];
        final int sz = seat[2];
        final boolean desk = nextTo(world, sx, sy, sz, Furniture.DESK);
        final boolean typewriter = within(world, sx, sy, sz, 1, Furniture.TYPEWRITER);
        final boolean bell = within(world, sx, sy, sz, 1, Furniture.BELL);
        return new DeskSurvey(
            true,
            seat,
            worker,
            founder,
            desk,
            typewriter,
            bell,
            filing(world, x, y, z),
            Amenities.score(world, sx, sy, sz));
    }

    /// The player sitting on the seat at `x, y, z`, if any: a founder. See Note [Founder mode].
    public static @Nullable EntityPlayer seatedPlayer(final World world, final int x, final int y, final int z) {
        final List<EntityPlayer> players = world
            .getEntitiesWithinAABB(EntityPlayer.class, AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 2, z + 1));
        for (final EntityPlayer player : players) {
            if (player.ridingEntity != null) {
                return player;
            }
        }
        return null;
    }

    private static boolean nextTo(final World world, final int x, final int y, final int z, final Furniture role) {
        return role.matches(world.getBlock(x + 1, y, z)) || role.matches(world.getBlock(x - 1, y, z))
            || role.matches(world.getBlock(x, y, z + 1))
            || role.matches(world.getBlock(x, y, z - 1));
    }

    private static boolean within(final World world, final int x, final int y, final int z, final int r,
        final Furniture role) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (role.matches(world.getBlock(x + dx, y + dy, z + dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /// Filing from the books within range of the tray: so many bytes per book in a bookcase, and per vanilla
    /// bookshelf, up to the desk's cap.
    private static long filing(final World world, final int x, final int y, final int z) {
        final int r = ABConfig.deskBookcaseRange;
        long bytes = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    final Block block = world.getBlock(x + dx, y + dy, z + dz);
                    if (block == Blocks.bookshelf) {
                        bytes += ABConfig.deskBytesPerBookshelf;
                    } else if (Furniture.BOOKCASE.matches(block)) {
                        final TileEntity te = world.getTileEntity(x + dx, y + dy, z + dz);
                        if (te instanceof IInventory inventory) {
                            bytes += (long) books(inventory) * ABConfig.deskBytesPerBook;
                        }
                    }
                }
            }
        }
        return Math.min(bytes, ABConfig.deskFilingCap);
    }

    private static int books(final IInventory inventory) {
        int books = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            final ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null && ItemList.BOOK.matches(stack)) {
                books += stack.stackSize;
            }
        }
        return books;
    }

}
