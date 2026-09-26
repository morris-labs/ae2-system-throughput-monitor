package dev.morrislabs.ae2throughput;

import dev.morrislabs.ae2throughput.config.ThroughputConfig;
import dev.morrislabs.ae2throughput.datagen.DataGenerators;
import dev.morrislabs.ae2throughput.menu.ThroughputMonitorMenu;
import dev.morrislabs.ae2throughput.network.ModNetwork;
import dev.morrislabs.ae2throughput.registry.ModCreativeTabs;
import dev.morrislabs.ae2throughput.registry.ModParts;
import dev.morrislabs.ae2throughput.screen.ClientEventHandlers;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Ae2ThroughputMod.MOD_ID)
public final class Ae2ThroughputMod {
    public static final String MOD_ID = "ae2throughputmonitor";

    public Ae2ThroughputMod(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ThroughputConfig.SPEC);
        ModParts.PARTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(ModNetwork::register);
        modEventBus.addListener(DataGenerators::gather);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientEventHandlers::registerScreens);
        }
        // Load ThroughputMonitorMenu now so its TYPE static field is queued in
        // AE2's InitMenuTypes before RegisterEvent fires for the MENU registry.
        @SuppressWarnings("unused")
        var ignored = ThroughputMonitorMenu.TYPE;
    }

    public static ResourceLocation makeId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
