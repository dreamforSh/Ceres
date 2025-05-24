package com.xinian.ceres.network;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CompressedDataPacket implements OptimizedPacket {
    private final int originalPacketId;
    private final byte[] compressedData;
    private final boolean isCompressed;
    private final int originalLength;

    public CompressedDataPacket(int originalPacketId, byte[] data) {
        this.originalPacketId = originalPacketId;

        byte[] compressed = PacketCompressor.compressData(data);
        this.isCompressed = (compressed != data);
        this.compressedData = compressed;
        this.originalLength = data.length;
    }

    private CompressedDataPacket(int originalPacketId, byte[] compressedData, boolean isCompressed, int originalLength) {
        this.originalPacketId = originalPacketId;
        this.compressedData = compressedData;
        this.isCompressed = isCompressed;
        this.originalLength = originalLength;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(originalPacketId);
        buf.writeBoolean(isCompressed);
        buf.writeVarInt(originalLength);
        buf.writeVarInt(compressedData.length);
        buf.writeBytes(compressedData);
    }

    public static CompressedDataPacket decode(FriendlyByteBuf buf) {
        int originalPacketId = buf.readVarInt();
        boolean isCompressed = buf.readBoolean();
        int originalLength = buf.readVarInt();
        int compressedLength = buf.readVarInt();

        byte[] compressedData = new byte[compressedLength];
        buf.readBytes(compressedData);

        return new CompressedDataPacket(originalPacketId, compressedData, isCompressed, originalLength);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            byte[] decompressedData = PacketCompressor.decompressData(compressedData, isCompressed);

            if (CeresConfig.COMMON.enableLogging.get()) {
                Ceres.LOGGER.debug("Received compressed packet: id={}, compressed={}, size={}/{}",
                        originalPacketId, isCompressed, compressedData.length, originalLength);
            }

            NetworkOptimizer.handleOriginalPacket(originalPacketId, decompressedData, ctx.get());
        });

        ctx.get().setPacketHandled(true);
    }

    public int getOriginalPacketId() {
        return originalPacketId;
    }

    public byte[] getDecompressedData() {
        return PacketCompressor.decompressData(compressedData, isCompressed);
    }
}