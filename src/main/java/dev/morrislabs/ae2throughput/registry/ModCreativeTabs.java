package dev.morrislabs.ae2throughput.registry;

import dev.morrislabs.ae2throughput.Ae2ThroughputMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ae2ThroughputMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CREATIVE_MODE_TABS.register(Ae2ThroughputMod.MOD_ID + "_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup." + Ae2ThroughputMod.MOD_ID))
                            .icon(() -> ModParts.THROUGHPUT_MONITOR.get().getDefaultInstance())
                            .displayItems(ModParts.PARTS.getEntries())
                            .build());
}
