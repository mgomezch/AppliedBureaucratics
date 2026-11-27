package com.gtnewhorizons.appliedbureaucratics.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.gtnewhorizons.appliedbureaucratics.AppliedBureaucratics;
import com.gtnewhorizons.appliedbureaucratics.facility.breakroom.TileBreakRoomCounter;
import com.gtnewhorizons.appliedbureaucratics.gui.GuiHandler;
import com.gtnewhorizons.appliedbureaucratics.item.ABCreativeTab;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The Break Room Counter. See [TileBreakRoomCounter].
public class BlockBreakRoomCounter extends BlockContainer {

    private IIcon top;

    public BlockBreakRoomCounter() {
        super(Material.wood);
        this.setHardness(2);
        this.setBlockName(AppliedBureaucratics.MODID + ".break_room_counter");
        this.setCreativeTab(ABCreativeTab.INSTANCE);
    }

    @Override
    public TileEntity createNewTileEntity(final World world, final int meta) {
        return new TileBreakRoomCounter();
    }

    @Override
    public boolean onBlockActivated(final World world, final int x, final int y, final int z, final EntityPlayer player,
        final int side, final float hitX, final float hitY, final float hitZ) {
        if (player.isSneaking()) {
            return false;
        }
        if (!world.isRemote) {
            player.openGui(AppliedBureaucratics.instance, GuiHandler.BREAK_ROOM_COUNTER, world, x, y, z);
        }
        return true;
    }

    @Override
    public void breakBlock(final World world, final int x, final int y, final int z, final Block block,
        final int meta) {
        if (world.getTileEntity(x, y, z) instanceof TileBreakRoomCounter counter) {
            for (int i = 0; i < counter.getSizeInventory(); i++) {
                final ItemStack stack = counter.getStackInSlot(i);
                if (stack != null) {
                    this.dropBlockAsItem(world, x, y, z, stack);
                }
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(final IIconRegister register) {
        this.blockIcon = register.registerIcon(AppliedBureaucratics.MODID + ":break_room_counter_side");
        this.top = register.registerIcon(AppliedBureaucratics.MODID + ":break_room_counter_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(final int side, final int meta) {
        return side == 1 ? this.top : this.blockIcon;
    }
}
