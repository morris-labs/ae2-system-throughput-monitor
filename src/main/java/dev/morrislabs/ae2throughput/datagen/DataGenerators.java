package dev.morrislabs.ae2throughput.datagen;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Wires all data providers into the NeoForge data-generation pipeline. */
public final class DataGenerators {

    private DataGenerators() {}

    public static void gather(GatherDataEvent event) {
        var gen      = event.getGenerator();
        var output   = gen.getPackOutput();
        var lookup   = event.getLookupProvider();
        var existing = event.getExistingFileHelper();

        gen.addProvider(event.includeClient(), new ModItemModelProvider(output, existing));
        gen.addProvider(event.includeClient(), new ModLanguageProvider(output, "en_us"));
        gen.addProvider(event.includeServer(), new ModRecipeProvider(output, lookup));
    }
}
