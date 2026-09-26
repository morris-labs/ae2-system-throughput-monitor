# AE2 System Throughput Monitor

A NeoForge 1.21.1 mod that adds a cable-face part showing all items flowing
through an Applied Energistics 2 network in real time.

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.247+ |
| Applied Energistics 2 | 19.2.x |

## Features

- **Cable-face part** -- attach to any ME cable face, requires one channel.
- **Per-item rates** -- shows every key with non-zero flow: green for produced,
  red for consumed, both colors when a key has simultaneous in/out flow.
- **Rolling window** -- rates are averaged over a configurable window (default 10
  samples) so spikes don't dominate the display.
- **Timescale cycling** -- press the `/s` button in the GUI to cycle through per
  second, per minute, and per hour display modes.
- **Adjustable sample period** -- change how often a snapshot is taken (1-100
  game ticks, default 20).
- **Filter bar** -- show All, Producing-only, or Consuming-only keys.
- **Scrollable list** -- scroll with the mouse wheel; up to 8 rows visible at a
  time.

## Crafting recipe

```
  C
C F C
  C
```

- **C** = AE2 Calculation Processor
- **F** = AE2 Fluix Crystal

## Configuration

The file `ae2throughputmonitor-common.toml` is created in your config directory
on first launch. It controls the defaults applied to newly placed parts:

```toml
[defaults]
    # Rolling-window size in samples. Each sample covers one sample period.
    # Range: 1 to 60
    windowSize = 10

    # Sample period in game ticks (20 ticks = 1 second).
    # Range: 1 to 100
    samplePeriodTicks = 20
```

Per-placed-instance settings (window, period, timescale) are stored in the part's
block entity NBT and persist across restarts.

## Usage

1. Craft the **ME System Throughput Monitor** part item.
2. Right-click a ME cable face to place it (it uses one channel).
3. Right-click the placed part to open the GUI.
4. Use the filter bar to narrow the list.
5. Use `Win:` and `Tick:` controls to tune averaging; use the timescale button to
   switch display units.

## License

MIT. See the `LICENSE` file for details.

## Building from source

```
git clone https://github.com/morris-labs/ae2-system-throughput-monitor
cd ae2-system-throughput-monitor
./gradlew build
```

The output JAR is at `build/libs/ae2throughputmonitor-VERSION.jar`.

To regenerate data files (models, lang, recipes):

```
./gradlew runData
```
