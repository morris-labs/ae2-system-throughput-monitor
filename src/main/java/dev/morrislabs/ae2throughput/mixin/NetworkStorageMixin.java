package dev.morrislabs.ae2throughput.mixin;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.me.storage.NetworkStorage;

import dev.morrislabs.ae2throughput.part.IFlowListener;

/**
 * Adds listener infrastructure to {@link NetworkStorage} so throughput monitor parts can
 * observe all storage inserts and extracts, including those that target void cells.
 *
 * <p>Only {@link Actionable#MODULATE} operations with a non-zero transferred amount fire
 * the listener callbacks. Simulate calls and zero-result operations are ignored.
 */
@Mixin(value = NetworkStorage.class, remap = false)
public abstract class NetworkStorageMixin implements INetworkStorageHook {

    @Unique
    private final Set<IFlowListener> ae2tm$listeners =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void addFlowListener(IFlowListener listener) {
        ae2tm$listeners.add(listener);
    }

    @Override
    public void removeFlowListener(IFlowListener listener) {
        ae2tm$listeners.remove(listener);
    }

    @Inject(method = "insert", at = @At("RETURN"), remap = false)
    private void ae2tm$afterInsert(AEKey what, long amount, Actionable type, IActionSource src,
            CallbackInfoReturnable<Long> cir) {
        if (type == Actionable.MODULATE) {
            long inserted = cir.getReturnValue();
            if (inserted > 0 && !ae2tm$listeners.isEmpty()) {
                for (IFlowListener listener : ae2tm$listeners) {
                    listener.onNetworkInsert(what, inserted, src);
                }
            }
        }
    }

    @Inject(method = "extract", at = @At("RETURN"), remap = false)
    private void ae2tm$afterExtract(AEKey what, long amount, Actionable mode, IActionSource source,
            CallbackInfoReturnable<Long> cir) {
        if (mode == Actionable.MODULATE) {
            long extracted = cir.getReturnValue();
            if (extracted > 0 && !ae2tm$listeners.isEmpty()) {
                for (IFlowListener listener : ae2tm$listeners) {
                    listener.onNetworkExtract(what, extracted, source);
                }
            }
        }
    }
}
