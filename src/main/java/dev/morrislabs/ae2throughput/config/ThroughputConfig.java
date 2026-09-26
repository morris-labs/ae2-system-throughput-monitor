package dev.morrislabs.ae2throughput.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import dev.morrislabs.ae2throughput.part.ThroughputMonitorPart;

/** Common config for the throughput monitor -- default window size and sample period. */
public final class ThroughputConfig {

    public static final ModConfigSpec.IntValue DEFAULT_WINDOW_SIZE;
    public static final ModConfigSpec.IntValue DEFAULT_SAMPLE_PERIOD_TICKS;

    public static final ModConfigSpec SPEC;

    static {
        var builder = new ModConfigSpec.Builder();

        builder.comment("Throughput Monitor defaults (applied when placing a new part).");
        builder.push("defaults");

        DEFAULT_WINDOW_SIZE = builder
                .comment("Rolling-window size in samples. Each sample covers one sample period.")
                .defineInRange("windowSize",
                        ThroughputMonitorPart.DEFAULT_WINDOW_SIZE,
                        ThroughputMonitorPart.MIN_WINDOW,
                        ThroughputMonitorPart.MAX_WINDOW);

        DEFAULT_SAMPLE_PERIOD_TICKS = builder
                .comment("Sample period in game ticks (20 ticks = 1 second).")
                .defineInRange("samplePeriodTicks",
                        ThroughputMonitorPart.DEFAULT_SAMPLE_PERIOD_TICKS,
                        ThroughputMonitorPart.MIN_PERIOD,
                        ThroughputMonitorPart.MAX_PERIOD);

        builder.pop();

        SPEC = builder.build();
    }

    private ThroughputConfig() {}
}
