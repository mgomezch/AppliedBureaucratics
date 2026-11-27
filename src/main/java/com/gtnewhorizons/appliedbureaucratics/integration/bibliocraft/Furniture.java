package com.gtnewhorizons.appliedbureaucratics.integration.bibliocraft;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import net.minecraft.block.Block;

import com.gtnewhorizons.appliedbureaucratics.AppliedBureaucratics;

import cpw.mods.fml.common.registry.GameData;

/// The furniture facilities are built from, by role. BiblioCraft is closed source, so blocks are recognised by
/// registry name and nothing of BiblioCraft is called. The names are config values ([#configure]), resolved once at
/// load ([#resolveAll]); matching a block is then an identity lookup.
public enum Furniture {

    SEAT("BiblioCraft:BiblioSeats"),
    DESK("BiblioCraft:BiblioDesk"),
    TYPEWRITER("BiblioCraft:BiblioTypewriter"),
    BELL("BiblioCraft:BiblioBell"),
    BOOKCASE("BiblioCraft:Bookcase", "minecraft:bookshelf"),
    CLOCK("BiblioCraft:BiblioClock"),
    LAMP("BiblioCraft:BiblioLamp", "BiblioCraft:BiblioIronLamp", "BiblioCraft:BiblioLantern",
        "BiblioCraft:BiblioIronLantern"),
    POSTER("BiblioCraft:BiblioFancySign"),
    PLANT("minecraft:flower_pot");

    private String[] names;
    private Set<Block> blocks = Collections.emptySet();

    Furniture(final String... names) {
        this.names = names;
    }

    public String[] getNames() {
        return this.names.clone();
    }

    public void configure(final String[] names) {
        this.names = names.clone();
    }

    public boolean matches(final Block block) {
        return this.blocks.contains(block);
    }

    public static void resolveAll() {
        for (final Furniture role : values()) {
            final Set<Block> found = Collections.newSetFromMap(new IdentityHashMap<>());
            for (final String name : role.names) {
                final Block block = GameData.getBlockRegistry()
                    .getObject(name);
                if (block != null && GameData.getBlockRegistry()
                    .getNameForObject(block)
                    .equals(name)) {
                    found.add(block);
                } else {
                    AppliedBureaucratics.LOG.info("Furniture {}: no block named {}", role, name);
                }
            }
            role.blocks = found;
        }
    }
}
