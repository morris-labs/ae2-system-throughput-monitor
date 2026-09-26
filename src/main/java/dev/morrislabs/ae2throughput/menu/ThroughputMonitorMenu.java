package dev.morrislabs.ae2throughput.menu;

import java.util.ArrayList;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.PacketDistributor;

import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;

import dev.morrislabs.ae2throughput.Ae2ThroughputMod;
import dev.morrislabs.ae2throughput.network.ThroughputUpdatePayload;
import dev.morrislabs.ae2throughput.part.ThroughputMonitorPart;

/**
 * Server-side container for the throughput monitor GUI. Broadcasts per-key flow rates
 * to the client each tick via {@link ThroughputUpdatePayload}.
 */
public class ThroughputMonitorMenu extends AEBaseMenu {

    public static final MenuType<ThroughputMonitorMenu> TYPE = MenuTypeBuilder
            .create(ThroughputMonitorMenu::new, ThroughputMonitorPart.class)
            .build(Ae2ThroughputMod.makeId("throughput_monitor"));

    private final ThroughputMonitorPart part;

    public ThroughputMonitorMenu(int containerId, Inventory playerInventory, ThroughputMonitorPart part) {
        super(TYPE, containerId, playerInventory, part);
        this.part = part;
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide() && getPlayer() instanceof ServerPlayer serverPlayer) {
            var averages = part.getTracker().getAverages();
            int periodTicks = part.getSamplePeriodTicks();
            float toPerSecond = 20.0f / periodTicks;

            var entries = new ArrayList<ThroughputUpdatePayload.Entry>(averages.size());
            for (var e : averages.entrySet()) {
                long produced = Math.round(e.getValue().produced() * toPerSecond);
                long consumed = Math.round(e.getValue().consumed() * toPerSecond);
                entries.add(new ThroughputUpdatePayload.Entry(e.getKey(), produced, consumed));
            }
            PacketDistributor.sendToPlayer(serverPlayer, new ThroughputUpdatePayload(entries));
        }
        super.broadcastChanges();
    }
}
