package com.gtnewhorizons.appliedbureaucratics.integration.oc;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.jspecify.annotations.Nullable;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;

/// A Docking Pad: an OpenComputers node on a Mainframe's terminal network, where a robot clocks in. See Note
/// [Robots].
public class TileDockingPad extends TileEntity implements Environment {

    private @Nullable Node node;
    private boolean joined;

    public TileDockingPad() {
        try {
            this.node = Network.newNode(this, Visibility.Network)
                .create();
        } catch (RuntimeException | LinkageError e) {
            this.node = null;
        }
    }

    @Override
    public void updateEntity() {
        if (!this.joined && this.worldObj != null && !this.worldObj.isRemote && this.node != null) {
            this.joined = true;
            Network.joinOrCreateNetwork(this);
        }
    }

    @Override
    public @Nullable Node node() {
        return this.node;
    }

    @Override
    public void onConnect(final Node node) {}

    @Override
    public void onDisconnect(final Node node) {}

    @Override
    public void onMessage(final Message message) {}

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (this.node != null) {
            this.node.remove();
        }
        this.joined = false;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (this.node != null) {
            this.node.remove();
        }
        this.joined = false;
    }

    @Override
    public void readFromNBT(final NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (this.node != null && this.node.host() == this) {
            this.node.load(tag.getCompoundTag("oc:node"));
        }
    }

    @Override
    public void writeToNBT(final NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (this.node != null && this.node.host() == this) {
            final NBTTagCompound nodeTag = new NBTTagCompound();
            this.node.save(nodeTag);
            tag.setTag("oc:node", nodeTag);
        }
    }
}
