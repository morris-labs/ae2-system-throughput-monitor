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
        saveChanges();
    }

    private void doAdjustPeriod(Integer delta) {
        part.setSamplePeriodTicks(part.getSamplePeriodTicks() + delta);
        saveChanges();
    }

    private void doAdjustWindow(Integer delta) {
        part.getTracker().setWindowSize(part.getTracker().getWindowSize() + delta);
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
            var averages = part.getTracker().getAverages();
            int periodTicks = part.getSamplePeriodTicks();
            float toPerSecond = 20.0f / periodTicks;

            var entries = new ArrayList<ThroughputUpdatePayload.Entry>(averages.size());
            for (var e : averages.entrySet()) {
                long produced = Math.round(e.getValue().produced() * toPerSecond);
                long consumed = Math.round(e.getValue().consumed() * toPerSecond);
                entries.add(new ThroughputUpdatePayload.Entry(e.getKey(), produced, consumed));
            }

            PacketDistributor.sendToPlayer(serverPlayer, new ThroughputUpdatePayload(
                    entries,
                    part.getTimescale().ordinal(),
                    part.getTracker().getWindowSize(),
                    periodTicks));
        }
        super.broadcastChanges();
    }

    // --- Helpers ---

    private void saveChanges() {
        part.getHost().markForSave();
    }
}
