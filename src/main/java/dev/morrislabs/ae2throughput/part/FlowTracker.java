package dev.morrislabs.ae2throughput.part;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import appeng.api.stacks.AEKey;

/**
 * Tracks per-key produce/consume flow over a rolling window of samples.
 *
 * <p>Call {@link #onStackChange} on every storage-watcher callback, and {@link #onNetworkInsert}
 * and {@link #onNetworkExtract} on every network storage transfer. Call {@link #pushSample}
 * once per sample period to commit the accumulated deltas into the rolling window. Call
 * {@link #getAverages} to read the current smoothed rates.
 *
 * <p>The unit returned by {@link #getAverages} is items-per-sample-period. Callers
 * multiply by {@code (20 / samplePeriodTicks)} to convert to per-second.
 */
public class FlowTracker {

    private int windowSize;

    /**
     * Increments whenever the data or settings that viewers display change. Each viewer keeps
     * its own last-sent value, so one viewer's update never hides a change from another.
     */
    private long version = 0;

    /** Last amount observed for each key. Keys absent here are first-time observations. */
    private final Map<AEKey, Long> lastSeen = new HashMap<>();

    /** Accumulated produced/consumed since the last {@link #pushSample} call. Index 0 = produced, 1 = consumed. */
    private final Map<AEKey, long[]> pending = new HashMap<>();

    /**
     * Total items inserted into the network storage layer per key since the last {@link #pushSample}
     * call, as reported by the {@link dev.morrislabs.ae2throughput.mixin.NetworkStorageMixin}.
     * Includes both items that reached real storage and items accepted by void cells.
     */
    private final Map<AEKey, Long> pendingNetworkInserted = new HashMap<>();

    /** Total items extracted from the network storage layer per key since the last {@link #pushSample}. */
    private final Map<AEKey, Long> pendingNetworkExtracted = new HashMap<>();

    /**
     * Keys whose first watcher event this period arrived without a baseline. The watcher delta
     * for these keys is unknown, so {@link #pushSample} does not infer voided items for them.
     */
    private final Set<AEKey> unbaselined = new java.util.HashSet<>();

    /** Circular buffer of [produced, consumed, voided] triples per key. */
    private final Map<AEKey, long[][]> windows = new HashMap<>();

    /** Current write head per key in the circular buffer. */
    private final Map<AEKey, Integer> heads = new HashMap<>();

    public FlowTracker(int windowSize) {
        this.windowSize = Math.max(1, windowSize);
    }

    /**
     * Records a storage-watcher callback. Compares against the last known amount to
     * separate produced (amount rose) from consumed (amount fell) without losing signal
     * when opposing flows cancel each other out.
     */
    public void onStackChange(AEKey what, long newAmount) {
        Long prev = lastSeen.put(what, newAmount);
        if (prev == null) {
            // First observation: no baseline to diff against, so this period's delta is unknown.
            unbaselined.add(what);
            return;
        }
        if (newAmount > prev) {
            long[] acc = pending.computeIfAbsent(what, k -> new long[2]);
            acc[0] += newAmount - prev;
        } else if (newAmount < prev) {
            long[] acc = pending.computeIfAbsent(what, k -> new long[2]);
            acc[1] += prev - newAmount;
        }
    }

    /**
     * Called by {@link dev.morrislabs.ae2throughput.mixin.NetworkStorageMixin} when items enter
     * the network storage layer. This fires before void cells silently discard items, so the
     * difference between this total and the watcher's positive delta reveals the voided amount.
     */
    public void onNetworkInsert(AEKey what, long amount) {
        pendingNetworkInserted.merge(what, amount, Long::sum);
    }

    /**
     * Called by {@link dev.morrislabs.ae2throughput.mixin.NetworkStorageMixin} when items leave
     * the network storage layer. The watcher reports only net per-tick changes, so this gross
     * count reveals items inserted and extracted within the same tick.
     */
    public void onNetworkExtract(AEKey what, long amount) {
        pendingNetworkExtracted.merge(what, amount, Long::sum);
    }

    /**
     * Commits the accumulated deltas into the rolling window and clears the pending
     * accumulators. Call this once per sample period.
     */
    public void pushSample() {
        // Write accumulated data (or zeros for keys already in a window but idle this period).
        for (AEKey key : allTrackedKeys()) {
            long[] acc = pending.get(key);
            long watcherRise = acc != null ? acc[0] : 0;
            long watcherFall = acc != null ? acc[1] : 0;
            long networkInserted = pendingNetworkInserted.getOrDefault(key, 0L);
            long networkExtracted = pendingNetworkExtracted.getOrDefault(key, 0L);
            long voided = unbaselined.contains(key)
                    ? 0
                    : computeVoided(watcherRise, watcherFall, networkInserted, networkExtracted);
            // The watcher sees only net per-tick change; gross insert and extract counts
            // recover flow that cancels out within one tick.
            long produced = Math.max(watcherRise, networkInserted - voided);
            long consumed = Math.max(watcherFall, networkExtracted);

            long[][] window = windows.computeIfAbsent(key, k -> new long[windowSize][3]);
            int head = heads.getOrDefault(key, 0);
            window[head][0] = produced;
            window[head][1] = consumed;
            window[head][2] = voided;
            heads.put(key, (head + 1) % windowSize);
        }

        pending.clear();
        pendingNetworkInserted.clear();
        pendingNetworkExtracted.clear();
        unbaselined.clear();
        version++;

        // Prune keys whose window has gone entirely to zero (flow stopped long enough ago).
        pruneZeroWindows();
    }

    /**
     * Returns the items accepted by the storage layer but not retained in real storage.
     * Extractions that the watcher did not see as a fall offset inserts that the watcher did
     * not see as a rise, which covers same-tick pass-through. Watcher falls without a matching
     * extract come from outside the network, such as a storage bus, and never count as voided.
     */
    static long computeVoided(long watcherRise, long watcherFall, long networkInserted, long networkExtracted) {
        long hiddenExtract = Math.max(0, networkExtracted - watcherFall);
        return Math.max(0, networkInserted - watcherRise - hiddenExtract);
    }

    private void pruneZeroWindows() {
        List<AEKey> toRemove = new ArrayList<>();
        for (var entry : windows.entrySet()) {
            long[][] window = entry.getValue();
            boolean anyNonZero = false;
            for (long[] sample : window) {
                if (sample[0] != 0 || sample[1] != 0 || sample[2] != 0) {
                    anyNonZero = true;
                    break;
                }
            }
            if (!anyNonZero) {
                toRemove.add(entry.getKey());
            }
        }
        for (AEKey key : toRemove) {
            windows.remove(key);
            heads.remove(key);
        }
    }

    /**
     * Returns raw window totals for every key that has non-zero flow.
     * Each value is the sum over all samples in the window. Callers divide by
     * {@link #getWindowSize()} and by {@code samplePeriodTicks / 20} to get items per second.
     * Returning totals rather than averages preserves the fractional part for display.
     */
    public Map<AEKey, FlowSample> getAverages() {
        Map<AEKey, FlowSample> result = new HashMap<>();
        for (var entry : windows.entrySet()) {
            long[][] window = entry.getValue();
            long totalProduced = 0;
            long totalConsumed = 0;
            long totalVoided = 0;
            for (long[] sample : window) {
                totalProduced += sample[0];
                totalConsumed += sample[1];
                totalVoided += sample[2];
            }
            if (totalProduced > 0 || totalConsumed > 0 || totalVoided > 0) {
                result.put(entry.getKey(), new FlowSample(totalProduced, totalConsumed, totalVoided));
            }
        }
        return result;
    }

    /** Keys that either have pending data this period or already have a window (tracking ongoing). */
    private Iterable<AEKey> allTrackedKeys() {
        // Defensive copies prevent ConcurrentModificationException if pushSample() modifies
        // the maps while iterating (for example, computeIfAbsent adding a new window entry).
        var all = new java.util.HashSet<AEKey>(windows.keySet());
        all.addAll(pending.keySet());
        all.addAll(pendingNetworkInserted.keySet());
        all.addAll(pendingNetworkExtracted.keySet());
        return all;
    }

    public int getWindowSize() {
        return windowSize;
    }

    /**
     * Returns a counter that changes whenever displayed data or settings change. Compare it with
     * the value from the last packet to skip network packets when nothing changed.
     */
    public long getVersion() {
        return version;
    }

    /** Records a settings change that viewers must receive. */
    public void markChanged() {
        version++;
    }

    /**
     * Clears the per-key baseline so the next {@link #onStackChange} callbacks after a grid
     * reconnect are treated as first observations rather than spurious spikes.
     * Leaves existing window data intact.
     */
    public void resetBaseline() {
        lastSeen.clear();
        pending.clear();
        pendingNetworkInserted.clear();
        pendingNetworkExtracted.clear();
        unbaselined.clear();
    }

    /**
     * Changes the window size. Clears all existing window data because the buffer
     * dimensions change.
     */
    public void setWindowSize(int newSize) {
        // Max bound matches ThroughputMonitorPart.MAX_WINDOW; clamped here to prevent a
        // malicious or overflowed client value from causing an OOM on window allocation.
        this.windowSize = Math.max(1, Math.min(300, newSize));
        windows.clear();
        heads.clear();
        version++;
    }
}
