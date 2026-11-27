package com.gtnewhorizons.appliedbureaucratics.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StatCollector;

/// The screen of a [ContainerSimple]: a title, an optional line of text, and the slots.
public class GuiSimple extends GuiContainer {

    private final String title;
    private final String line;

    public GuiSimple(final InventoryPlayer player, final IInventory inventory, final String titleKey,
        final String lineKey) {
        super(new ContainerSimple(player, inventory));
        this.title = StatCollector.translateToLocal(titleKey);
        this.line = lineKey == null ? "" : StatCollector.translateToLocal(lineKey);
        this.xSize = 176;
        this.ySize = 133;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(this.title, 8, 6, 0x404040);
        this.fontRendererObj.drawString("§o" + this.line, 8, 40, 0x606060);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(final float partialTicks, final int mouseX, final int mouseY) {
        final int x = this.guiLeft;
        final int y = this.guiTop;
        drawRect(x, y, x + this.xSize, y + this.ySize, 0xFF8B8B8B);
        drawRect(x + 1, y + 1, x + this.xSize - 1, y + this.ySize - 1, 0xFFC6C6C6);
        for (final Object o : this.inventorySlots.inventorySlots) {
            final Slot slot = (Slot) o;
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
