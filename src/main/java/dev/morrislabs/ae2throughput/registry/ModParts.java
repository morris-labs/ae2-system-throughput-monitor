package dev.morrislabs.ae2throughput.registry;

import dev.morrislabs.ae2throughput.Ae2ThroughputMod;
import dev.morrislabs.ae2throughput.part.ThroughputMonitorPart;

import appeng.items.parts.PartItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParts {
    public static final DeferredRegister<Item> PARTS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.ITEM, Ae2ThroughputMod.MOD_ID);

    public static final DeferredHolder<Item, PartItem<ThroughputMonitorPart>> THROUGHPUT_MONITOR =
            PARTS.register("throughput_monitor", () ->
                    new PartItem<>(
                            new Item.Properties().stacksTo(1),
                            ThroughputMonitorPart.class,
                            ThroughputMonitorPart::new));
}
