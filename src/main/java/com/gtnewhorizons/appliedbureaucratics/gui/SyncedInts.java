package com.gtnewhorizons.appliedbureaucratics.gui;

import java.util.List;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;

/// Ints a container sends its GUI. Vanilla sends progress bar values as shorts, so each int goes as two.
public final class SyncedInts {

    private final int[] server;
    private final int[] sent;
    private final int[] client;

    public SyncedInts(final int count) {
        this.server = new int[count];
        this.sent = new int[count];
        this.client = new int[count];
        java.util.Arrays.fill(this.sent, Integer.MIN_VALUE);
    }

    public void set(final int index, final int value) {
        this.server[index] = value;
    }

    public int get(final int index) {
        return this.client[index];
    }

    public void send(final Container container, final List<?> crafters, final boolean force) {
        for (int i = 0; i < this.server.length; i++) {
            if (force || this.server[i] != this.sent[i]) {
                this.sent[i] = this.server[i];
                for (final Object crafter : crafters) {
                    ((ICrafting) crafter).sendProgressBarUpdate(container, 2 * i, this.server[i] & 0xFFFF);
                    ((ICrafting) crafter).sendProgressBarUpdate(container, 2 * i + 1, this.server[i] >>> 16);
                }
            }
        }
    }

    public void receive(final int id, final int value) {
        final int i = id / 2;
        if (i < 0 || i >= this.client.length) {
            return;
        }
        if (id % 2 == 0) {
            this.client[i] = (this.client[i] & 0xFFFF0000) | (value & 0xFFFF);
        } else {
            this.client[i] = (this.client[i] & 0xFFFF) | ((value & 0xFFFF) << 16);
        }
    }
}
