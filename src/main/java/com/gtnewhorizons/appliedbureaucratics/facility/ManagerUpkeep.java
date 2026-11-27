package com.gtnewhorizons.appliedbureaucratics.facility;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.workforce.ManagerKind;
import com.mojang.authlib.GameProfile;

/// What a facility's managers cost (design document, section 4.2): an ender pearl a day per Enderman, a gold ingot a
/// day per Crimson Cleric, a diamond a day per boss, and 100 LP a minute per demon from its owner's soul network. An
/// unpaid manager works at
/// efficiency 0.5.
public final class ManagerUpkeep {

    /// A manager, and who employs it.
    public record Upkept(ManagerKind kind, UUID employer) {}

    /// Takes up to `count` items matching a predicate, and says how many it took.
    public interface Taker {

        int take(Predicate<ItemStack> what, int count);
    }

    private static final int DAY = 24000;
    private static @Nullable Method syphon;
    private static boolean syphonLooked;

    private List<Upkept> managers = new ArrayList<>();
    private final Map<ManagerKind, Boolean> paid = new EnumMap<>(ManagerKind.class);
    private long ticks;

    public void setManagers(final List<Upkept> managers) {
        this.managers = new ArrayList<>(managers);
    }

    public boolean isPaid(final ManagerKind kind) {
        return this.paid.getOrDefault(kind, true);
    }

    /// Advances by `elapsed` ticks: demons are paid every call, the others once a day.
    public void tick(final long elapsed, final Taker taker) {
        this.ticks += elapsed;
        boolean demonsPaid = true;
        for (final Upkept manager : this.managers) {
            if (manager.kind() == ManagerKind.DEMON) {
                demonsPaid &= syphonLp(manager.employer(), (int) (100 * elapsed / 1200));
            }
        }
        this.paid.put(ManagerKind.DEMON, demonsPaid);
        if (this.ticks < DAY) {
            return;
        }
        this.ticks -= DAY;
        int endermen = 0;
        int clerics = 0;
        int bosses = 0;
        for (final Upkept manager : this.managers) {
            if (manager.kind() == ManagerKind.ENDERMAN) {
                endermen++;
            } else if (manager.kind() == ManagerKind.CLERIC) {
                clerics++;
            } else if (manager.kind() == ManagerKind.BOSS) {
                bosses++;
            }
        }
        this.paid.put(ManagerKind.ENDERMAN, taker.take(s -> s.getItem() == Items.ender_pearl, endermen) == endermen);
        this.paid.put(ManagerKind.CLERIC, taker.take(s -> s.getItem() == Items.gold_ingot, clerics) == clerics);
        this.paid.put(ManagerKind.BOSS, taker.take(s -> s.getItem() == Items.diamond, bosses) == bosses);
    }

    /// Takes `lp` from the soul network of `owner` through Blood Magic's API, found by name once. Without Blood Magic
    /// there are no demons, so this is never asked.
    private static boolean syphonLp(final UUID owner, final int lp) {
        if (!syphonLooked) {
            syphonLooked = true;
            try {
                syphon = Class.forName("WayofTime.alchemicalWizardry.api.soulNetwork.SoulNetworkHandler")
                    .getMethod("syphonFromNetwork", String.class, int.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                syphon = null;
            }
        }
        if (syphon == null) {
            return true;
        }
        final MinecraftServer server = MinecraftServer.getServer();
        final GameProfile profile = server == null ? null
            : server.func_152358_ax()
                .func_152652_a(owner);
        if (profile == null) {
            return false;
        }
        try {
            return syphon.invoke(null, profile.getName(), lp) instanceof Integer taken && taken >= lp;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    public void writeToNBT(final NBTTagCompound tag) {
        tag.setLong("upkeepTicks", this.ticks);
        for (final Map.Entry<ManagerKind, Boolean> e : this.paid.entrySet()) {
            tag.setBoolean(
                "upkeepPaid" + e.getKey()
                    .name(),
                e.getValue());
        }
    }

    public void readFromNBT(final NBTTagCompound tag) {
        this.ticks = tag.getLong("upkeepTicks");
        for (final ManagerKind kind : ManagerKind.values()) {
            if (tag.hasKey("upkeepPaid" + kind.name())) {
                this.paid.put(kind, tag.getBoolean("upkeepPaid" + kind.name()));
            }
        }
    }
}
