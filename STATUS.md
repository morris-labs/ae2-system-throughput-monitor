# Status

**Active plan:** plan-ae2-throughput-monitor.md
**Current phase:** Phase 1 (complete -- pending local build verification)
**Done:** Rewound to MC 1.21.1 / NeoForge 21.1.247 / Java 21. Fixed all 26.x API
references (Identifier → ResourceLocation, displayItems lambda). Committed and pushed.
**Next:** Confirm `./gradlew compileJava` succeeds locally (especially the AE2 version),
then Phase 2 -- wire IStorageWatcherNode.
**Blockers:** AE2 version `19.1.9-beta` is a best estimate -- verify the correct
1.21.1 NeoForge artifact at https://modmaven.dev/ before building. Also confirm that
`net.neoforged.moddev` plugin `1.0.21` resolves correctly for NeoForge 21.1.247.
