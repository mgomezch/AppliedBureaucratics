package com.gtnewhorizons.appliedbureaucratics.block;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import com.gtnewhorizons.appliedbureaucratics.AppliedBureaucratics;
import com.gtnewhorizons.appliedbureaucratics.item.ABCreativeTab;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.util.GTLanguageManager;
import gregtech.common.blocks.BlockCasingsAbstract;

/// The casings of every multiblock facility, one meta each. Their GregTech casing textures are page
/// [#TEXTURE_PAGE], which no other GTNH mod uses, at the casing's meta.
public class BlockABCasing extends BlockCasingsAbstract {

    public static final int TEXTURE_PAGE = 17;

    public enum Casing {

        MAINFRAME("Mainframe Casing"),
        TAPE_WINDOW("Tape Reel Window"),
        TUBE("Tube Casing"),
        BACKBONE("Backbone Core"),
        DATA_HALL("Data Hall Casing"),
        RAISED_FLOOR("Raised Floor Casing"),
        CONTAINMENT("Containment Pylon"),
        DISH("Satellite Dish");

        public final String displayName;

        Casing(final String displayName) {
            this.displayName = displayName;
        }

        public int meta() {
            return this.ordinal();
        }

        public int textureIndex() {
            return (TEXTURE_PAGE << 7) | this.ordinal();
        }
    }

    private final IIcon[] icons = new IIcon[Casing.values().length];

    public BlockABCasing() {
        super(ItemABCasing.class, "appliedbureaucratics.casing", Material.iron, Casing.values().length);
        this.setHardness(5);
        this.setResistance(10);
        this.setCreativeTab(ABCreativeTab.INSTANCE);
        for (final Casing casing : Casing.values()) {
            GTLanguageManager
                .addStringLocalization(this.getUnlocalizedName() + "." + casing.meta() + ".name", casing.displayName);
        }
    }

    @Override
    public int getTextureIndex(final int meta) {
        return (TEXTURE_PAGE << 7) | Math.max(0, Math.min(Casing.values().length - 1, meta));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(final IIconRegister register) {
        for (final Casing casing : Casing.values()) {
            this.icons[casing.ordinal()] = register.registerIcon(
                AppliedBureaucratics.MODID + ":casing_"
                    + casing.name()
                        .toLowerCase());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(final int side, final int meta) {
        final IIcon icon = this.icons[Math.max(0, Math.min(this.icons.length - 1, meta))];
        return icon != null ? icon : net.minecraft.init.Blocks.iron_block.getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(final Item item, final CreativeTabs tab, final List list) {
        for (final Casing casing : Casing.values()) {
            list.add(new ItemStack(item, 1, casing.meta()));
        }
    }

    @Override
    public int damageDropped(final int meta) {
        return meta;
    }
}
