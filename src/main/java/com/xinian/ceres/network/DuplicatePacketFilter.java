package com.xinian.ceres.network;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.protocol.Packet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DuplicatePacketFilter {
    private static final Map<Class<?>, PacketCache> PACKET_CACHE = new ConcurrentHashMap<>();

    private static final AtomicLong DUPLICATE_PACKETS_FILTERED = new AtomicLong(0);
    private static final AtomicLong TOTAL_PACKETS_CHECKED = new AtomicLong(0);

    private static final int MAX_CACHE_ENTRIES = 1000;

    private static int cleanupCounter = 0;

    public static boolean isDuplicate(Packet<?> packet) {
        if (!CeresConfig.COMMON.enableDuplicateFiltering.get()) {
            return false;
        }

        TOTAL_PACKETS_CHECKED.incrementAndGet();

        Class<?> packetClass = packet.getClass();

        if (!shouldFilterPacketType(packetClass)) {
            return false;
        }

        int packetHash = computePacketHash(packet);

        PacketCache cache = PACKET_CACHE.computeIfAbsent(packetClass, k -> new PacketCache());

        boolean isDuplicate = cache.checkAndUpdate(packetHash);

        if (isDuplicate) {
            DUPLICATE_PACKETS_FILTERED.incrementAndGet();

            if (CeresConfig.COMMON.enableLogging.get()) {
                Ceres.LOGGER.debug("Filtered duplicate packet: {}", packetClass.getSimpleName());
            }
        }

        cleanupCacheIfNeeded();

        return isDuplicate;
    }

    private static int computePacketHash(Packet<?> packet) {
        try {
            ByteBuf buf = Unpooled.buffer();

            try {
                packet.getClass().getMethod("write", ByteBuf.class).invoke(packet, buf);
            } catch (NoSuchMethodException e) {
                net.minecraft.network.FriendlyByteBuf friendlyBuf = new net.minecraft.network.FriendlyByteBuf(buf);
                packet.getClass().getMethod("write", net.minecraft.network.FriendlyByteBuf.class).invoke(packet, friendlyBuf);
            }

            int hash = 0;
            byte[] bytes = new byte[buf.readableBytes()];
            buf.getBytes(0, bytes);

            for (byte b : bytes) {
                hash = 31 * hash + b;
            }

            buf.release();
            return hash;
        } catch (Exception e) {
            Ceres.LOGGER.warn("Failed to compute packet hash for {}: {}", packet.getClass().getSimpleName(), e.getMessage());
            return packet.hashCode();
        }
    }

    private static boolean shouldFilterPacketType(Class<?> packetClass) {
        String className = packetClass.getName();

        if (className.contains("Position") || className.contains("Move")) {
            return CeresConfig.COMMON.filterPositionPackets.get();
        }

        if (className.contains("Chunk") || className.contains("Block")) {
            return CeresConfig.COMMON.filterChunkPackets.get();
        }

        if (className.contains("Entity")) {
            return CeresConfig.COMMON.filterEntityPackets.get();
        }

        return true;
    }

    private static void cleanupCacheIfNeeded() {
        cleanupCounter++;

        if (cleanupCounter >= 1000) {
            cleanupCounter = 0;

            if (PACKET_CACHE.size() > MAX_CACHE_ENTRIES) {
                PACKET_CACHE.clear();

                if (CeresConfig.COMMON.enableLogging.get()) {
                    Ceres.LOGGER.debug("Cleared packet cache");
                }
            }
        }
    }

    public static void resetStats() {
        DUPLICATE_PACKETS_FILTERED.set(0);
        TOTAL_PACKETS_CHECKED.set(0);
        PACKET_CACHE.clear();
        cleanupCounter = 0;
    }

    public static String getStats() {
        long total = TOTAL_PACKETS_CHECKED.get();
        long filtered = DUPLICATE_PACKETS_FILTERED.get();
        double percentage = total > 0 ? (filtered * 100.0 / total) : 0;

        return String.format(
                "Duplicate packets: %d/%d (%.1f%%) filtered, cache size: %d",
                filtered, total, percentage, PACKET_CACHE.size()
        );
    }

    public static long getFilteredPacketsCount() {
        return DUPLICATE_PACKETS_FILTERED.get();
    }

    private static class PacketCache {
        private int lastHash;
        private int duplicateCount;
        private long lastUpdateTime;

        public PacketCache() {
            this.lastHash = 0;
            this.duplicateCount = 0;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        public boolean checkAndUpdate(int hash) {
            long currentTime = System.currentTimeMillis();

            int timeoutMs = CeresConfig.COMMON.duplicateTimeoutMs.get();
            if (currentTime - lastUpdateTime > timeoutMs) {
                lastHash = hash;
                duplicateCount = 0;
                lastUpdateTime = currentTime;
                return false;
            }

            boolean isDuplicate = (hash == lastHash);

            if (isDuplicate) {
                duplicateCount++;

                int maxDuplicates = CeresConfig.COMMON.maxConsecutiveDuplicates.get();
                if (duplicateCount > maxDuplicates) {
                    lastHash = hash;
                    duplicateCount = 0;
                    lastUpdateTime = currentTime;
                    return false;
                }
            } else {
                lastHash = hash;
                duplicateCount = 0;
            }

            lastUpdateTime = currentTime;

            return isDuplicate;
        }
    }
}
