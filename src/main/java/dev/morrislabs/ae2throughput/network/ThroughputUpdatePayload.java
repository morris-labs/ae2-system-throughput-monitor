package dev.morrislabs.ae2throughput.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import appeng.api.stacks.AEKey;

import dev.morrislabs.ae2throughput.Ae2ThroughputMod;

/**
 * Server-to-client snapshot of per-key flow rates for all keys with non-zero throughput.
 * Rates are in items per second.
 */
public record ThroughputUpdatePayload(List<Entry> entries) implements CustomPacketPayload {

    /** One key's current produce/consume rates, in items per second. */
    public record Entry(AEKey key, long produced, long consumed) {}

    public static final Type<ThroughputUpdatePayload> TYPE =
            new Type<>(Ae2ThroughputMod.makeId("throughput_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ThroughputUpdatePayload> STREAM_CODEC =
            StreamCodec.ofMember(ThroughputUpdatePayload::write, ThroughputUpdatePayload::decode);

    public static ThroughputUpdatePayload decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        var entries = new ArrayList<Entry>(size);
        for (int i = 0; i < size; i++) {
            var key = AEKey.readKey(buf);
            long produced = buf.readVarLong();
            long consumed = buf.readVarLong();
            entries.add(new Entry(key, produced, consumed));
        }
        return new ThroughputUpdatePayload(List.copyOf(entries));
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (var e : entries) {
            AEKey.writeKey(buf, e.key());
            buf.writeVarLong(e.produced());
            buf.writeVarLong(e.consumed());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
