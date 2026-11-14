package dev.morrislabs.ae2throughput.mixin;

import dev.morrislabs.ae2throughput.part.IFlowListener;

/**
 * Injected into {@code appeng.me.storage.NetworkStorage} by {@link NetworkStorageMixin}.
 * Cast any {@link appeng.api.storage.MEStorage} returned by
 * {@code IStorageService.getInventory()} to this interface to register a listener.
 */
public interface INetworkStorageHook {

    void addFlowListener(IFlowListener listener);

    void removeFlowListener(IFlowListener listener);
}
