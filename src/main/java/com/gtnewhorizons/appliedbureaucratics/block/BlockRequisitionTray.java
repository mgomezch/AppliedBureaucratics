package com.gtnewhorizons.appliedbureaucratics.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.gtnewhorizons.appliedbureaucratics.AppliedBureaucratics;
import com.gtnewhorizons.appliedbureaucratics.facility.desk.TileRequisitionTray;
import com.gtnewhorizons.appliedbureaucratics.gui.GuiHandler;
import com.gtnewhorizons.appliedbureaucratics.item.ABCreativeTab;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The Requisition Tray, the heart of a Desk. See [TileRequisitionTray].
public class BlockRequisitionTray extends BlockContainer {

    private IIcon top;
    private IIcon bottom;

    public BlockRequisitionTray() {
        super(Material.iron);
        this.setHardness(3);
        this.setResistance(10);
        this.setBlockName(AppliedBureaucratics.MODID + ".requisition_tray");
        this.setCreativeTab(ABCreativeTab.INSTANCE);
        this.setHarvestLevel("wrench", 1);
    }

    @Override
    public TileEntity createNewTileEntity(final World world, final int meta) {
        return new TileRequisitionTray();
    }

    @Override
    public boolean onBlockActivated(final World world, final int x, final int y, final int z, final EntityPlayer player,
        final int side, final float hitX, final float hitY, final float hitZ) {
        if (player.isSneaking()) {
            return false;
        }
        if (!world.isRemote) {
            player.openGui(AppliedBureaucratics.instance, GuiHandler.REQUISITION_TRAY, world, x, y, z);
        }
        return true;
    }

    @Override
    public void breakBlock(final World world, final int x, final int y, final int z, final Block block,
        final int meta) {
        final TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileRequisitionTray tray) {
            tray.onBroken();
            for (int i = 0; i < tray.getSizeInventory(); i++) {
                final net.minecraft.item.ItemStack stack = tray.getStackInSlot(i);
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
        this.blockIcon = register.registerIcon(AppliedBureaucratics.MODID + ":requisition_tray_side");
        this.top = register.registerIcon(AppliedBureaucratics.MODID + ":requisition_tray_top");
        this.bottom = register.registerIcon(AppliedBureaucratics.MODID + ":requisition_tray_bottom");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(final int side, final int meta) {
        return side == 1 ? this.top : side == 0 ? this.bottom : this.blockIcon;
    }
}
