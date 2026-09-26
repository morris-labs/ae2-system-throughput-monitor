package dev.morrislabs.ae2throughput.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import dev.morrislabs.ae2throughput.Ae2ThroughputMod;
import dev.morrislabs.ae2throughput.screen.ThroughputMonitorScreen;

/**
 * Registers all custom network payload types for this mod.
 */
public final class ModNetwork {

    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(Ae2ThroughputMod.MOD_ID);
        registrar.playToClient(
                ThroughputUpdatePayload.TYPE,
                ThroughputUpdatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ThroughputMonitorScreen.handlePayload(payload)));
    }
}
