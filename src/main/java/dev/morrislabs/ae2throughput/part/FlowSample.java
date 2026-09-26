package dev.morrislabs.ae2throughput.part;

/**
 * Raw flow totals for a single item key, summed over the entire rolling window.
 * Divide by {@code windowSize} and by {@code samplePeriodTicks / 20} to get items per second.
 *
 * @param totalProduced items added to the network across all window samples
 * @param totalConsumed items removed from the network across all window samples
 */
public record FlowSample(long totalProduced, long totalConsumed) {
    public static final FlowSample EMPTY = new FlowSample(0, 0);

    public boolean isZero() {
        return totalProduced == 0 && totalConsumed == 0;
    }
}
