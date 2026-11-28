package com.gtnewhorizons.appliedbureaucratics.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.StatCollector;

import com.gtnewhorizons.appliedbureaucratics.facility.Bottleneck;
import com.gtnewhorizons.appliedbureaucratics.facility.desk.TileRequisitionTray;

/// The Requisition Tray's screen: the bottleneck first, then the sarcasm, then the figures.
public class GuiRequisitionTray extends GuiContainer {

    private final ContainerRequisitionTray container;

    public GuiRequisitionTray(final InventoryPlayer player, final TileRequisitionTray tray) {
        super(new ContainerRequisitionTray(player, tray));
        this.container = (ContainerRequisitionTray) this.inventorySlots;
        this.xSize = 206;
        this.ySize = 220;
    }

    /// Room for text inside the panel's margins.
    private static final int TEXT_WIDTH = 190;

    private int value(final int index) {
        return this.container.values.get(index);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(
            StatCollector.translateToLocal("tile.appliedbureaucratics.requisition_tray.name"),
            8,
            6,
            0x404040);
        final Bottleneck[] all = Bottleneck.values();
        final Bottleneck bottleneck = all[Math
            .max(0, Math.min(all.length - 1, this.value(ContainerRequisitionTray.BOTTLENECK)))];
        // The bottleneck may take two lines; the panel leaves room for them.
        this.fontRendererObj.drawSplitString(
            StatCollector.translateToLocal(bottleneck.key()),
            8,
            18,
            TEXT_WIDTH,
            bottleneck == Bottleneck.NONE ? 0x206020 : 0x902020);
        this.fontRendererObj
            .drawString("§o" + StatCollector.translateToLocal(bottleneck.key() + ".quip"), 8, 38, 0x606060);
        final String worker = switch (this.value(ContainerRequisitionTray.WORKER)) {
            case 1 -> "ab.gui.worker.villager";
            case 2 -> "ab.gui.worker.founder";
            default -> "ab.gui.worker.none";
        };
        this.fontRendererObj.drawString(StatCollector.translateToLocal(worker), 8, 50, 0x404040);
        final int flags = this.value(ContainerRequisitionTray.FLAGS);
        final StringBuilder perks = new StringBuilder();
        if ((flags & 1) != 0) {
            perks.append(StatCollector.translateToLocal("ab.gui.crunch"))
                .append("  ");
        }
        if ((flags & 2) != 0) {
            perks.append(StatCollector.translateToLocal("ab.gui.breakroomperk"))
                .append("  ");
        }
        if ((flags & 4) != 0) {
            perks.append(StatCollector.translateToLocal("ab.gui.party"));
        }
        this.fontRendererObj.drawString(perks.toString(), 8, 126, 0x804000);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocalFormatted(
                "ab.gui.planning",
                this.value(ContainerRequisitionTray.PLANNING_RATE_X10) / 10.0),
            8,
            64,
            0x404040);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocalFormatted(
                "ab.gui.dispatch",
                this.value(ContainerRequisitionTray.DISPATCH_RATE_X10) / 10.0),
            8,
            74,
            0x404040);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocalFormatted(
                "ab.gui.filing",
                this.value(ContainerRequisitionTray.FILING_USED),
                this.value(ContainerRequisitionTray.FILING)),
            8,
            84,
            0x404040);
        this.fontRendererObj.drawString(
            StatCollector.translateToLocalFormatted("ab.gui.morale", this.value(ContainerRequisitionTray.MORALE)),
            8,
            94,
            0x404040);
        this.fontRendererObj.drawString(StatCollector.translateToLocal("ab.gui.paper"), 30, 108, 0x404040);
        final String wages = StatCollector
            .translateToLocal(this.value(ContainerRequisitionTray.PAID) == 1 ? "ab.gui.paid" : "ab.gui.unpaid");
        // Right-aligned against the payroll slot.
        this.fontRendererObj.drawString(wages, 176 - this.fontRendererObj.getStringWidth(wages), 108, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(final float partialTicks, final int mouseX, final int mouseY) {
        final int x = this.guiLeft;
        final int y = this.guiTop;
        drawRect(x, y, x + this.xSize, y + this.ySize, 0xFF8B8B8B);
        drawRect(x + 1, y + 1, x + this.xSize - 1, y + this.ySize - 1, 0xFFC6C6C6);
        for (final Object o : this.inventorySlots.inventorySlots) {
            final net.minecraft.inventory.Slot slot = (net.minecraft.inventory.Slot) o;
            drawRect(
                x + slot.xDisplayPosition - 1,
                y + slot.yDisplayPosition - 1,
                x + slot.xDisplayPosition + 17,
                y + slot.yDisplayPosition + 17,
                0xFF373737);
            drawRect(
                x + slot.xDisplayPosition,
                y + slot.yDisplayPosition,
                x + slot.xDisplayPosition + 16,
                y + slot.yDisplayPosition + 16,
                0xFF8B8B8B);
        }
    }
}
