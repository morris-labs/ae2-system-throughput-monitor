# Plan: AE2 System Throughput Monitor

## Goal

A NeoForge mod that adds a cable-face part (mirroring `StorageMonitor`) called the
**System Throughput Monitor**. Right-clicking it opens a GUI showing every item flowing
through the attached AE2 network -- items being produced (positive, green) and items
being consumed (negative, red) -- sorted by absolute throughput rate. Items produced
and consumed at equal rates remain visible with both figures shown.

---

## Target environment

| Parameter         | Value                  |
|-------------------|------------------------|
| Minecraft         | 26.1.2                 |
| NeoForge          | 26.1.2.x               |
| AE2               | 26.1.x (modmaven)      |
| Java              | 25                     |
| Mod ID            | `ae2throughputmonitor` |
| Group ID          | `dev.morrislabs`       |
| Mod version       | `26.1.2-1.0.0`         |

---

## Key design decisions

### Data collection: separate produced/consumed accumulation

`IStorageWatcherNode.setWatchAll(true)` fires `onStackChange(AEKey, long newAmount)`
on every change. Because this gives only a new total (not a direction), the part
keeps a `Map<AEKey, Long> lastSeenAmounts`. On each call:

- If `newAmount > lastSeen` → add `newAmount - lastSeen` to a per-key `produced`
  accumulator.
- If `newAmount < lastSeen` → add `lastSeen - newAmount` to a per-key `consumed`
  accumulator.

This ensures items produced and consumed at the same rate (net delta = 0) still appear
in the display with both figures populated.

### Rolling average window

Every 20 ticks (one world second), the tick handler:

1. Pushes each key's `(produced, consumed)` accumulator into a fixed-length circular
   buffer (`FlowSample[]`).
2. Resets the accumulators.
3. Computes the rolling average: `sum(window) / windowSize`.

Default window size is 10 samples (10 seconds). Configurable via Forge config and also
via the in-game GUI on the part.

### Timescale cycling

The GUI has a cycle button to change the display unit. Cycling does not change the
window size; it only changes how the computed per-second rate is presented:

| Timescale | Label  | Multiply by |
|-----------|--------|-------------|
| /s        | `+x/s` | 1           |
| /m        | `+x/m` | 60          |
| /h        | `+x/h` | 3600        |

The selected timescale is stored in the part's NBT and synced to clients.

### Sampling period

Default: 20 ticks (1 second). Range: 1–100 ticks. Configurable in `ae2throughputmonitor-common.toml`
and also in the in-game part GUI. Stored in the part's NBT (per-placed instance).

---

## Project structure

```
ae2throughputmonitor/
  build.gradle
  gradle.properties
  settings.gradle
  src/main/java/dev/morrislabs/ae2throughput/
    Ae2ThroughputMod.java            -- @Mod entry point, registers DeferredRegisters
    config/
      ThroughputConfig.java          -- ForgeConfigSpec (common): default window, period
    part/
      ThroughputMonitorPart.java     -- core part (IGridTickable + IStorageWatcherNode)
      FlowSample.java                -- record(long produced, long consumed)
      FlowTracker.java               -- per-key circular buffer + rolling avg
    registry/
      ModParts.java                  -- DeferredRegister<Item> for the part item
      ModMenuTypes.java              -- DeferredRegister<MenuType<?>>
      ModCreativeTabs.java           -- creative tab
    menu/
      ThroughputMonitorMenu.java     -- server-side container, holds snapshot list
    screen/
      ThroughputMonitorScreen.java   -- client-side GUI
      ThroughputEntryWidget.java     -- one row: icon + name + rate
    network/
      ThroughputUpdatePayload.java   -- CustomPacketPayload carrying List<ThroughputEntry>
      ThroughputEntry.java           -- record(AEKey, long producedPerSec, long consumedPerSec)
    datagen/
      DataGenerators.java
      ModItemModelProvider.java
      ModRecipeProvider.java
  src/main/resources/
    META-INF/neoforge.mods.toml
    META-INF/accesstransformer.cfg  (if needed)
    assets/ae2throughputmonitor/
      models/item/throughput_monitor.json
      textures/item/throughput_monitor.png
      lang/en_us.json
```

---

## GUI layout

```
+------------------------------------------+
|  System Throughput Monitor               |
|  [Window: 10s] [Period: 1s] [Scale: /s]  |
|------------------------------------------|
|  [icon] Iron Ingot          +120/s       |
|  [icon] Iron Ore            -60/s        |
|  [icon] Coal                -30/s  +30/s |  <- consumed and produced both non-zero
|  ...                                     |
|  [scroll]                                |
+------------------------------------------+
```

- Rows produced-only: green rate on the right.
- Rows consumed-only: red rate on the right.
- Rows with both: red and green rates side-by-side.
- Sorted descending by `max(|produced|, |consumed|)`.
- Filter toggle bar: All / Producing / Consuming.

---

## Build phases

| Phase | Deliverable                                                                  |
|-------|------------------------------------------------------------------------------|
| 1     | Gradle project scaffold, `@Mod`, part item in creative tab (no function)     |
| 2     | Part attaches to cable face, joins AE2 grid, `IStorageWatcherNode` collects  |
| 3     | `IGridTickable` tick: accumulate, sample, compute rolling averages           |
| 4     | Menu + network packet: server sends `ThroughputUpdatePayload` to open clients|
| 5     | Client GUI: scrollable list, color coding, filter bar                        |
| 6     | Timescale cycle button, in-game window/period controls, persist to NBT       |
| 7     | Forge config (`ae2throughputmonitor-common.toml`) for defaults               |
| 8     | Data generation: model, recipe, lang file                                    |
| 9     | Polish, textures, README, initial git tag `v1.0.0`                           |

---

## Git and versioning

- Version format: `MC_VERSION-MOD_VERSION` (for example, `26.1.2-1.0.0`).
- Every completed phase gets a commit and a version bump on the patch segment.
- Breaking changes bump minor; API-stable new features bump patch.
- Tags: `v26.1.2-1.0.0`, `v26.1.2-1.0.1`, etc.
- `github.env` in the repo root is gitignored and never committed.

---

## Open questions (resolved)

| Question                       | Decision                                              |
|-------------------------------|-------------------------------------------------------|
| Sampling rate                 | Configurable: Forge config + per-instance in-game GUI |
| Rolling average vs. snapshot  | Rolling average (default 10s window)                  |
| Simultaneous produce/consume  | Track separately per `onStackChange` direction        |
| Timescale display             | Cycle button: /s, /m, /h                              |
| Block vs. cable part          | Cable face part, mirroring `StorageMonitor`           |
| Mod ID / group                | `ae2throughputmonitor` / `dev.morrislabs`             |
