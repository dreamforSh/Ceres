package com.xinian.ceres.client;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import com.xinian.ceres.network.DuplicatePacketFilter;
import com.xinian.ceres.network.NettyOptimizer;
import com.xinian.ceres.network.PacketCompressor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;

/**
 * Ceres调试覆盖层
 * 在F3调试屏幕上显示网络优化统计信息
 */
@Mod.EventBusSubscriber(modid = Ceres.MOD_ID, value = Dist.CLIENT)
public class DebugOverlay {

    private static final List<String> CERES_DEBUG_LINES = new ArrayList<>();
    private static int updateCounter = 0;
    private static boolean hasNetworkActivity = false;

    // 用于格式化数字的格式化器
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#,##0.0");

    // 统计数据
    private static long totalOriginalBytes = 0;
    private static long totalOptimizedBytes = 0;
    private static long totalPackets = 0;
    private static long totalFilteredPackets = 0;
    private static long sessionStartTime = System.currentTimeMillis();

    /**
     * 客户端Tick事件处理
     * 用于定期更新调试信息
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.side != LogicalSide.CLIENT || event.phase != TickEvent.Phase.END) {
            return;
        }

        // 每5刻更新一次统计信息（约0.25秒），加快刷新速度
        if (updateCounter++ % 5 == 0) {
            updateDebugInfo();
        }
    }

    /**
     * 更新调试信息
     */
    private static void updateDebugInfo() {
        CERES_DEBUG_LINES.clear();

        // 添加模式信息
        CERES_DEBUG_LINES.add("[Ceres] Mode: " + CeresConfig.COMMON.optimizationMode.get());

        // 检查是否有网络活动
        hasNetworkActivity = checkNetworkActivity();

        // 更新统计数据
        updateStatistics();

        if (!hasNetworkActivity) {
            // 如果没有网络活动，显示状态信息
            CERES_DEBUG_LINES.add("[Ceres] Status: Waiting for network activity...");

            // 添加配置信息，即使没有网络活动也能显示
            if (CeresConfig.COMMON.enableCompression.get()) {
                CERES_DEBUG_LINES.add("[Ceres] Compression: Enabled (Threshold: " +
                        CeresConfig.COMMON.compressionThreshold.get() + " bytes, Level: " +
                        CeresConfig.COMMON.compressionLevel.get() + ")");
            } else {
                CERES_DEBUG_LINES.add("[Ceres] Compression: Disabled");
            }

            if (CeresConfig.COMMON.enableDuplicateFiltering.get()) {
                CERES_DEBUG_LINES.add("[Ceres] Duplicate Filtering: Enabled");
            } else {
                CERES_DEBUG_LINES.add("[Ceres] Duplicate Filtering: Disabled");
            }
        } else {
            // 如果有网络活动，显示统计信息

            // 添加压缩统计信息
            CERES_DEBUG_LINES.add("[Ceres] " + PacketCompressor.getCompressionStats());

            // 添加重复过滤统计信息
            CERES_DEBUG_LINES.add("[Ceres] " + DuplicatePacketFilter.getStats());

            // 添加Netty优化统计信息（如果使用现代模式）
            if (CeresConfig.COMMON.optimizationMode.get() == CeresConfig.OptimizationMode.MODERN) {
                CERES_DEBUG_LINES.add("[Ceres] " + NettyOptimizer.getNetworkStats());
            }

            // 添加总体优化统计信息
            addOverallStatistics();
        }
    }

    /**
     * 更新统计数据
     */
    private static void updateStatistics() {

        String compressionStats = PacketCompressor.getCompressionStats();
        if (!compressionStats.startsWith("No packets")) {
            try {

                String[] parts = compressionStats.split(",");
                if (parts.length >= 3) {
                    String beforePart = parts[1].trim();
                    String afterPart = parts[2].trim();

                    int beforeIndex = beforePart.indexOf("KB");
                    int afterIndex = afterPart.indexOf("KB");

                    if (beforeIndex > 0 && afterIndex > 0) {
                        long beforeKB = Long.parseLong(beforePart.substring(0, beforeIndex).trim());
                        long afterKB = Long.parseLong(afterPart.substring(0, afterIndex).trim());

                        totalOriginalBytes = beforeKB * 1024;
                        totalOptimizedBytes = afterKB * 1024;
                    }
                }
            } catch (Exception e) {
                // 解析失败，忽略
            }
        }

        // 从DuplicatePacketFilter获取过滤统计信息
        totalFilteredPackets = DuplicatePacketFilter.getFilteredPacketsCount();

        // 从NettyOptimizer获取总包数（如果使用现代模式）
        if (CeresConfig.COMMON.optimizationMode.get() == CeresConfig.OptimizationMode.MODERN) {
            totalPackets = NettyOptimizer.getSentPacketsCount();
        }
    }

    /**
     * 添加总体优化统计信息
     */
    private static void addOverallStatistics() {

        long sessionDurationMs = System.currentTimeMillis() - sessionStartTime;
        long sessionDurationSec = sessionDurationMs / 1000;


        double optimizationRatio = (totalOriginalBytes > 0) ?
                (double) totalOptimizedBytes / totalOriginalBytes : 1.0;


        double savedBytes = totalOriginalBytes - totalOptimizedBytes;


        double originalBytesPerSec = (sessionDurationSec > 0) ?
                (double) totalOriginalBytes / sessionDurationSec : 0;
        double optimizedBytesPerSec = (sessionDurationSec > 0) ?
                (double) totalOptimizedBytes / sessionDurationSec : 0;


        double originalMB = totalOriginalBytes / (1024.0 * 1024.0);
        double optimizedMB = totalOptimizedBytes / (1024.0 * 1024.0);
        double savedMB = savedBytes / (1024.0 * 1024.0);
        double originalKBPerSec = originalBytesPerSec / 1024.0;
        double optimizedKBPerSec = optimizedBytesPerSec / 1024.0;


        CERES_DEBUG_LINES.add(String.format("[Ceres] Overall: %s MB → %s MB (%.1f%% ratio, saved %s MB)",
                DECIMAL_FORMAT.format(originalMB),
                DECIMAL_FORMAT.format(optimizedMB),
                optimizationRatio * 100,
                DECIMAL_FORMAT.format(savedMB)));


        CERES_DEBUG_LINES.add(String.format("[Ceres] Bandwidth: %s KB/s → %s KB/s (%.1f%% reduction)",
                DECIMAL_FORMAT.format(originalKBPerSec),
                DECIMAL_FORMAT.format(optimizedKBPerSec),
                (1 - optimizationRatio) * 100));


        String sessionTime = formatDuration(sessionDurationSec);
        CERES_DEBUG_LINES.add(String.format("[Ceres] Session: %s, %d packets, %d filtered (%.1f%%)",
                sessionTime,
                totalPackets,
                totalFilteredPackets,
                (totalPackets > 0) ? (totalFilteredPackets * 100.0 / totalPackets) : 0));
    }

    /**
     * 格式化持续时间
     *
     * @param seconds 秒数
     * @return 格式化的持续时间字符串
     */
    private static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    /**
     * 检查是否有网络活动
     */
    private static boolean checkNetworkActivity() {
        boolean hasCompressedPackets = !PacketCompressor.getCompressionStats().startsWith("No packets");
        long filteredPackets = DuplicatePacketFilter.getFilteredPacketsCount();
        long sentPackets = 0;

        if (CeresConfig.COMMON.optimizationMode.get() == CeresConfig.OptimizationMode.MODERN) {
            sentPackets = NettyOptimizer.getSentPacketsCount();
        }


        return hasCompressedPackets || filteredPackets > 0 || sentPackets > 0;
    }

    /**
     * 渲染调试信息事件处理
     */
    @SubscribeEvent
    public static void onRenderDebugInfo(CustomizeGuiOverlayEvent.DebugText event) {
        if (!CeresConfig.CLIENT.showNetworkStats.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.renderDebug) {
            event.getLeft().add("");
            event.getLeft().add("[Ceres] Network Optimization v" + Ceres.VERSION);
            event.getLeft().addAll(CERES_DEBUG_LINES);
        }
    }

    /**
     * 重置统计信息
     * 在连接到新服务器时调用
     */
    public static void resetStats() {
        totalOriginalBytes = 0;
        totalOptimizedBytes = 0;
        totalPackets = 0;
        totalFilteredPackets = 0;
        sessionStartTime = System.currentTimeMillis();
        updateCounter = 0;
        hasNetworkActivity = false;
        CERES_DEBUG_LINES.clear();
    }
}
