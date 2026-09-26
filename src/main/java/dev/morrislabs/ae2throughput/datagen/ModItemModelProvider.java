package dev.morrislabs.ae2throughput.datagen;

import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.minecraft.data.PackOutput;

import dev.morrislabs.ae2throughput.Ae2ThroughputMod;
import dev.morrislabs.ae2throughput.registry.ModParts;

/** Generates item model JSON for the throughput monitor part item. */
public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Ae2ThroughputMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModParts.THROUGHPUT_MONITOR.get());
    }
}
