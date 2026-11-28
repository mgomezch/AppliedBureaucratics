package com.gtnewhorizons.appliedbureaucratics.config;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameData;

/// A configured list of items, as registry names with an optional `:meta`, resolved once at load so matching is a
/// map lookup. A name with a `*` in it is a pattern, matched against every item's registry name, ignoring case.
///
/// Items are kept by identity, not by numeric ID: a world remaps mods' item IDs to its own when it loads, so an ID
/// taken at load would stop matching, as Pam's coffee beans once did in a test world.
public enum ItemList {

    /// A Coffee Machine's: beans, which every cup needs, and what may go in it. See Note [Break rooms].
    COFFEE_BEANS("harvestcraft:coffeebeanItem"),
    SUGAR("minecraft:sugar"),
    COCOA_POWDER("harvestcraft:cocoapowderItem"),
    SOY_MILK("harvestcraft:soymilkItem"),
    /// What a Snack Vending Machine sells.
    SANDWICHES("harvestcraft:*sandwich*", "harvestcraft:bltItem", "harvestcraft:grilledcheeseItem"),
    PARTY_FOOD("harvestcraft:pizzaItem", "minecraft:cake", "minecraft:pumpkin_pie"),
    PAPER("minecraft:paper"),
    BOOK("minecraft:book", "minecraft:writable_book", "minecraft:written_book", "minecraft:enchanted_book");

    private String[] names;
    /// The metadata that match, by item; [#ANY] matches every one.
    private Map<Item, Set<Integer>> keys = new IdentityHashMap<>();

    ItemList(final String... names) {
        this.names = names;
    }

    public String[] getNames() {
        return this.names.clone();
    }

    public void configure(final String[] names) {
        this.names = names.clone();
    }

    private static final int ANY = -1;

    public boolean matches(final ItemStack stack) {
        final Set<Integer> metas = this.keys.get(stack.getItem());
        return metas != null && (metas.contains(ANY) || metas.contains(stack.getItemDamage()));
    }

    public boolean matches(final Item item) {
        final Set<Integer> metas = this.keys.get(item);
        return metas != null && metas.contains(ANY);
    }

    public static void resolveAll() {
        for (final ItemList list : values()) {
            final Map<Item, Set<Integer>> keys = new IdentityHashMap<>();
            for (final String spec : list.names) {
                if (spec.contains("*")) {
                    final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "\\Q" + spec.replace("*", "\\E.*\\Q") + "\\E",
                        java.util.regex.Pattern.CASE_INSENSITIVE);
                    for (final Object name : GameData.getItemRegistry()
                        .getKeys()) {
                        if (pattern.matcher((String) name)
                            .matches()) {
                            add(
                                keys,
                                (Item) GameData.getItemRegistry()
                                    .getObject((String) name),
                                ANY);
                        }
                    }
                    continue;
                }
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
                    add(keys, item, meta);
                }
            }
            list.keys = keys;
        }
    }

    private static void add(final Map<Item, Set<Integer>> keys, final Item item, final int meta) {
        if (item != null) {
            keys.computeIfAbsent(item, i -> new HashSet<>())
                .add(meta);
        }
    }
}
