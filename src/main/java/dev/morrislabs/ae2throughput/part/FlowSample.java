package dev.morrislabs.ae2throughput.part;

/**
 * One time-slice of flow data for a single item key.
 *
 * @param produced items added to the network during this sample period
 * @param consumed items removed from the network during this sample period
 */
public record FlowSample(long produced, long consumed) {
    public static final FlowSample EMPTY = new FlowSample(0, 0);

    public boolean isZero() {
        return produced == 0 && consumed == 0;
    }
}
