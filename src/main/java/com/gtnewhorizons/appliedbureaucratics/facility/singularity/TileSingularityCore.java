package com.gtnewhorizons.appliedbureaucratics.facility.singularity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

/// The sphere the Singularity's controller places in the middle of its frame: render only. It carries what the
/// renderer shows, Alignment and load, synced to clients. See Note [The Singularity].
public class TileSingularityCore extends TileEntity {

    private float alignment = 50;
    private float load;

    public float getAlignment() {
        return this.alignment;
    }

    public float getLoad() {
        return this.load;
    }

    public void setState(final double alignment, final double load) {
        if (Math.abs(alignment - this.alignment) >= 1 || Math.abs(load - this.load) >= 0.05) {
            this.alignment = (float) alignment;
            this.load = (float) load;
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        }
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 256 * 256;
    }

    @Override
    public void writeToNBT(final NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setFloat("alignment", this.alignment);
        tag.setFloat("load", this.load);
    }

    @Override
    public void readFromNBT(final NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.alignment = tag.getFloat("alignment");
        this.load = tag.getFloat("load");
    }

    @Override
    public Packet getDescriptionPacket() {
        final NBTTagCompound tag = new NBTTagCompound();
        this.writeToNBT(tag);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(final NetworkManager net, final S35PacketUpdateTileEntity packet) {
        this.readFromNBT(packet.func_148857_g());
    }
}
