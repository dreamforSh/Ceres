package com.xinian.ceres.common.network.compression;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import com.xinian.ceres.common.network.util.CeresNatives;
import com.xinian.ceres.common.network.util.CeresNatives.CeresCompressor;

/**
 * Minecraft压缩组件工厂
 * 用于创建和管理压缩/解压组件
 */
public class CeresMinecraftCompressorFactory {
    private static final CeresNatives.CompressorFactory COMPRESSOR_FACTORY = CeresNatives.compress;

    /**
     * 创建压缩编码器
     *
     * @return 压缩编码器
     */
    public static CeresMinecraftCompressEncoder createEncoder() {
        int threshold = CeresConfig.COMMON.compressionThreshold.get();
        int level = CeresConfig.COMMON.compressionLevel.get();
        CeresCompressor compressor = COMPRESSOR_FACTORY.create(level);
        return new CeresMinecraftCompressEncoder(threshold, compressor);
    }

    /**
     * 创建解压解码器
     *
     * @param validate 是否验证压缩数据
     * @return 解压解码器
     */
    public static CeresMinecraftCompressDecoder createDecoder(boolean validate) {
        int threshold = CeresConfig.COMMON.compressionThreshold.get();
        CeresCompressor compressor = COMPRESSOR_FACTORY.create(0); // 解压不需要级别
        return new CeresMinecraftCompressDecoder(threshold, validate, compressor);
    }

    /**
     * 获取当前使用的压缩器工厂
     *
     * @return 压缩器工厂
     */
    public static CeresNatives.CompressorFactory getCompressorFactory() {
        return COMPRESSOR_FACTORY;
    }

    /**
     * 获取当前使用的压缩器实现名称
     *
     * @return 压缩器实现名称
     */
    public static String getCompressorImplementationName() {
        return COMPRESSOR_FACTORY.getClass().getSimpleName();
    }

    /**
     * 记录当前使用的压缩器实现信息
     */
    public static void logImplementationDetails() {
        Ceres.LOGGER.info("Using compressor implementation: {}", COMPRESSOR_FACTORY.getLoadedVariant());
        Ceres.LOGGER.info("Compression threshold: {} bytes, level: {}",
                CeresConfig.COMMON.compressionThreshold.get(),
                CeresConfig.COMMON.compressionLevel.get());
    }

    /**
     * 重置所有压缩统计信息
     */
    public static void resetAllStats() {
        CeresMinecraftCompressEncoder.resetStats();
        CeresMinecraftCompressDecoder.resetStats();
    }
}
