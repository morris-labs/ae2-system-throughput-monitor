package dev.morrislabs.ae2throughput.screen;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import dev.morrislabs.ae2throughput.menu.ThroughputMonitorMenu;

/** Registers client-only event handlers on the mod event bus. */
public final class ClientEventHandlers {

    private ClientEventHandlers() {}

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ThroughputMonitorMenu.TYPE, ThroughputMonitorScreen::new);
    }
}
