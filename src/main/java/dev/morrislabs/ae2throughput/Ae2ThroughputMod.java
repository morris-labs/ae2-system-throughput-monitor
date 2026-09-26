package dev.morrislabs.ae2throughput;

import dev.morrislabs.ae2throughput.registry.ModCreativeTabs;
import dev.morrislabs.ae2throughput.registry.ModParts;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Ae2ThroughputMod.MOD_ID)
public final class Ae2ThroughputMod {
    public static final String MOD_ID = "ae2throughputmonitor";

    public Ae2ThroughputMod(IEventBus modEventBus) {
        ModParts.PARTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
    }

    public static Identifier makeId(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
