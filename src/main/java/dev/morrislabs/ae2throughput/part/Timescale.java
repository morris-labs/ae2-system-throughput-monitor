package dev.morrislabs.ae2throughput.part;

/** Display timescale for throughput rates. */
public enum Timescale {
    PER_SECOND("/s"),
    PER_TICK("/t");

    public final String suffix;

    Timescale(String suffix) {
        this.suffix = suffix;
    }

    private static final Timescale[] VALUES = values();

    public Timescale next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public static Timescale fromOrdinal(int ord) {
        return VALUES[Math.max(0, Math.min(ord, VALUES.length - 1))];
    }
}
