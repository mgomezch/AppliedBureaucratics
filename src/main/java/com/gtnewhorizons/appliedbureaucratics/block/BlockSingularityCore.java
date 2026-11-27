package com.gtnewhorizons.appliedbureaucratics.block;

import java.util.Random;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.gtnewhorizons.appliedbureaucratics.AppliedBureaucratics;
import com.gtnewhorizons.appliedbureaucratics.facility.singularity.TileSingularityCore;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The Singularity's core: no item, no collision, no drops; placed and removed by its controller.
public class BlockSingularityCore extends BlockContainer {

    public BlockSingularityCore() {
        super(Material.portal);
        this.setBlockUnbreakable();
        this.setResistance(6000000);
        this.setLightLevel(0.5F);
        this.setBlockName(AppliedBureaucratics.MODID + ".singularity_core");
    }

    @Override
    public TileEntity createNewTileEntity(final World world, final int meta) {
        return new TileSingularityCore();
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(final World world, final int x, final int y, final int z) {
        return null;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public Item getItemDropped(final int meta, final Random random, final int fortune) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(final IIconRegister register) {
        this.blockIcon = register.registerIcon(AppliedBureaucratics.MODID + ":casing_containment");
    }
}
