package com.gtnewhorizons.appliedbureaucratics.block;

import java.util.function.Supplier;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnewhorizons.appliedbureaucratics.AppliedBureaucratics;
import com.gtnewhorizons.appliedbureaucratics.item.ABCreativeTab;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// A plain block with a tile entity and one texture, named `appliedbureaucratics.<name>`.
public class BlockSimpleTile extends BlockContainer {

    private final String name;
    private final Supplier<TileEntity> tile;

    public BlockSimpleTile(final String name, final Material material, final Supplier<TileEntity> tile) {
        super(material);
        this.name = name;
        this.tile = tile;
        this.setHardness(5);
        this.setResistance(10);
        this.setBlockName(AppliedBureaucratics.MODID + "." + name);
        this.setCreativeTab(ABCreativeTab.INSTANCE);
    }

    @Override
    public TileEntity createNewTileEntity(final World world, final int meta) {
        return this.tile.get();
    }

    /// A tile that is an inventory opens a plain screen of its slots.
    @Override
    public boolean onBlockActivated(final World world, final int x, final int y, final int z,
        final net.minecraft.entity.player.EntityPlayer player, final int side, final float hitX, final float hitY,
        final float hitZ) {
        if (player.isSneaking() || !(world.getTileEntity(x, y, z) instanceof net.minecraft.inventory.IInventory)) {
            return false;
        }
        if (!world.isRemote) {
            player.openGui(
                AppliedBureaucratics.instance,
                com.gtnewhorizons.appliedbureaucratics.gui.GuiHandler.INVENTORY,
                world,
                x,
                y,
                z);
        }
        return true;
    }

    @Override
    public void breakBlock(final World world, final int x, final int y, final int z,
        final net.minecraft.block.Block block, final int meta) {
        if (world.getTileEntity(x, y, z) instanceof net.minecraft.inventory.IInventory inventory) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                final net.minecraft.item.ItemStack stack = inventory.getStackInSlot(i);
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
        this.blockIcon = register.registerIcon(AppliedBureaucratics.MODID + ":" + this.name);
    }
}
