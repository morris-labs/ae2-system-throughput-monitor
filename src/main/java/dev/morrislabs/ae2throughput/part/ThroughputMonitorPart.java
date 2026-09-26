package dev.morrislabs.ae2throughput.part;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEKey;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;

import dev.morrislabs.ae2throughput.menu.ThroughputMonitorMenu;

/**
 * Cable-face part that tracks every item flowing through the attached AE2 network
 * and accumulates produce/consume rates using a rolling average window.
 */
public class ThroughputMonitorPart extends AEBasePart implements IGridTickable {

    static final int DEFAULT_WINDOW_SIZE = 10;
    static final int DEFAULT_SAMPLE_PERIOD_TICKS = 20;

    private final FlowTracker tracker;
    private int samplePeriodTicks;

    public ThroughputMonitorPart(IPartItem<?> partItem) {
        super(partItem);

        this.samplePeriodTicks = DEFAULT_SAMPLE_PERIOD_TICKS;
        this.tracker = new FlowTracker(DEFAULT_WINDOW_SIZE);

        getMainNode()
                .setIdlePowerUsage(1.0 / 2.0)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .addService(IStorageWatcherNode.class, new IStorageWatcherNode() {
                    @Override
                    public void updateWatcher(IStackWatcher newWatcher) {
                        newWatcher.setWatchAll(true);
                    }

                    @Override
                    public void onStackChange(AEKey what, long amount) {
                        tracker.onStackChange(what, amount);
                    }
                });
    }

    // --- IGridTickable ---

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(samplePeriodTicks, samplePeriodTicks, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        tracker.pushSample();
        return TickRateModulation.SAME;
    }

    // --- Interaction ---

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!player.getCommandSenderWorld().isClientSide()) {
            MenuOpener.open(ThroughputMonitorMenu.TYPE, player, MenuLocators.forPart(this));
        }
        return true;
    }

    // --- Geometry ---

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(2, 2, 14, 14, 14, 16);
        bch.addBox(4, 4, 13, 12, 12, 14);
    }

    // --- Accessors for Phase 4+ ---

    public FlowTracker getTracker() {
        return tracker;
    }

    public int getSamplePeriodTicks() {
        return samplePeriodTicks;
    }

    public void setSamplePeriodTicks(int ticks) {
        this.samplePeriodTicks = Math.max(1, Math.min(100, ticks));
    }
}
