package dev.morrislabs.ae2throughput.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import dev.morrislabs.ae2throughput.menu.ThroughputMonitorMenu;
import dev.morrislabs.ae2throughput.part.ThroughputMonitorPart;

/** Registers the {@code /ae2throughput} command. */
public final class ModCommands {

    private ModCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("ae2throughput")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("window")
                                .then(Commands.argument("seconds",
                                        IntegerArgumentType.integer(1, 3600))
                                        .executes(ctx -> {
                                            int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                            return setWindow(ctx.getSource()
                                                    .getServer()
                                                    .getPlayerList()
                                                    .getPlayers()
                                                    .stream()
                                                    .filter(p -> p.containerMenu instanceof ThroughputMonitorMenu)
                                                    .map(p -> ((ThroughputMonitorMenu) p.containerMenu).getPart())
                                                    .toList(),
                                                    seconds,
                                                    ctx.getSource());
                                        }))));
    }

    private static int setWindow(
            java.util.List<ThroughputMonitorPart> parts,
            int seconds,
            net.minecraft.commands.CommandSourceStack src) {
        if (parts.isEmpty()) {
            src.sendFailure(Component.literal(
                    "No throughput monitor GUI is currently open."));
            return 0;
        }
        parts.forEach(p -> p.applyWindowSeconds(seconds));
        ThroughputMonitorPart example = parts.get(0);
        int actualTicks = example.getTracker().getWindowSize() * example.getSamplePeriodTicks();
        int actualSec = actualTicks / 20;
        src.sendSuccess(() -> Component.literal(String.format(
                "Set window on %d monitor(s) to ~%ds (%dt, window=%d, period=%dt).",
                parts.size(), actualSec, actualTicks,
                example.getTracker().getWindowSize(),
                example.getSamplePeriodTicks())), true);
        return parts.size();
    }
}
