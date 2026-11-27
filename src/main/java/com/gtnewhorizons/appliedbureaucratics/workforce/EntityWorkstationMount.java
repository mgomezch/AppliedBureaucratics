package com.gtnewhorizons.appliedbureaucratics.workforce;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.gtnewhorizons.appliedbureaucratics.integration.bibliocraft.Furniture;

/// The invisible seat a contracted mob rides at its workstation, placed on a BiblioCraft seat. Riding one is what
/// puts a worker on shift: see Note [Contracted mobs are held by mixins].
///
/// It keeps its rider turned towards its desk, and removes itself when it has had no rider for a second or when the
/// seat block under it is gone.
public class EntityWorkstationMount extends Entity {

    /// Where the rider's feet are, above the seat block's base.
    private static final double SEAT_HEIGHT = 0.35;

    private int seatX;
    private int seatY;
    private int seatZ;
    private float facing;

    public EntityWorkstationMount(final World world) {
        super(world);
        this.setSize(0.25F, 0.25F);
        this.noClip = true;
        this.isImmuneToFire = true;
    }

    public EntityWorkstationMount(final World world, final int x, final int y, final int z, final float facing) {
        this(world);
        this.seatX = x;
        this.seatY = y;
        this.seatZ = z;
        this.facing = facing;
        this.setLocationAndAngles(x + 0.5, y + SEAT_HEIGHT, z + 0.5, facing, 0);
    }

    public int getSeatX() {
        return this.seatX;
    }

    public int getSeatY() {
        return this.seatY;
    }

    public int getSeatZ() {
        return this.seatZ;
    }

    public boolean isAt(final int x, final int y, final int z) {
        return this.seatX == x && this.seatY == y && this.seatZ == z;
    }

    @Override
    protected void entityInit() {}

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.motionX = this.motionY = this.motionZ = 0;
        if (this.riddenByEntity instanceof EntityLivingBase rider) {
            rider.rotationYaw = rider.prevRotationYaw = this.facing;
            rider.rotationYawHead = rider.prevRotationYawHead = this.facing;
            rider.renderYawOffset = rider.prevRenderYawOffset = this.facing;
        }
        if (this.worldObj.isRemote) {
            return;
        }
        this.ticksExisted++;
        if (this.riddenByEntity == null && this.ticksExisted > 20) {
            this.setDead();
            return;
        }
        if (this.ticksExisted % 20 == 0
            && !Furniture.SEAT.matches(this.worldObj.getBlock(this.seatX, this.seatY, this.seatZ))) {
            if (this.riddenByEntity != null) {
                this.riddenByEntity.mountEntity(null);
            }
            this.setDead();
        }
    }

    @Override
    public double getMountedYOffset() {
        return 0;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean isEntityInvulnerable() {
        return true;
    }

    @Override
    protected void readEntityFromNBT(final NBTTagCompound tag) {
        this.seatX = tag.getInteger("seatX");
        this.seatY = tag.getInteger("seatY");
        this.seatZ = tag.getInteger("seatZ");
        this.facing = tag.getFloat("facing");
    }

    @Override
    protected void writeEntityToNBT(final NBTTagCompound tag) {
        tag.setInteger("seatX", this.seatX);
        tag.setInteger("seatY", this.seatY);
        tag.setInteger("seatZ", this.seatZ);
        tag.setFloat("facing", this.facing);
    }
}
