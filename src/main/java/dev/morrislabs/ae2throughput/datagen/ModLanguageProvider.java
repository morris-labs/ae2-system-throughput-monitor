package dev.morrislabs.ae2throughput.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import dev.morrislabs.ae2throughput.Ae2ThroughputMod;
import dev.morrislabs.ae2throughput.registry.ModParts;

/** Generates the en_us language file for the throughput monitor mod. */
public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, Ae2ThroughputMod.MOD_ID, locale);
    }

    @Override
    protected void addTranslations() {
        add(ModParts.THROUGHPUT_MONITOR.get(), "ME System Throughput Monitor");
        add("itemGroup.ae2throughputmonitor",              "AE2 System Throughput Monitor");
        add("gui.ae2throughputmonitor.filter.all",         "All");
        add("gui.ae2throughputmonitor.filter.producing",   "Producing");
        add("gui.ae2throughputmonitor.filter.consuming",   "Consuming");
        add("gui.ae2throughputmonitor.no_flow",            "No items flowing");
        add("gui.ae2throughputmonitor.search",             "Search");
        add("gui.ae2throughputmonitor.search_hint",        "Search items...");
    }
}
