package com.gtnewhorizons.appliedbureaucratics.workforce;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;

import org.jspecify.annotations.Nullable;

/*-
Note [Contracted mobs are held by mixins] (generated)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
A contracted mob (a villager clerk, an Enderman team lead, a cleric) must sit at its workstation and do nothing
else, and must go back to normal when released.  We do not edit its AI task lists for that: some mobs never read
them (the Enderman and Blood Magic's demons use the legacy AI path, updateEntityActionState), other mods add or swap
tasks behind our back, and a stripped list would have to be saved and restored exactly.

Instead, one check, Employment.isOnShift, gates the mob's own behaviour at a few call sites:

  MixinEntityLivingBase_Shift   EntityLivingBase.onLivingUpdate: its calls to updateAITasks() and
                                updateEntityActionState() are skipped while on shift.  Both are virtual, so
                                this covers every mob and every override, including the villager's village and
                                home logic (updateAITasks calls updateAITick).  Movement and physics still run,
                                so the worker stays on its mount.  A worker on a break (Employment.isOnBreak)
                                runs Breaks.drive in updateAITasks' place, which walks it to its break room and
                                back: see Note [Break rooms].
  MixinEntityEnderman_Shift     EntityEnderman's teleports return false while on shift, and it does not pick up
                                blocks: a manager does not rearrange the office.

A mob is on shift while it is employed and riding an EntityWorkstationMount, the invisible seat we put on a
BiblioCraft seat.  Releasing a worker (a Pink Slip) clears its Employment and dismounts it; nothing else needs undoing.

Employment lives in a field added to EntityLivingBase (IEmployable), loaded lazily from the entity's Forge data
(Entity.getEntityData(), tag TAG) and written back there whenever it changes, so it is saved with the entity.

Things that are not mixins: mobs at work, on shift or on a break (Employment.isAtWork), take no damage
(WorkforceEvents.onAttack, LivingAttackEvent); right-clicking a worker shows its employee card instead of trading
(WorkforceEvents.onInteract, EntityInteractEvent, which fires before EntityVillager.interact); the mount turns its
rider to face its workstation every tick (see Note [Facing the workstation]).
*/

/// A contracted mob's employment. See Note [Contracted mobs are held by mixins].
public final class Employment {

    /// The tag in the entity's Forge data.
    public static final String TAG = "AppliedBureaucratics";

    public enum Role {
        WORKER,
        MANAGER
    }

    private final UUID employer;
    private final Role role;
    private final String name;
    private final String title;
    private final long hiredAt;
    private final Shift shift = new Shift();

    public Employment(final UUID employer, final Role role, final String name, final String title, final long hiredAt) {
        this.employer = employer;
        this.role = role;
        this.name = name;
        this.title = title;
        this.hiredAt = hiredAt;
    }

    public UUID getEmployer() {
        return this.employer;
    }

    public Role getRole() {
        return this.role;
    }

    public String getName() {
        return this.name;
    }

    public String getTitle() {
        return this.title;
    }

    public long getHiredAt() {
        return this.hiredAt;
    }

    /// The worker's break room, breaks and management. See Note [Break rooms].
    public Shift shift() {
        return this.shift;
    }

    public static @Nullable Employment of(final Entity entity) {
        return entity instanceof IEmployable employable ? employable.ab$getEmployment() : null;
    }

    /// Whether `entity` is employed and seated at a workstation, so its own behaviour is suspended.
    public static boolean isOnShift(final Entity entity) {
        return entity.ridingEntity instanceof EntityWorkstationMount && of(entity) != null;
    }

    /// Whether `entity` is on a break, walked by [Breaks] instead of its own AI. See Note [Break rooms].
    public static boolean isOnBreak(final Entity entity) {
        // Riding anything but its own mount, such as a BiblioCraft seat it walked onto, it is still on its break.
        if (entity.ridingEntity instanceof EntityWorkstationMount) {
            return false;
        }
        final Employment employment = of(entity);
        return employment != null && employment.shift.onBreak != null;
    }

    /// On shift or on a break: at work, so its own behaviour stays suspended and it takes no damage.
    public static boolean isAtWork(final Entity entity) {
        return isOnShift(entity) || isOnBreak(entity);
    }

    public static void set(final EntityLivingBase entity, final @Nullable Employment employment) {
        ((IEmployable) entity).ab$setEmployment(employment);
    }

    /// Reads the employment saved in `entity`'s Forge data, if any.
    public static @Nullable Employment load(final Entity entity) {
        final NBTTagCompound data = entity.getEntityData();
        if (!data.hasKey(TAG)) {
            return null;
        }
        final NBTTagCompound tag = data.getCompoundTag(TAG);
        try {
            final Employment employment = new Employment(
                new UUID(tag.getLong("employerHi"), tag.getLong("employerLo")),
                Role.values()[Math.min(Role.values().length - 1, tag.getByte("role"))],
                tag.getString("name"),
                tag.getString("title"),
                tag.getLong("hiredAt"));
            if (tag.hasKey("break")) {
                employment.shift.onBreak = Shift.Break.load(tag.getCompoundTag("break"));
            }
            return employment;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static void save(final Entity entity, final @Nullable Employment employment) {
        final NBTTagCompound data = entity.getEntityData();
        if (employment == null) {
            data.removeTag(TAG);
            return;
        }
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("employerHi", employment.employer.getMostSignificantBits());
        tag.setLong("employerLo", employment.employer.getLeastSignificantBits());
        tag.setByte("role", (byte) employment.role.ordinal());
        tag.setString("name", employment.name);
        tag.setString("title", employment.title);
        tag.setLong("hiredAt", employment.hiredAt);
        if (employment.shift.onBreak != null) {
            tag.setTag("break", employment.shift.onBreak.save());
        }
        data.setTag(TAG, tag);
    }
}
