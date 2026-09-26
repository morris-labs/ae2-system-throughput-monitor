package dev.morrislabs.ae2throughput.part;

/** Display timescale for throughput rates. Applies a multiplier to per-second values. */
public enum Timescale {
    PER_SECOND(1L, "/s"),
    PER_MINUTE(60L, "/m"),
    PER_HOUR(3600L, "/h");

    public final long multiplier;
    public final String suffix;

    Timescale(long multiplier, String suffix) {
        this.multiplier = multiplier;
        this.suffix = suffix;
    }

    public Timescale next() {
        Timescale[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static Timescale fromOrdinal(int ord) {
        Timescale[] v = values();
        return v[Math.max(0, Math.min(ord, v.length - 1))];
    }
}
