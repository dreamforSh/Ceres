package com.xinian.ceres.client;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import com.xinian.ceres.common.compression.CeresCompressionManager;
import com.xinian.ceres.common.compression.CeresLibdeflate;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Ceres.MOD_ID, value = Dist.CLIENT)
public class DebugOverlay {

    private static final List<String> CERES_DEBUG_LINES = new ArrayList<>();
    private static int updateCounter = 0;
    private static boolean hasNetworkActivity = false;


    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#,##0.0");


    private static long totalOriginalBytes = 0;
    private static long totalOptimizedBytes = 0;
    private static long totalPackets = 0;
    private static long totalFilteredPackets = 0;
    private static long sessionStartTime = System.currentTimeMillis();


    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.side != LogicalSide.CLIENT || event.phase != TickEvent.Phase.END) {
            return;
        }


        int updateFrequency = CeresConfig.CLIENT.statsUpdateFrequency.get();
        if (updateCounter++ % updateFrequency == 0) {
            updateDebugInfo();
        }
    }


    private static void updateDebugInfo() {
        CERES_DEBUG_LINES.clear();


        CERES_DEBUG_LINES.add("[Ceres] Mode: " + CeresConfig.COMMON.optimizationMode.get() +
                ", Engine: " + CeresConfig.COMMON.compressionEngine.get());


        hasNetworkActivity = checkNetworkActivity();


        updateStatistics();

        if (!hasNetworkActivity) {

            CERES_DEBUG_LINES.add("[Ceres] Status: Waiting for network activity...");


            if (CeresConfig.COMMON.enableCompression.get()) {
                CERES_DEBUG_LINES.add("[Ceres] Compression: Enabled (Threshold: " +
                        CeresConfig.COMMON.minPacketSizeToCompress.get() + " bytes, Format: " +
                        CeresConfig.COMMON.compressionFormat.get() + ")");


                String levelInfo;
                if (CeresConfig.COMMON.compressionEngine.get() == CeresConfig.CompressionEngine.LIBDEFLATE &&
                        CeresLibdeflate.isAvailable()) {
                    levelInfo = "Level: " + CeresConfig.COMMON.advancedCompressionLevel.get() + " (libdeflate)";
                } else {
                    levelInfo = "Level: " + CeresConfig.COMMON.compressionLevel.get() + " (Java)";
                }
                CERES_DEBUG_LINES.add("[Ceres] " + levelInfo);
            } else {
                CERES_DEBUG_LINES.add("[Ceres] Compression: Disabled");
            }

            if (CeresConfig.COMMON.enableDuplicateFiltering.get()) {
                CERES_DEBUG_LINES.add("[Ceres] Duplicate Filtering: Enabled");
            } else {
                CERES_DEBUG_LINES.add("[Ceres] Duplicate Filtering: Disabled");
            }
        } else {



            CERES_DEBUG_LINES.add("[Ceres] " + PacketCompressor.getCompressionStats());


            CERES_DEBUG_LINES.add("[Ceres] " + DuplicatePacketFilter.getStats());


            if (CeresConfig.COMMON.optimizationMode.get() == CeresConfig.OptimizationMode.MODERN) {
                CERES_DEBUG_LINES.add("[Ceres] " + NettyOptimizer.getNetworkStats());
            }


            addOverallStatistics();
        }
    }


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

            }
        }


        totalFilteredPackets = DuplicatePacketFilter.getFilteredPacketsCount();


        if (CeresConfig.COMMON.optimizationMode.get() == CeresConfig.OptimizationMode.MODERN) {
            totalPackets = NettyOptimizer.getSentPacketsCount();
        }
    }


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


    private static boolean checkNetworkActivity() {
        boolean hasCompressedPackets = !PacketCompressor.getCompressionStats().startsWith("No packets");
        long filteredPackets = DuplicatePacketFilter.getFilteredPacketsCount();
        long sentPackets = 0;

        if (CeresConfig.COMMON.optimizationMode.get() == CeresConfig.OptimizationMode.MODERN) {
            sentPackets = NettyOptimizer.getSentPacketsCount();
        }


        return hasCompressedPackets || filteredPackets > 0 || sentPackets > 0;
    }


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
