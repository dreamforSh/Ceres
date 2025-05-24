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

@Mod.EventBusSubscriber(modid = Ceres.MOD_ID, value = Dist.CLIENT)
public class DebugOverlay {

    private static final List<String> CERES_DEBUG_LINES = new ArrayList<>();
    private static int updateCounter = 0;
    private static boolean hasNetworkActivity = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.side != LogicalSide.CLIENT || event.phase != TickEvent.Phase.END) {
            return;
        }

        // 每20刻更新一次统计信息（约1秒）
        if (updateCounter++ % 20 == 0) {
            updateDebugInfo();
        }
    }

    private static void updateDebugInfo() {
        CERES_DEBUG_LINES.clear();

        // 添加模式信息
        CERES_DEBUG_LINES.add("[Ceres] Mode: " + CeresConfig.COMMON.optimizationMode.get());

        // 检查是否有网络活动
        hasNetworkActivity = checkNetworkActivity();

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
            CERES_DEBUG_LINES.add("[Ceres] " + PacketCompressor.getCompressionStats());
            CERES_DEBUG_LINES.add("[Ceres] " + DuplicatePacketFilter.getStats());

            if (CeresConfig.COMMON.optimizationMode.get() == CeresConfig.OptimizationMode.MODERN) {
                CERES_DEBUG_LINES.add("[Ceres] " + NettyOptimizer.getNetworkStats());
            }
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

        // 如果有任何一种网络活动，返回true
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
}