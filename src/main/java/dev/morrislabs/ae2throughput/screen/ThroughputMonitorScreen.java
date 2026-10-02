package dev.morrislabs.ae2throughput.screen;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.api.client.AEKeyRendering;

import dev.morrislabs.ae2throughput.menu.ThroughputMonitorMenu;
import dev.morrislabs.ae2throughput.network.ThroughputUpdatePayload;
import dev.morrislabs.ae2throughput.part.Timescale;

/**
 * Client-side GUI for the throughput monitor part. Displays all keys with non-zero flow,
 * color-coded green (produced) and red (consumed), sorted by total rate descending.
 * A settings bar at the bottom controls timescale, sample period, and rolling window.
 */
public class ThroughputMonitorScreen extends AbstractContainerScreen<ThroughputMonitorMenu> {

    private static final int IMAGE_W      = 320;
    private static final int IMAGE_H      = 270;
    private static final int ROW_H        = 20;
    private static final int VISIBLE_ROWS = 9;
    private static final int LIST_X       = 6;
    // LIST_Y accounts for: title (y=4), filter buttons (y=15, h=12),
    // search bar (y=29, h=12), and a 2-px separator gap.
    private static final int LIST_Y       = 46;
    private static final int LIST_W       = 308;  // IMAGE_W - 2 * LIST_X
    private static final int SCROLL_W     = 10;
    private static final int SETTINGS_Y   = LIST_Y + VISIBLE_ROWS * ROW_H + 4;  // 230

    // X offsets (from leftPos) for each settings section.
    private static final int S1_X = 4;    // Timescale:    4 – 107
    private static final int S2_X = 112;  // Window size: 112 – 213
    private static final int S3_X = 220;  // Period:      220 – 315

    private static final int COLOR_BG       = 0xFF1A1A2E;
    private static final int COLOR_BORDER   = 0xFF4466AA;
    private static final int COLOR_HEADER   = 0xFF222244;
    private static final int COLOR_ROW_ALT  = 0x18FFFFFF;
    private static final int COLOR_PRODUCED = 0xFF55FF55;
    private static final int COLOR_CONSUMED = 0xFFFF5555;
    private static final int COLOR_TEXT     = 0xFFE0E0E0;
    private static final int COLOR_DIM      = 0xFF888888;
    private static final int COLOR_SCROLL   = 0xFF556699;

    private enum Filter { ALL, PRODUCING, CONSUMING }

    private Filter filter      = Filter.ALL;
    private int scrollOffset   = 0;
    private Timescale timescale = Timescale.PER_SECOND;
    private int windowSize     = 10;
    private int samplePeriodTicks = 20;
    private List<ThroughputUpdatePayload.Entry> rawEntries = List.of();
    private List<ThroughputUpdatePayload.Entry> filteredCache = List.of();

    // Widgets created in init
    private Button btnTimescale;
    private EditBox searchBox;

    public ThroughputMonitorScreen(ThroughputMonitorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = IMAGE_W;
        this.imageHeight = IMAGE_H;
    }

    @Override
    protected void init() {
        super.init();

        // Filter buttons (y=15, height=12)
        int bx = leftPos + LIST_X;
        int by = topPos + 15;
        addRenderableWidget(Button.builder(
                Component.translatable("gui.ae2throughputmonitor.filter.all"),
                b -> setFilter(Filter.ALL))
                .pos(bx, by).size(52, 12).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.ae2throughputmonitor.filter.producing"),
                b -> setFilter(Filter.PRODUCING))
                .pos(bx + 56, by).size(88, 12).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.ae2throughputmonitor.filter.consuming"),
                b -> setFilter(Filter.CONSUMING))
                .pos(bx + 148, by).size(88, 12).build());

        // Search bar (y=29, height=12, spans list width)
        searchBox = new EditBox(font,
                leftPos + LIST_X + 1, topPos + 29,
                LIST_W - 2, 12,
                Component.translatable("gui.ae2throughputmonitor.search"));
        searchBox.setHint(Component.translatable("gui.ae2throughputmonitor.search_hint"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(text -> {
            scrollOffset = 0;
            rebuildFilteredCache();
        });
        addRenderableWidget(searchBox);

        // Settings bar: two rows — label row (SETTINGS_Y+2) and control row (SETTINGS_Y+14)
        int sy = topPos + SETTINGS_Y;

        // S1: Timescale button centered in [4, 107]
        btnTimescale = addRenderableWidget(Button.builder(
                Component.literal(timescale.suffix),
                b -> menu.cycleTimescale())
                .pos(leftPos + 26, sy + 14).size(60, 16).build());

        // S2: Window size [-] value [+] in [112, 213]
        addRenderableWidget(Button.builder(Component.literal("-"),
                b -> menu.adjustWindow(-1))
                .pos(leftPos + S2_X, sy + 14).size(18, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"),
                b -> menu.adjustWindow(+1))
                .pos(leftPos + S2_X + 72, sy + 14).size(18, 16).build());

        // S3: Sample period [-] value [+] in [220, 315]
        addRenderableWidget(Button.builder(Component.literal("-"),
                b -> menu.adjustPeriod(-1))
                .pos(leftPos + S3_X, sy + 14).size(18, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"),
                b -> menu.adjustPeriod(+1))
                .pos(leftPos + S3_X + 76, sy + 14).size(18, 16).build());
    }

    private void setFilter(Filter f) {
        filter = f;
        scrollOffset = 0;
        rebuildFilteredCache();
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partial) {
        renderBackground(gg, mouseX, mouseY, partial);
        renderWindow(gg);
        super.render(gg, mouseX, mouseY, partial);
        renderRows(gg, mouseX, mouseY);
        renderScrollbar(gg);
        renderSettingsBar(gg);
        renderTooltip(gg, mouseX, mouseY);
    }

    private void renderWindow(GuiGraphics gg) {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int h = imageHeight;

        gg.fill(x, y, x + w, y + h, COLOR_BG);
        gg.fill(x, y, x + w, y + LIST_Y - 2, COLOR_HEADER);
        gg.fill(x, y + LIST_Y - 2, x + w, y + LIST_Y - 1, COLOR_BORDER);
        gg.fill(x, y + SETTINGS_Y - 2, x + w, y + SETTINGS_Y - 1, COLOR_BORDER);
        gg.fill(x, y,         x + w, y + 1,     COLOR_BORDER);
        gg.fill(x, y + h - 1, x + w, y + h,     COLOR_BORDER);
        gg.fill(x, y,         x + 1, y + h,     COLOR_BORDER);
        gg.fill(x + w - 1, y, x + w, y + h,     COLOR_BORDER);

        gg.drawCenteredString(font, title, x + w / 2, y + 4, 0xFFFFFF);
    }

    private void renderRows(GuiGraphics gg, int mouseX, int mouseY) {
        List<ThroughputUpdatePayload.Entry> visible = filteredCache;
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

            AEKeyRendering.drawInGui(mc, gg, rowX + 2, rowY + 2, entry.key());

            String name = font.plainSubstrByWidth(
                    entry.key().getDisplayName().getString(), 100);
            gg.drawString(font, name, rowX + 20, rowY + 6, COLOR_TEXT, false);

            if (entry.produced() > 0) {
                String prod = "+" + formatRate(entry.produced(), timescale) + timescale.suffix;
                int tx = rowX + LIST_W - SCROLL_W - 4 - font.width(prod);
                gg.drawString(font, prod, tx, rowY + 3, COLOR_PRODUCED, false);
            }
            if (entry.consumed() > 0) {
                String cons = "-" + formatRate(entry.consumed(), timescale) + timescale.suffix;
                int tx = rowX + LIST_W - SCROLL_W - 4 - font.width(cons);
                gg.drawString(font, cons, tx, rowY + 12, COLOR_CONSUMED, false);
            }

            if (mouseX >= rowX + 2 && mouseX < rowX + 18
                    && mouseY >= rowY + 2 && mouseY < rowY + 18) {
                setTooltipForNextRenderPass(entry.key().getDisplayName());
            }
        }
    }

    private void renderScrollbar(GuiGraphics gg) {
        List<ThroughputUpdatePayload.Entry> visible = filteredCache;
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

    private void renderSettingsBar(GuiGraphics gg) {
        int sy = topPos + SETTINGS_Y;
        int lx = leftPos;

        btnTimescale.setMessage(Component.literal(timescale.suffix));

        // Section dividers
        gg.fill(lx + S2_X - 2, sy, lx + S2_X - 1, sy + 32, COLOR_BORDER);
        gg.fill(lx + S3_X - 2, sy, lx + S3_X - 1, sy + 32, COLOR_BORDER);

        // S1: timescale button + computed window-period label
        int totalTicks = windowSize * samplePeriodTicks;
        gg.drawCenteredString(font, formatWindowDuration(totalTicks), lx + 56, sy + 4, COLOR_DIM);

        // S2: "Window size" label + value
        gg.drawCenteredString(font, "Window size", lx + S2_X + 45, sy + 4, COLOR_DIM);
        gg.drawCenteredString(font, String.valueOf(windowSize), lx + S2_X + 45, sy + 19, COLOR_TEXT);

        // S3: "Sample period" label + value
        gg.drawCenteredString(font, "Sample period", lx + S3_X + 47, sy + 4, COLOR_DIM);
        gg.drawCenteredString(font, samplePeriodTicks + "t", lx + S3_X + 47, sy + 19, COLOR_TEXT);
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partial, int mouseX, int mouseY) {}

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, filteredCache.size() - VISIBLE_ROWS);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - Math.signum(scrollY)));
        return true;
    }

    // --- Data feed ---

    public static void handlePayload(ThroughputUpdatePayload payload) {
        if (Minecraft.getInstance().screen instanceof ThroughputMonitorScreen s) {
            s.receivePayload(payload);
        }
    }

    private void receivePayload(ThroughputUpdatePayload payload) {
        this.rawEntries = payload.entries();
        this.timescale = Timescale.fromOrdinal(payload.timescaleOrdinal());
        this.windowSize = payload.windowSize();
        this.samplePeriodTicks = payload.samplePeriodTicks();
        rebuildFilteredCache();
        int maxScroll = Math.max(0, filteredCache.size() - VISIBLE_ROWS);
        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    // --- Helpers ---

    private void rebuildFilteredCache() {
        String query = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT) : "";
        filteredCache = rawEntries.stream()
                .filter(e -> switch (filter) {
                    case ALL -> true;
                    case PRODUCING -> e.produced() > 0;
                    case CONSUMING -> e.consumed() > 0;
                })
                .filter(e -> query.isEmpty()
                        || e.key().getDisplayName().getString().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparingLong(
                        (ThroughputUpdatePayload.Entry e) -> e.produced() + e.consumed())
                        .reversed())
                .toList();
    }

    /** Dispatches to the per-second or per-tick formatter based on the active timescale. */
    private static String formatRate(long tenths, Timescale ts) {
        return ts == Timescale.PER_TICK ? formatRateTick(tenths) : formatRatePerSecond(tenths);
    }

    /**
     * Formats a rate in tenths-of-items/sec as an items/sec string with one decimal place
     * below 1 k/s; uses k/M/G suffixes above.
     */
    private static String formatRatePerSecond(long tenths) {
        if (tenths >= 10_000_000_000L) return (tenths / 10_000_000_000L) + "G";
        if (tenths >= 10_000_000L)     return (tenths / 10_000_000L) + "M";
        if (tenths >= 10_000L)         return (tenths / 10_000L) + "k";
        return (tenths / 10) + "." + (tenths % 10);
    }

    /**
     * Formats a rate in tenths-of-items/sec as an items/tick string.
     * Uses thousandths internally: thousandths/tick = tenths/sec × 5
     * (because 1 item/tick = 20 items/sec = 200 tenths/sec → 1000 thousandths/tick).
     */
    private static String formatRateTick(long tenths) {
        long thou = tenths * 5L; // thousandths of items per tick
        if (thou >= 1_000_000_000L) return (thou / 1_000_000_000L) + "G";
        if (thou >= 1_000_000L)     return (thou / 1_000_000L) + "M";
        if (thou >= 1_000_000L)     return (thou / 1_000_000L) + "M";
        if (thou >= 1_000L)         return (thou / 1_000L) + "." + ((thou % 1_000L) / 100L);
        // Below 1/t: show three decimal places (e.g. 0.075)
        return "0." + String.format("%03d", thou);
    }

    /**
     * Formats a window duration expressed in ticks as "last Xm Ys (Zt)" for the settings bar.
     */
    private static String formatWindowDuration(int ticks) {
        int totalSec = ticks / 20;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        String time = min > 0
                ? (sec > 0 ? min + "m " + sec + "s" : min + "m")
                : totalSec + "s";
        return "last " + time + " (" + ticks + "t)";
    }
}

