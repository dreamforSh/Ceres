package com.xinian.neptune.client;

import com.xinian.neptune.Neptune;
import com.xinian.neptune.NeptuneConfig;
import com.xinian.neptune.network.DuplicatePacketFilter;
import com.xinian.neptune.network.NettyOptimizer;
import com.xinian.neptune.network.PacketCompressor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Neptune.MOD_ID, value = Dist.CLIENT)
public class DebugOverlay {

    private static final List<String> NEPTUNE_DEBUG_LINES = new ArrayList<>();
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
        NEPTUNE_DEBUG_LINES.clear();

        // 添加模式信息
        NEPTUNE_DEBUG_LINES.add("[Neptune] Mode: " + NeptuneConfig.COMMON.optimizationMode.get());

        // 检查是否有网络活动
        hasNetworkActivity = checkNetworkActivity();

        if (!hasNetworkActivity) {
            // 如果没有网络活动，显示状态信息
            NEPTUNE_DEBUG_LINES.add("[Neptune] Status: Waiting for network activity...");

            // 添加配置信息，即使没有网络活动也能显示
            if (NeptuneConfig.COMMON.enableCompression.get()) {
                NEPTUNE_DEBUG_LINES.add("[Neptune] Compression: Enabled (Threshold: " +
                        NeptuneConfig.COMMON.compressionThreshold.get() + " bytes, Level: " +
                        NeptuneConfig.COMMON.compressionLevel.get() + ")");
            } else {
                NEPTUNE_DEBUG_LINES.add("[Neptune] Compression: Disabled");
            }

            if (NeptuneConfig.COMMON.enableDuplicateFiltering.get()) {
                NEPTUNE_DEBUG_LINES.add("[Neptune] Duplicate Filtering: Enabled");
            } else {
                NEPTUNE_DEBUG_LINES.add("[Neptune] Duplicate Filtering: Disabled");
            }
        } else {
            // 如果有网络活动，显示统计信息
            NEPTUNE_DEBUG_LINES.add("[Neptune] " + PacketCompressor.getCompressionStats());
            NEPTUNE_DEBUG_LINES.add("[Neptune] " + DuplicatePacketFilter.getStats());

            if (NeptuneConfig.COMMON.optimizationMode.get() == NeptuneConfig.OptimizationMode.MODERN) {
                NEPTUNE_DEBUG_LINES.add("[Neptune] " + NettyOptimizer.getNetworkStats());
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
        if (NeptuneConfig.COMMON.optimizationMode.get() == NeptuneConfig.OptimizationMode.MODERN) {
            sentPackets = NettyOptimizer.getSentPacketsCount();
        }

        // 如果有任何一种网络活动，返回true
        return hasCompressedPackets || filteredPackets > 0 || sentPackets > 0;
    }

    @SubscribeEvent
    public static void onRenderDebugInfo(CustomizeGuiOverlayEvent.DebugText event) {
        if (!NeptuneConfig.CLIENT.showNetworkStats.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.renderDebug) {
            event.getLeft().add("");
            event.getLeft().add("[Neptune] Network Optimization v" + Neptune.VERSION);
            event.getLeft().addAll(NEPTUNE_DEBUG_LINES);
        }
    }
}
