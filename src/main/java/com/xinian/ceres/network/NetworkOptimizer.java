package com.xinian.ceres.network;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import io.netty.buffer.Unpooled;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 网络优化管理器
 * 负责协调各种网络优化功能
 */
public class NetworkOptimizer {

    private static final Map<Integer, PacketProcessor<?>> packetProcessors = new HashMap<>();

    private static int nextPacketId = 0;

    /**
     * 初始化网络优化器
     */
    public static void init() {
        Ceres.LOGGER.info("Initializing NetworkOptimizer");
    }

    /**
     * 注册数据包处理器
     *
     * @param packetClass 数据包类
     * @param encoder 编码器
     * @param decoder 解码器
     * @param handler 处理器
     * @param <T> 数据包类型
     * @return 分配的数据包ID
     */
    public static <T> int registerPacketProcessor(
            Class<T> packetClass,
            BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder,
            BiConsumer<T, NetworkEvent.Context> handler) {

        int packetId = nextPacketId++;
        packetProcessors.put(packetId, new PacketProcessor<>(packetClass, encoder, decoder, handler));

        Ceres.LOGGER.debug("Registered packet processor for {} with ID {}", packetClass.getSimpleName(), packetId);

        return packetId;
    }

    /**
     * 发送优化后的数据包到服务器
     *
     * @param packet 原始数据包
     * @param packetId 数据包ID
     */
    public static void sendToServer(Object packet, int packetId) {
        if (!CeresConfig.CLIENT.enableClientOptimizations.get()) {
            return;
        }

        PacketProcessor<?> processor = packetProcessors.get(packetId);
        if (processor == null) {
            Ceres.LOGGER.error("No packet processor registered for ID {}", packetId);
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        processor.encodePacket(packet, buf);

        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        buf.release();

        CompressedDataPacket compressedPacket = new CompressedDataPacket(packetId, data);

        Ceres.NETWORK.sendToServer(compressedPacket);
    }

    /**
     * 处理原始数据包
     *
     * @param packetId 数据包ID
     * @param data 解压后的数据
     * @param ctx 网络事件上下文
     */
    @SuppressWarnings("unchecked")
    public static void handleOriginalPacket(int packetId, byte[] data, NetworkEvent.Context ctx) {
        // 获取数据包处理器
        PacketProcessor<?> processor = packetProcessors.get(packetId);
        if (processor == null) {
            Ceres.LOGGER.error("No packet processor registered for ID {}", packetId);
            return;
        }

        // 解码数据包
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        Object packet = processor.decodePacket(buf);
        buf.release();

        // 处理数据包
        processor.handlePacket(packet, ctx);
    }

    /**
     * 数据包处理器
     *
     * @param <T> 数据包类型
     */
    private static class PacketProcessor<T> {
        private final Class<T> packetClass;
        private final BiConsumer<T, FriendlyByteBuf> encoder;
        private final Function<FriendlyByteBuf, T> decoder;
        private final BiConsumer<T, NetworkEvent.Context> handler;

        public PacketProcessor(
                Class<T> packetClass,
                BiConsumer<T, FriendlyByteBuf> encoder,
                Function<FriendlyByteBuf, T> decoder,
                BiConsumer<T, NetworkEvent.Context> handler) {
            this.packetClass = packetClass;
            this.encoder = encoder;
            this.decoder = decoder;
            this.handler = handler;
        }

        @SuppressWarnings("unchecked")
        public void encodePacket(Object packet, FriendlyByteBuf buf) {
            encoder.accept((T) packet, buf);
        }

        public T decodePacket(FriendlyByteBuf buf) {
            return decoder.apply(buf);
        }

        @SuppressWarnings("unchecked")
        public void handlePacket(Object packet, NetworkEvent.Context ctx) {
            handler.accept((T) packet, ctx);
        }
    }
}