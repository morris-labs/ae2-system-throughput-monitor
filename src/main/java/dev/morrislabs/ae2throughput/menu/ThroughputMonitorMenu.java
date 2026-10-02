package dev.morrislabs.ae2throughput.menu;

import java.util.ArrayList;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.PacketDistributor;

import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;

import dev.morrislabs.ae2throughput.Ae2ThroughputMod;
import dev.morrislabs.ae2throughput.network.ThroughputUpdatePayload;
import dev.morrislabs.ae2throughput.part.ThroughputMonitorPart;

/**
 * Server-side container for the throughput monitor GUI. Broadcasts per-key flow rates
 * and part settings to the client each tick via {@link ThroughputUpdatePayload}.
 */
public class ThroughputMonitorMenu extends AEBaseMenu {

    public static final MenuType<ThroughputMonitorMenu> TYPE = MenuTypeBuilder
            .create(ThroughputMonitorMenu::new, ThroughputMonitorPart.class)
            .build(Ae2ThroughputMod.makeId("throughput_monitor"));

    private static final String ACTION_CYCLE_TIMESCALE = "cycle_timescale";
    private static final String ACTION_ADJUST_PERIOD   = "adjust_period";
    private static final String ACTION_ADJUST_WINDOW   = "adjust_window";

    private final ThroughputMonitorPart part;
    /** Becomes {@code true} whenever a player action changes a setting; triggers immediate resend. */
    private boolean settingsDirty = true;

    public ThroughputMonitorMenu(int containerId, Inventory playerInventory, ThroughputMonitorPart part) {
        super(TYPE, containerId, playerInventory, part);
        this.part = part;

        registerClientAction(ACTION_CYCLE_TIMESCALE, this::doCycleTimescale);
        registerClientAction(ACTION_ADJUST_PERIOD, Integer.class, this::doAdjustPeriod);
        registerClientAction(ACTION_ADJUST_WINDOW, Integer.class, this::doAdjustWindow);
    }

    // --- Server-side action handlers ---

    private void doCycleTimescale() {
        part.cycleTimescale();
        settingsDirty = true;
        saveChanges();
    }

    private void doAdjustPeriod(Integer delta) {
        part.setSamplePeriodTicks(part.getSamplePeriodTicks() + delta);
        settingsDirty = true;
        saveChanges();
    }

    private void doAdjustWindow(Integer delta) {
        part.getTracker().setWindowSize(part.getTracker().getWindowSize() + delta);
        settingsDirty = true;
        saveChanges();
    }

    // --- Client-side action senders (called from the screen) ---

    public void cycleTimescale() {
        if (isClientSide()) {
            sendClientAction(ACTION_CYCLE_TIMESCALE);
            return;
        }
        doCycleTimescale();
    }

    public void adjustPeriod(int delta) {
        if (isClientSide()) {
            sendClientAction(ACTION_ADJUST_PERIOD, delta);
            return;
        }
        doAdjustPeriod(delta);
    }

    public void adjustWindow(int delta) {
        if (isClientSide()) {
            sendClientAction(ACTION_ADJUST_WINDOW, delta);
            return;
        }
        doAdjustWindow(delta);
    }

    // --- Broadcast ---

    @Override
    public void broadcastChanges() {
        if (isServerSide() && getPlayer() instanceof ServerPlayer serverPlayer) {
            var tracker = part.getTracker();
            boolean trackerChanged = tracker.isDirtyAndClear();
            if (trackerChanged || settingsDirty) {
                settingsDirty = false;
                int periodTicks = part.getSamplePeriodTicks();
                int windowSize  = tracker.getWindowSize();
                // Scale factor: tenths-of-items per second, preserving fractional flow.
                // totalProduced is the raw window sum; divide by windowSize*periodTicks/20
                // to get items/sec, then multiply by 10 for tenths precision.
                double toTenthsPerSec = 200.0 / ((double) windowSize * periodTicks);
                var averages = tracker.getAverages();

                var entries = new ArrayList<ThroughputUpdatePayload.Entry>(averages.size());
                for (var e : averages.entrySet()) {
                    long produced = Math.round(e.getValue().totalProduced() * toTenthsPerSec);
                    long consumed = Math.round(e.getValue().totalConsumed() * toTenthsPerSec);
                    entries.add(new ThroughputUpdatePayload.Entry(e.getKey(), produced, consumed));
                }

                PacketDistributor.sendToPlayer(serverPlayer, new ThroughputUpdatePayload(
                        entries,
                        part.getTimescale().ordinal(),
                        windowSize,
                        periodTicks));
            }
        }
        super.broadcastChanges();
    }

    /**
     * Ensures this class is loaded and {@link #TYPE} is queued with AE2's menu registry.
     * Call this during mod initialization before AE2's {@code InitMenuTypes} fires.
     */
    public static void ensureRegistered() {
        // Referencing TYPE here is intentional: it forces the static initializer which calls
        // MenuTypeBuilder.build(), queuing the type registration with AE2.
        var ignored = TYPE;
    }

    public ThroughputMonitorPart getPart() {
        return part;
    }

    // --- Helpers ---

    private void saveChanges() {
        part.getHost().markForSave();
    }
}
