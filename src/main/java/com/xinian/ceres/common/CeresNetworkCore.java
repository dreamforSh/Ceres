package com.xinian.ceres.common;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.common.network.compression.CeresMinecraftCompressorFactory;
import com.xinian.ceres.common.network.pipeline.CeresMinecraftCipherFactory;
import com.xinian.ceres.common.network.util.CeresNatives;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ceres网络优化核心类
 * 提供网络传输的底层优化
 */
public class CeresNetworkCore {
    private static final Logger LOGGER = LogManager.getLogger(CeresNetworkCore.class);

    static {
        // 默认情况下，Netty为PooledByteBufAllocator分配16MiB的区域。这对于Minecraft来说太多了，
        // Minecraft对数据包大小有2MiB的限制！我们使用4MiB作为更合理的默认值。
        //
        // 注意：io.netty.allocator.pageSize << io.netty.allocator.maxOrder是用于计算块大小的公式。
        // 我们将maxOrder从默认的11降低到9。（我们也使用空检查，以便用户可以自由选择其他设置）
        if (System.getProperty("io.netty.allocator.maxOrder") == null) {
            System.setProperty("io.netty.allocator.maxOrder", "9");
            LOGGER.debug("Set Netty allocator max order to 9 (4MiB arenas)");
        }

        // 设置Netty的直接内存缓存阈值
        if (System.getProperty("io.netty.maxDirectMemoryCacheSize") == null) {
            // 限制为1MB，防止过度缓存
            System.setProperty("io.netty.maxDirectMemoryCacheSize", "1048576");
            LOGGER.debug("Set Netty max direct memory cache size to 1MB");
        }
    }

    /**
     * 初始化网络核心
     * 在模组主类中调用
     */
    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(CeresNetworkCore::commonSetup);
        LOGGER.info("Ceres network core initialized");
    }

    /**
     * 通用设置事件处理
     *
     * @param event 通用设置事件
     */
    private static void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Compression will use " + CeresNatives.compress.getLoadedVariant() +
                ", encryption will use " + CeresNatives.cipher.getLoadedVariant());

        // 记录实现详情
        event.enqueueWork(() -> {
            CeresMinecraftCompressorFactory.logImplementationDetails();
            CeresMinecraftCipherFactory.logImplementationDetails();
        });
    }

    /**
     * 获取压缩实现名称
     *
     * @return 压缩实现名称
     */
    public static String getCompressionImplementation() {
        return CeresNatives.compress.getLoadedVariant();
    }

    /**
     * 获取加密实现名称
     *
     * @return 加密实现名称
     */
    public static String getEncryptionImplementation() {
        return CeresNatives.cipher.getLoadedVariant();
    }

    /**
     * 检查是否使用本地实现
     *
     * @return 如果使用本地实现则为true
     */
    public static boolean isUsingNativeImplementation() {
        return !getCompressionImplementation().equals("Java") ||
                !getEncryptionImplementation().equals("Java");
    }

    /**
     * 重置所有网络统计信息
     */
    public static void resetAllStats() {
        CeresMinecraftCompressorFactory.resetAllStats();
    }
}
