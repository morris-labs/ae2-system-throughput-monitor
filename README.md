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

- **Cable-face part** -- attach to any ME cable face; requires one channel.
- **Per-item rates** -- shows every key with non-zero flow: green for produced,
  red for consumed, both colors when a key has simultaneous in/out flow.
- **Rolling window** -- rates are averaged over a configurable rolling window
  (default 5 minutes) so spikes don't dominate the display.
- **Timescale cycling** -- press the rate button in the GUI to toggle between
  per-second (`/s`) and per-tick (`/t`) display modes.
- **Adjustable sample period** -- change how often a snapshot is taken (1-200
  game ticks, default 20).
- **Search bar** -- filter the item list by name.
- **Filter bar** -- show All, Producing-only, or Consuming-only keys.
- **Scrollable list** -- scroll with the mouse wheel; up to 9 rows visible at a
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
    # Range: 1 to 300
    windowSize = 300

    # Sample period in game ticks (20 ticks = 1 second).
    # Range: 1 to 200
    samplePeriodTicks = 20
```

Per-placed-instance settings (window, period, timescale) are stored in the part's
NBT and persist across restarts.

## Commands

`/ae2throughput window <seconds>` sets the averaging window on every monitor GUI
currently open by any player. Requires permission level 2.

```
/ae2throughput window 300
```

The command picks the best combination of window size and sample period to cover
the requested duration, and reports the actual achieved values.

## Usage

1. Craft the **ME System Throughput Monitor** part item.
2. Right-click a ME cable face to place it (it uses one channel).
3. Right-click the placed part to open the GUI.
4. Use the search bar or filter buttons to narrow the list.
5. Use the **Win** and **Tick** controls in the settings bar to tune averaging.
6. Use the rate button (`/s` or `/t`) to switch display units.

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
