package dev.morrislabs.ae2throughput.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import appeng.api.stacks.AEKey;

import dev.morrislabs.ae2throughput.Ae2ThroughputMod;

/**
 * Server-to-client snapshot of per-key flow rates and current part settings.
 * Rates in the entries are in items per second; the client multiplies by the
 * timescale's multiplier for display.
 */
public record ThroughputUpdatePayload(
        List<Entry> entries,
        int timescaleOrdinal,
        int windowSize,
        int samplePeriodTicks
) implements CustomPacketPayload {

    /** One key's produce/consume rates in items per second. */
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
            // key is null when the serialized key type is no longer registered (e.g. after a mod
            // update). Skip the entry rather than storing a null that causes an NPE in the renderer.
            if (key != null) {
                entries.add(new Entry(key, produced, consumed));
            }
        }
        int timescaleOrd = buf.readUnsignedByte();
        int windowSize = buf.readVarInt();
        int period = buf.readVarInt();
        return new ThroughputUpdatePayload(List.copyOf(entries), timescaleOrd, windowSize, period);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (var e : entries) {
            AEKey.writeKey(buf, e.key());
            buf.writeVarLong(e.produced());
            buf.writeVarLong(e.consumed());
        }
        buf.writeByte(timescaleOrdinal);
        buf.writeVarInt(windowSize);
        buf.writeVarInt(samplePeriodTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
