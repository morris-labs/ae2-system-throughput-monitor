package dev.morrislabs.ae2throughput.screen;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.api.client.AEKeyRendering;

import dev.morrislabs.ae2throughput.menu.ThroughputMonitorMenu;
import dev.morrislabs.ae2throughput.network.ThroughputUpdatePayload;

/**
 * Client-side GUI for the throughput monitor part. Displays all item keys with non-zero
 * flow, color-coded green (produced) and red (consumed), sorted by total rate descending.
 */
public class ThroughputMonitorScreen extends AbstractContainerScreen<ThroughputMonitorMenu> {

    // Layout constants (all in pixels, relative to leftPos/topPos unless noted)
    private static final int IMAGE_W = 256;
    private static final int IMAGE_H = 220;
    private static final int ROW_H = 20;
    private static final int VISIBLE_ROWS = 8;
    private static final int LIST_X = 4;       // x offset for rows inside window
    private static final int LIST_Y = 32;      // y offset for first row
    private static final int LIST_W = 238;     // width available for rows
    private static final int SCROLL_W = 10;    // scrollbar track width

    private static final int COLOR_BG       = 0xFF1A1A2E;
    private static final int COLOR_BORDER   = 0xFF4466AA;
    private static final int COLOR_HEADER   = 0xFF222244;
    private static final int COLOR_ROW_ALT  = 0x18FFFFFF;
    private static final int COLOR_PRODUCED = 0xFF55FF55;
    private static final int COLOR_CONSUMED = 0xFFFF5555;
    private static final int COLOR_TEXT     = 0xFFE0E0E0;
    private static final int COLOR_DIM      = 0xFF888888;
    private static final int COLOR_SCROLL   = 0xFF556699;

    /** Which flow direction to show. */
    private enum Filter { ALL, PRODUCING, CONSUMING }

    private Filter filter = Filter.ALL;
    private int scrollOffset = 0;
    private List<ThroughputUpdatePayload.Entry> rawEntries = List.of();

    public ThroughputMonitorScreen(ThroughputMonitorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = IMAGE_W;
        this.imageHeight = IMAGE_H;
    }

    @Override
    protected void init() {
        super.init();
        int bx = leftPos + 6;
        int by = topPos + 16;
        addRenderableWidget(Button.builder(
                Component.translatable("gui.ae2throughputmonitor.filter.all"),
                b -> setFilter(Filter.ALL))
                .pos(bx, by).size(46, 12).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.ae2throughputmonitor.filter.producing"),
                b -> setFilter(Filter.PRODUCING))
                .pos(bx + 50, by).size(68, 12).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.ae2throughputmonitor.filter.consuming"),
                b -> setFilter(Filter.CONSUMING))
                .pos(bx + 122, by).size(68, 12).build());
    }

    private void setFilter(Filter f) {
        filter = f;
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partial) {
        renderBackground(gg, mouseX, mouseY, partial);
        renderWindow(gg);
        super.render(gg, mouseX, mouseY, partial);
        renderRows(gg, mouseX, mouseY);
        renderScrollbar(gg);
        renderTooltip(gg, mouseX, mouseY);
    }

    private void renderWindow(GuiGraphics gg) {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int h = imageHeight;

        gg.fill(x, y, x + w, y + h, COLOR_BG);
        // Header area
        gg.fill(x, y, x + w, y + LIST_Y - 2, COLOR_HEADER);
        // Header separator
        gg.fill(x, y + LIST_Y - 2, x + w, y + LIST_Y - 1, COLOR_BORDER);
        // Outer border
        gg.fill(x,         y,         x + w, y + 1,     COLOR_BORDER);
        gg.fill(x,         y + h - 1, x + w, y + h,     COLOR_BORDER);
        gg.fill(x,         y,         x + 1, y + h,     COLOR_BORDER);
        gg.fill(x + w - 1, y,         x + w, y + h,     COLOR_BORDER);

        // Title
        gg.drawCenteredString(font, title, x + w / 2, y + 4, 0xFFFFFF);
    }

    private void renderRows(GuiGraphics gg, int mouseX, int mouseY) {
        List<ThroughputUpdatePayload.Entry> visible = filteredEntries();
        int total = visible.size();
        int maxScroll = Math.max(0, total - VISIBLE_ROWS);
        scrollOffset = Math.min(scrollOffset, maxScroll);
        int start = scrollOffset;
        int end = Math.min(start + VISIBLE_ROWS, total);

        if (total == 0) {
            gg.drawCenteredString(font,
                    Component.translatable("gui.ae2throughputmonitor.no_flow"),
                    leftPos + imageWidth / 2,
                    topPos + LIST_Y + VISIBLE_ROWS * ROW_H / 2,
                    COLOR_DIM);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        for (int i = start; i < end; i++) {
            ThroughputUpdatePayload.Entry entry = visible.get(i);
            int rowY = topPos + LIST_Y + (i - start) * ROW_H;
            int rowX = leftPos + LIST_X;

            if ((i & 1) == 0) {
                gg.fill(rowX, rowY, rowX + LIST_W, rowY + ROW_H, COLOR_ROW_ALT);
            }

            // Item icon (16x16)
            AEKeyRendering.drawInGui(mc, gg, rowX + 2, rowY + 2, entry.key());

            // Item name (truncated)
            String name = font.plainSubstrByWidth(
                    entry.key().getDisplayName().getString(), 110);
            gg.drawString(font, name, rowX + 20, rowY + 6, COLOR_TEXT, false);

            // Rates, right-aligned in two half-height lines
            if (entry.produced() > 0) {
                String prod = "+" + formatRate(entry.produced()) + "/s";
                int tx = rowX + LIST_W - SCROLL_W - 4 - font.width(prod);
                gg.drawString(font, prod, tx, rowY + 3, COLOR_PRODUCED, false);
            }
            if (entry.consumed() > 0) {
                String cons = "-" + formatRate(entry.consumed()) + "/s";
                int tx = rowX + LIST_W - SCROLL_W - 4 - font.width(cons);
                gg.drawString(font, cons, tx, rowY + 12, COLOR_CONSUMED, false);
            }

            // Hover tooltip with full name
            if (mouseX >= rowX + 2 && mouseX < rowX + 18
                    && mouseY >= rowY + 2 && mouseY < rowY + 18) {
                setTooltipForNextRenderPass(entry.key().getDisplayName());
            }
        }
    }

    private void renderScrollbar(GuiGraphics gg) {
        List<ThroughputUpdatePayload.Entry> visible = filteredEntries();
        int total = visible.size();
        if (total <= VISIBLE_ROWS) return;

        int trackX = leftPos + LIST_X + LIST_W - SCROLL_W + 1;
        int trackY = topPos + LIST_Y;
        int trackH = VISIBLE_ROWS * ROW_H;

        gg.fill(trackX, trackY, trackX + SCROLL_W - 2, trackY + trackH, 0xFF222233);

        int thumbH = Math.max(12, trackH * VISIBLE_ROWS / total);
        int maxScroll = total - VISIBLE_ROWS;
        int thumbY = trackY + (trackH - thumbH) * scrollOffset / maxScroll;
        gg.fill(trackX + 1, thumbY, trackX + SCROLL_W - 3, thumbY + thumbH, COLOR_SCROLL);
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partial, int mouseX, int mouseY) {
        // Background drawn in renderWindow(); nothing additional needed here.
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        // Labels rendered in renderRows() with absolute coordinates; suppress defaults.
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, filteredEntries().size() - VISIBLE_ROWS);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - Math.signum(scrollY)));
        return true;
    }

    // --- Data feed from network ---

    /**
     * Called by the payload handler when the server sends a throughput snapshot.
     * Checks whether the current screen is a ThroughputMonitorScreen.
     */
    public static void handlePayload(ThroughputUpdatePayload payload) {
        if (Minecraft.getInstance().screen instanceof ThroughputMonitorScreen s) {
            s.receiveEntries(payload.entries());
        }
    }

    private void receiveEntries(List<ThroughputUpdatePayload.Entry> entries) {
        this.rawEntries = entries;
        int maxScroll = Math.max(0, filteredEntries().size() - VISIBLE_ROWS);
        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    // --- Helpers ---

    private List<ThroughputUpdatePayload.Entry> filteredEntries() {
        return rawEntries.stream()
                .filter(e -> switch (filter) {
                    case ALL -> true;
                    case PRODUCING -> e.produced() > 0;
                    case CONSUMING -> e.consumed() > 0;
                })
                .sorted(Comparator.comparingLong(
                        (ThroughputUpdatePayload.Entry e) -> e.produced() + e.consumed())
                        .reversed())
                .collect(Collectors.toList());
    }

    private static String formatRate(long rate) {
        if (rate >= 1_000_000_000L) return (rate / 1_000_000_000L) + "G";
        if (rate >= 1_000_000L)     return (rate / 1_000_000L) + "M";
        if (rate >= 1_000L)         return (rate / 1_000L) + "k";
        return Long.toString(rate);
    }
}
