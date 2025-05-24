package com.xinian.ceres.network;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkHooks;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class NettyOptimizer {
    private static final AtomicLong PACKETS_SENT = new AtomicLong(0);
    private static final AtomicLong PACKETS_RECEIVED = new AtomicLong(0);
    private static final AtomicLong BYTES_SENT = new AtomicLong(0);
    private static final AtomicLong BYTES_RECEIVED = new AtomicLong(0);
    private static final AtomicLong PACKETS_BATCHED = new AtomicLong(0);
    private static final AtomicLong OPTIMIZED_PACKETS_PASSED = new AtomicLong(0);

    private static ScheduledExecutorService batchScheduler;

    private static final String OPTIMIZER_HANDLER_NAME = "ceres:optimizer";

    public static void init() {
        Ceres.LOGGER.info("Initializing Netty packet optimizer");

        batchScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "Ceres-BatchScheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    public static void shutdown() {
        if (batchScheduler != null) {
            batchScheduler.shutdown();
            try {
                if (!batchScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    batchScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                batchScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void injectOptimizer(Connection connection, NetworkDirection direction) {
        try {
            Channel channel = getChannel(connection);
            if (channel == null) {
                Ceres.LOGGER.error("Failed to get Netty channel from connection");
                return;
            }

            ChannelPipeline pipeline = channel.pipeline();

            if (pipeline.get(OPTIMIZER_HANDLER_NAME) != null) {
                Ceres.LOGGER.debug("Optimizer already injected into channel");
                return;
            }

            CeresChannelHandler handler = new CeresChannelHandler(direction);
            pipeline.addBefore("packet_handler", OPTIMIZER_HANDLER_NAME, handler);

            Ceres.LOGGER.info("Successfully injected packet optimizer into {} connection",
                    direction == NetworkDirection.PLAY_TO_SERVER ? "client->server" : "server->client");

        } catch (Exception e) {
            Ceres.LOGGER.error("Failed to inject packet optimizer: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private static Channel getChannel(Connection connection) {
        try {
            Field channelField = Connection.class.getDeclaredField("channel");
            channelField.setAccessible(true);
            return (Channel) channelField.get(connection);
        } catch (Exception e) {
            Ceres.LOGGER.error("Failed to access channel field: {}", e.getMessage());
            return null;
        }
    }

    public static String getNetworkStats() {
        return String.format(
                "Sent: %d packets (%d KB), Received: %d packets (%d KB), Batched: %d packets, Optimized passed: %d",
                PACKETS_SENT.get(), BYTES_SENT.get() / 1024,
                PACKETS_RECEIVED.get(), BYTES_RECEIVED.get() / 1024,
                PACKETS_BATCHED.get(), OPTIMIZED_PACKETS_PASSED.get()
        );
    }

    public static void resetStats() {
        PACKETS_SENT.set(0);
        PACKETS_RECEIVED.set(0);
        BYTES_SENT.set(0);
        BYTES_RECEIVED.set(0);
        PACKETS_BATCHED.set(0);
        OPTIMIZED_PACKETS_PASSED.set(0);
    }

    public static long getSentPacketsCount() {
        return PACKETS_SENT.get();
    }

    private static class CeresChannelHandler extends ChannelDuplexHandler {
        private final NetworkDirection direction;
        private final ConcurrentLinkedQueue<PacketEntry> outboundQueue;
        private boolean flushScheduled = false;

        public CeresChannelHandler(NetworkDirection direction) {
            this.direction = direction;
            this.outboundQueue = new ConcurrentLinkedQueue<>();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof Packet<?>) {
                PACKETS_RECEIVED.incrementAndGet();

                // 检查是否是已经优化过的数据包
                if (isOptimizedPacket(msg)) {
                    OPTIMIZED_PACKETS_PASSED.incrementAndGet();
                    if (CeresConfig.COMMON.enableLogging.get()) {
                        Ceres.LOGGER.debug("Received optimized packet: {}", msg.getClass().getSimpleName());
                    }
                } else if (CeresConfig.COMMON.enableLogging.get()) {
                    Ceres.LOGGER.debug("Received packet: {}", msg.getClass().getSimpleName());
                }
            }

            super.channelRead(ctx, msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof Packet<?>) {
                PACKETS_SENT.incrementAndGet();

                // 检查是否是已经优化过的数据包
                if (isOptimizedPacket(msg)) {
                    // 如果是已经优化过的数据包（如CompressedDataPacket），直接传递，不进行额外处理
                    OPTIMIZED_PACKETS_PASSED.incrementAndGet();
                    if (CeresConfig.COMMON.enableLogging.get()) {
                        Ceres.LOGGER.debug("Passing optimized packet: {}", msg.getClass().getSimpleName());
                    }
                    super.write(ctx, msg, promise);
                    return;
                }

                if (DuplicatePacketFilter.isDuplicate((Packet<?>) msg)) {
                    promise.setSuccess();
                    return;
                }

                boolean enableBatching = false;
                int batchDelay = 0;
                int batchSize = 0;

                if (direction == NetworkDirection.PLAY_TO_SERVER) {
                    enableBatching = CeresConfig.CLIENT.enableClientOptimizations.get();
                    batchDelay = CeresConfig.CLIENT.clientPacketBatchDelay.get();
                    batchSize = CeresConfig.CLIENT.clientPacketBatchSize.get();
                } else {
                    enableBatching = CeresConfig.SERVER.enableServerOptimizations.get();
                    batchDelay = CeresConfig.SERVER.serverPacketBatchDelay.get();
                    batchSize = CeresConfig.SERVER.serverPacketBatchSize.get();
                }

                if (enableBatching && shouldBatchPacket((Packet<?>) msg)) {
                    outboundQueue.add(new PacketEntry(msg, promise));
                    PACKETS_BATCHED.incrementAndGet();

                    if (outboundQueue.size() >= batchSize) {
                        flushQueue(ctx);
                    } else if (!flushScheduled) {
                        flushScheduled = true;
                        batchScheduler.schedule(() -> {
                            ctx.executor().execute(() -> {
                                flushQueue(ctx);
                            });
                        }, batchDelay, TimeUnit.MILLISECONDS);
                    }

                    return;
                }

                if (CeresConfig.COMMON.enableLogging.get()) {
                    Ceres.LOGGER.debug("Sending packet: {}", msg.getClass().getSimpleName());
                }
            }

            super.write(ctx, msg, promise);
        }

        /**
         * 检查是否是已经优化过的数据包
         */
        private boolean isOptimizedPacket(Object msg) {
            // 检查是否是CompressedDataPacket或其他已优化的数据包类型
            return msg instanceof CompressedDataPacket ||
                    msg.getClass().getName().startsWith("com.xinian.ceres.network");
        }

        private void flushQueue(ChannelHandlerContext ctx) {
            flushScheduled = false;

            if (outboundQueue.isEmpty()) {
                return;
            }

            List<PacketEntry> packets = new ArrayList<>();
            while (!outboundQueue.isEmpty()) {
                packets.add(outboundQueue.poll());
            }

            if (packets.size() == 1) {
                PacketEntry entry = packets.get(0);
                ctx.write(entry.packet, entry.promise);
            } else {
                for (PacketEntry entry : packets) {
                    ctx.write(entry.packet, entry.promise);
                }
            }

            ctx.flush();

            if (CeresConfig.COMMON.enableLogging.get()) {
                Ceres.LOGGER.debug("Flushed {} packets", packets.size());
            }
        }

        private boolean shouldBatchPacket(Packet<?> packet) {
            // 这里可以添加更复杂的逻辑来决定哪些数据包应该批处理
            // 例如，可以根据数据包类型或大小来决定

            // 简单实现：批处理所有数据包
            return true;
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            flushQueue(ctx);
            super.channelInactive(ctx);
        }
    }

    private static class PacketEntry {
        final Object packet;
        final ChannelPromise promise;

        public PacketEntry(Object packet, ChannelPromise promise) {
            this.packet = packet;
            this.promise = promise;
        }
    }
}
