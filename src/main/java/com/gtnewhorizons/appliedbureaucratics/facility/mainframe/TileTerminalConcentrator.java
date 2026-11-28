package com.gtnewhorizons.appliedbureaucratics.facility.mainframe;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.AppliedBureaucratics;
import com.gtnewhorizons.appliedbureaucratics.integration.oc.TileDockingPad;
import com.gtnewhorizons.appliedbureaucratics.util.Cadence;

import li.cil.oc.api.Network;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;

/// The Terminal Concentrator: the Mainframe's link to its office. It is an OpenComputers network node; the screens on
/// its network are the office's terminals ([#getTerminals], rescanned every 100 ticks). While its Mainframe runs, it
/// keeps its network's power topped up, so screens light without OC power plumbing, and it writes to the screens
/// what the Mainframe tells it to show ([#show]).
public class TileTerminalConcentrator extends TileEntity implements Environment {

    /// A screen on the concentrator's network.
    public record Terminal(int x, int y, int z, int tier, TextBuffer buffer) {}

    private static final int MAX_TERMINALS = 32;
    private static @Nullable Method hostMethod;
    private static boolean hostMethodLooked;

    private @Nullable Node node;
    private @Nullable MTEMainframe owner;
    private boolean joined;
    private boolean powered;
    private int ticks;
    private final Cadence scanCadence = new Cadence(Cadence.SMALL);
    private List<Terminal> terminals = Collections.emptyList();
    private List<TileDockingPad> pads = Collections.emptyList();
    private List<String> screen = Collections.emptyList();
    private boolean screenChanged;

    public TileTerminalConcentrator() {
        try {
            this.node = Network.newNode(this, Visibility.Network)
                .withConnector(1000)
                .create();
        } catch (RuntimeException | LinkageError e) {
            this.node = null;
        }
    }

    /// The Mainframe whose structure holds this concentrator, set at each structure check.
    public @Nullable MTEMainframe getOwner() {
        return this.owner != null && this.owner.isFormed() ? this.owner : null;
    }

    public void setOwner(final @Nullable MTEMainframe owner) {
        this.owner = owner;
    }

    public List<Terminal> getTerminals() {
        return this.terminals;
    }

    /// The Docking Pads on this concentrator's network. See Note [Robots].
    public List<TileDockingPad> getDockingPads() {
        return this.pads;
    }

    /// Whether the Mainframe this belongs to is running, so the concentrator powers its network.
    public void setPowered(final boolean powered) {
        this.powered = powered;
    }

    /// The lines every terminal's screen shows from now on.
    public void show(final List<String> lines) {
        if (!lines.equals(this.screen)) {
            this.screen = new ArrayList<>(lines);
            this.screenChanged = true;
        }
    }

    @Override
    public void updateEntity() {
        if (this.worldObj == null || this.worldObj.isRemote || this.node == null) {
            return;
        }
        if (!this.joined) {
            this.joined = true;
            Network.joinOrCreateNetwork(this);
        }
        if (this.powered && this.node instanceof Connector connector) {
            connector.changeBuffer(connector.globalBufferSize() - connector.globalBuffer());
        }
        if (this.scanCadence.tick()) {
            this.terminals = this.scan();
            this.screenChanged = true;
        }
        if (this.ticks % 40 == 0 && this.screenChanged) {
            this.screenChanged = false;
            this.draw();
        }
        this.ticks++;
    }

    private List<Terminal> scan() {
        final List<Terminal> found = new ArrayList<>();
        final List<TileDockingPad> pads = new ArrayList<>();
        this.pads = pads;
        if (this.node == null || this.node.network() == null) {
            return found;
        }
        for (final Node other : this.node.network()
            .nodes()) {
            if (other.host() instanceof TileDockingPad pad) {
                pads.add(pad);
                continue;
            }
            if (found.size() >= MAX_TERMINALS) {
                break;
            }
            if (!(other.host() instanceof TextBuffer buffer)) {
                continue;
            }
            final EnvironmentHost host = hostOf(buffer);
            if (host instanceof TileEntity te && te.getWorldObj() == this.worldObj) {
                found.add(new Terminal(te.xCoord, te.yCoord, te.zCoord, tierOf(te), buffer));
            }
        }
        return found;
    }

    /// The tile a screen's text buffer belongs to. OpenComputers' TextBuffer component keeps its host in a public
    /// Scala `val`, which its API does not expose.
    private static @Nullable EnvironmentHost hostOf(final TextBuffer buffer) {
        if (!hostMethodLooked) {
            hostMethodLooked = true;
            try {
                hostMethod = buffer.getClass()
                    .getMethod("host");
            } catch (NoSuchMethodException e) {
                AppliedBureaucratics.LOG.warn("OpenComputers' TextBuffer has no host(); no terminals will be found");
            }
        }
        try {
            return hostMethod != null && hostMethod.invoke(buffer) instanceof EnvironmentHost host ? host : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /// A screen's tier, 1 to 3, from its block's registry name (`screen1` to `screen3`).
    private static int tierOf(final TileEntity te) {
        final String name = net.minecraft.block.Block.blockRegistry.getNameForObject(te.getBlockType());
        if (name != null && name.length() > 0) {
            final char last = name.charAt(name.length() - 1);
            if (last >= '1' && last <= '3') {
                return last - '0';
            }
        }
        return 1;
    }

    private void draw() {
        for (final Terminal terminal : this.terminals) {
            try {
                final TextBuffer buffer = terminal.buffer();
                final int width = buffer.getWidth();
                final int height = buffer.getHeight();
                buffer.fill(0, 0, width, height, ' ');
                for (int row = 0; row < this.screen.size() && row < height; row++) {
                    final String line = this.screen.get(row);
                    buffer.set(0, row, line.length() > width ? line.substring(0, width) : line, false);
                }
            } catch (RuntimeException e) {
                // A screen that went away mid-draw.
            }
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
