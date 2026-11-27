package com.gtnewhorizons.appliedbureaucratics.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import com.gtnewhorizons.appliedbureaucratics.AppliedBureaucratics;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// The mod's crafting components and media, one meta each.
public class ItemComponent extends Item {

    public enum Kind {

        TAPE_REEL(16384),
        HIGH_DENSITY_TAPE_REEL(65536),
        DISK_ARRAY(262144),
        PUNCH_CARD(0),
        BLANK_DATA_TAPE(0),
        WORK_LOG(0),
        ACCELERATOR_CARD(0),
        CONVERGED_MODEL_WEIGHTS(0),
        PAPERCLIP(0);

        /// The Filing this item adds where it is storage media, in bytes.
        public final long filing;

        Kind(final long filing) {
            this.filing = filing;
        }

        public String key() {
            return this.name()
                .toLowerCase();
        }

        public ItemStack stack(final int count) {
            return new ItemStack(ModItems.component, count, this.ordinal());
        }

        public boolean is(final ItemStack stack) {
            return stack != null && stack.getItem() == ModItems.component && stack.getItemDamage() == this.ordinal();
        }

        public static Kind of(final ItemStack stack) {
            if (stack == null || stack.getItem() != ModItems.component) {
                return null;
            }
            final Kind[] all = values();
            final int meta = stack.getItemDamage();
            return meta >= 0 && meta < all.length ? all[meta] : null;
        }
    }

    private final IIcon[] icons = new IIcon[Kind.values().length];

    public ItemComponent() {
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
        this.setUnlocalizedName(AppliedBureaucratics.MODID + ".component");
        this.setCreativeTab(ABCreativeTab.INSTANCE);
    }

    @Override
    public String getUnlocalizedName(final ItemStack stack) {
        final Kind kind = Kind.of(stack);
        return "item." + AppliedBureaucratics.MODID + "." + (kind != null ? kind.key() : "component");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(final IIconRegister register) {
        for (final Kind kind : Kind.values()) {
            this.icons[kind.ordinal()] = register.registerIcon(AppliedBureaucratics.MODID + ":" + kind.key());
        }
    }

    @Override
    public IIcon getIconFromDamage(final int meta) {
        return this.icons[Math.max(0, Math.min(this.icons.length - 1, meta))];
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(final Item item, final CreativeTabs tab, final List list) {
        for (final Kind kind : Kind.values()) {
            list.add(kind.stack(1));
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(final ItemStack stack, final EntityPlayer player, final List lines,
        final boolean advanced) {
        final String key = this.getUnlocalizedName(stack) + ".tooltip";
        if (StatCollector.canTranslate(key)) {
            for (final String line : StatCollector.translateToLocal(key)
                .split("\\\\n")) {
                lines.add("§7§o" + line);
            }
        }
        final Kind kind = Kind.of(stack);
        if (kind != null && kind.filing > 0) {
            lines.add("§bFiling: " + kind.filing / 1024 + " k");
        }
    }
}
