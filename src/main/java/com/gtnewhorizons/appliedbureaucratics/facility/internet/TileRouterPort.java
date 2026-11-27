package com.gtnewhorizons.appliedbureaucratics.facility.internet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.facility.mainframe.MTEMainframe;
import com.gtnewhorizons.appliedbureaucratics.facility.mainframe.TileTerminalConcentrator;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;

/// A Router Port: part of the Internet's wall, and an OpenComputers node of its own, so each port's network stays
/// separate. A port whose network holds exactly one Terminal Concentrator links that concentrator's Mainframe to the
/// Internet. See Note [The Internet].
public class TileRouterPort extends TileEntity implements Environment {

    private @Nullable Node node;
    private boolean joined;

    public TileRouterPort() {
        try {
            this.node = Network.newNode(this, Visibility.Network)
                .create();
        } catch (RuntimeException | LinkageError e) {
            this.node = null;
        }
    }

    /// The Mainframe this port links, if its network holds exactly one Terminal Concentrator and that has a formed
    /// Mainframe.
    public @Nullable MTEMainframe linkedMainframe() {
        if (this.node == null || this.node.network() == null) {
            return null;
        }
        TileTerminalConcentrator found = null;
        for (final Node other : this.node.network()
            .nodes()) {
            if (other.host() instanceof TileTerminalConcentrator concentrator) {
                if (found != null && found != concentrator) {
                    return null;
                }
                found = concentrator;
            }
        }
        return found != null ? found.getOwner() : null;
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
