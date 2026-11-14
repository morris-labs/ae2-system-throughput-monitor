package dev.morrislabs.ae2throughput.part;

/**
 * Raw flow totals for a single item key, summed over the entire rolling window.
 * Divide by {@code windowSize} and by {@code samplePeriodTicks / 20} to get items per second.
 *
 * @param totalProduced items added to network storage across all window samples
 * @param totalConsumed items removed from network storage across all window samples
 * @param totalVoided   items accepted by the storage layer but not retained (for example, items
 *                      inserted into a void cell); equals inserted-minus-watcher-positive-delta
 */
public record FlowSample(long totalProduced, long totalConsumed, long totalVoided) {
    public static final FlowSample EMPTY = new FlowSample(0, 0, 0);

    public boolean isZero() {
        return totalProduced == 0 && totalConsumed == 0 && totalVoided == 0;
    }
}
