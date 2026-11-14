package dev.morrislabs.ae2throughput.part;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.parts.PartModel;

import dev.morrislabs.ae2throughput.Ae2ThroughputMod;
import dev.morrislabs.ae2throughput.config.ThroughputConfig;
import dev.morrislabs.ae2throughput.menu.ThroughputMonitorMenu;
import dev.morrislabs.ae2throughput.mixin.INetworkStorageHook;

/**
 * Cable-face part that tracks every item flowing through the attached AE2 network
 * and accumulates produce/consume rates using a rolling average window.
 */
public class ThroughputMonitorPart extends AEBasePart implements IGridTickable, IFlowListener {

    public static final IPartModel MODEL = new PartModel(
            Ae2ThroughputMod.makeId("part/throughput_monitor"));

    public static final int DEFAULT_WINDOW_SIZE = 300;
    public static final int DEFAULT_SAMPLE_PERIOD_TICKS = 20;
    public static final int MIN_PERIOD = 1;
    public static final int MAX_PERIOD = 200;
    public static final int MIN_WINDOW = 1;
    public static final int MAX_WINDOW = 300;

    private final FlowTracker tracker;
    private int samplePeriodTicks;
    private Timescale timescale = Timescale.PER_SECOND;
    private int tickCounter = 0;
    private INetworkStorageHook registeredHook = null;

    public ThroughputMonitorPart(IPartItem<?> partItem) {
        super(partItem);

        this.samplePeriodTicks = ThroughputConfig.DEFAULT_SAMPLE_PERIOD_TICKS.get();
        this.tracker = new FlowTracker(ThroughputConfig.DEFAULT_WINDOW_SIZE.get());

        getMainNode()
                .setIdlePowerUsage(1.0 / 2.0)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .addService(IStorageWatcherNode.class, new IStorageWatcherNode() {
                    @Override
                    public void updateWatcher(IStackWatcher newWatcher) {
                        // AE2 diffs the next callbacks against the new grid's cached
                        // inventory, so use that snapshot as the baseline. Diffing against
                        // the old grid's amounts would produce spurious spikes.
                        tracker.resetBaseline(snapshotInventory());
                        newWatcher.setWatchAll(true);
                    }

                    @Override
                    public void onStackChange(AEKey what, long amount) {
                        tracker.onStackChange(what, amount);
                    }
                });
    }

    /** Returns the cached inventory of the grid this part belongs to, or null without a grid. */
    private Map<AEKey, Long> snapshotInventory() {
        IGrid grid = getMainNode().getGrid();
        if (grid == null) return null;
        Map<AEKey, Long> snapshot = new HashMap<>();
        for (var entry : grid.getService(IStorageService.class).getCachedInventory()) {
            snapshot.put(entry.getKey(), entry.getLongValue());
        }
        return snapshot;
    }

    // --- IFlowListener ---

    @Override
    public void onNetworkInsert(AEKey what, long amount, IActionSource src) {
        tracker.onNetworkInsert(what, amount);
    }

    @Override
    public void onNetworkExtract(AEKey what, long amount, IActionSource src) {
        tracker.onNetworkExtract(what, amount);
    }

    // --- Grid lifecycle ---

    @Override
    protected void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        if (getMainNode().isActive()) {
            registerFlowListener();
        } else {
            unregisterFlowListener();
        }
    }

    @Override
    public void removeFromWorld() {
        unregisterFlowListener();
        super.removeFromWorld();
    }

    private void registerFlowListener() {
        IGrid grid = getMainNode().getGrid();
        if (grid == null) return;
        MEStorage storage = grid.getService(IStorageService.class).getInventory();
        if (!(storage instanceof INetworkStorageHook hook)) return;
        if (registeredHook == hook) return;
        unregisterFlowListener();
        hook.addFlowListener(this);
        registeredHook = hook;
    }

    private void unregisterFlowListener() {
        if (registeredHook != null) {
            registeredHook.removeFlowListener(this);
            registeredHook = null;
        }
    }

    // --- IGridTickable ---

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        // Tick every game tick so samplePeriodTicks can be changed at runtime.
        return new TickingRequest(1, 1, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        tickCounter += ticksSinceLastCall;
        if (tickCounter >= samplePeriodTicks) {
            tickCounter -= samplePeriodTicks;
            tracker.pushSample();
        }
        return TickRateModulation.SAME;
    }

    // --- NBT ---

    @Override
    public void writeToNBT(CompoundTag data, HolderLookup.Provider registries) {
        super.writeToNBT(data, registries);
        data.putInt("samplePeriodTicks", samplePeriodTicks);
        data.putInt("windowSize", tracker.getWindowSize());
        data.putInt("timescale", timescale.ordinal());
    }

    @Override
    public void readFromNBT(CompoundTag data, HolderLookup.Provider registries) {
        super.readFromNBT(data, registries);
        if (data.contains("samplePeriodTicks")) {
            setSamplePeriodTicks(data.getInt("samplePeriodTicks"));
        }
        if (data.contains("windowSize")) {
            tracker.setWindowSize(data.getInt("windowSize"));
        }
        if (data.contains("timescale")) {
            timescale = Timescale.fromOrdinal(data.getInt("timescale"));
        }
    }

    // --- Interaction ---

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!player.level().isClientSide()) {
            MenuOpener.open(ThroughputMonitorMenu.TYPE, player, MenuLocators.forPart(this));
        }
        return true;
    }

    // --- Model ---

    @Override
    public IPartModel getStaticModels() {
        return MODEL;
    }

    // --- Geometry ---

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(2, 2, 14, 14, 14, 16);
        bch.addBox(4, 4, 13, 12, 12, 14);
    }

    // --- Accessors ---

    public FlowTracker getTracker() {
        return tracker;
    }

    public int getSamplePeriodTicks() {
        return samplePeriodTicks;
    }

    public void setSamplePeriodTicks(int ticks) {
        this.samplePeriodTicks = Math.max(MIN_PERIOD, Math.min(MAX_PERIOD, ticks));
        tracker.markChanged();
    }

    public Timescale getTimescale() {
        return timescale;
    }

    public void cycleTimescale() {
        this.timescale = timescale.next();
        tracker.markChanged();
    }

    /**
     * Sets window size and sample period to cover approximately {@code totalSeconds}
     * of observation time. Uses as many samples as MAX_WINDOW allows given the period.
     */
    public void applyWindowSeconds(int totalSeconds) {
        int totalTicks = totalSeconds * 20;
        int period = Math.max(MIN_PERIOD,
                Math.min(MAX_PERIOD, (totalTicks + MAX_WINDOW - 1) / MAX_WINDOW));
        int window = Math.max(MIN_WINDOW,
                Math.min(MAX_WINDOW, totalTicks / period));
        tracker.setWindowSize(window);
        setSamplePeriodTicks(period);
        if (getHost() != null) {
            getHost().markForSave();
        }
    }
}
