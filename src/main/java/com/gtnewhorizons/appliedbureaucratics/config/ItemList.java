package com.gtnewhorizons.appliedbureaucratics.config;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameData;

/// A configured list of items, as registry names with an optional `:meta`, resolved once at load so matching is a
/// set lookup.
public enum ItemList {

    COFFEE("harvestcraft:coffeeItem", "harvestcraft:coffeeconlecheItem", "harvestcraft:espressoItem",
        "minecraft:dye:3"),
    PARTY_FOOD("harvestcraft:pizzaItem", "minecraft:cake", "minecraft:pumpkin_pie"),
    PAPER("minecraft:paper"),
    BOOK("minecraft:book", "minecraft:writable_book", "minecraft:written_book", "minecraft:enchanted_book");

    private String[] names;
    private Set<Long> keys = new HashSet<>();

    ItemList(final String... names) {
        this.names = names;
    }

    public String[] getNames() {
        return this.names.clone();
    }

    public void configure(final String[] names) {
        this.names = names.clone();
    }

    private static long key(final Item item, final int meta) {
        return ((long) Item.getIdFromItem(item) << 32) | (meta & 0xFFFFFFFFL);
    }

    private static final int ANY = -1;

    public boolean matches(final ItemStack stack) {
        return this.keys.contains(key(stack.getItem(), ANY))
            || this.keys.contains(key(stack.getItem(), stack.getItemDamage()));
    }

    public boolean matches(final Item item) {
        return this.keys.contains(key(item, ANY));
    }

    public static void resolveAll() {
        for (final ItemList list : values()) {
            final Set<Long> keys = new HashSet<>();
            for (final String spec : list.names) {
                final String[] parts = spec.split(":");
                final String name = parts.length >= 2 ? parts[0] + ":" + parts[1] : spec;
                int meta = ANY;
                if (parts.length >= 3) {
                    try {
                        meta = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException e) {
                        meta = ANY;
                    }
                }
                final Item item = (Item) GameData.getItemRegistry()
                    .getObject(name);
                if (item != null && name.equals(
                    GameData.getItemRegistry()
                        .getNameForObject(item))) {
                    keys.add(key(item, meta));
                }
            }
            list.keys = keys;
        }
    }
}
