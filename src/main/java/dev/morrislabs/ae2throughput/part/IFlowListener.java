package dev.morrislabs.ae2throughput.part;

import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;

/**
 * Listener that receives raw flow events from the {@link appeng.me.storage.NetworkStorage}
 * Mixin before any filtering or aggregation.
 *
 * <p>Both methods fire only for {@link appeng.api.config.Actionable#MODULATE} operations with
 * a non-zero transferred amount.
 */
public interface IFlowListener {

    /**
     * Called when items are inserted into the network's storage layer, including into void cells.
     *
     * @param what   the key inserted
     * @param amount the number of items actually accepted (return value of {@code insert})
     * @param src    the source of the operation
     */
    void onNetworkInsert(AEKey what, long amount, IActionSource src);

    /**
     * Called when items are extracted from the network's storage layer.
     *
     * @param what   the key extracted
     * @param amount the number of items actually taken (return value of {@code extract})
     * @param src    the source of the operation
     */
    void onNetworkExtract(AEKey what, long amount, IActionSource src);
}
