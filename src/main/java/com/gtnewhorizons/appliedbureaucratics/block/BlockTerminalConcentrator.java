package com.gtnewhorizons.appliedbureaucratics.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnewhorizons.appliedbureaucratics.AppliedBureaucratics;
import com.gtnewhorizons.appliedbureaucratics.facility.mainframe.TileTerminalConcentrator;
import com.gtnewhorizons.appliedbureaucratics.item.ABCreativeTab;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The Terminal Concentrator, part of a Mainframe's back face. See [TileTerminalConcentrator].
public class BlockTerminalConcentrator extends BlockContainer {

    public BlockTerminalConcentrator() {
        super(Material.iron);
        this.setHardness(5);
        this.setResistance(10);
        this.setBlockName(AppliedBureaucratics.MODID + ".terminal_concentrator");
        this.setCreativeTab(ABCreativeTab.INSTANCE);
    }

    @Override
    public TileEntity createNewTileEntity(final World world, final int meta) {
        return new TileTerminalConcentrator();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(final IIconRegister register) {
        this.blockIcon = register.registerIcon(AppliedBureaucratics.MODID + ":terminal_concentrator");
    }
}
