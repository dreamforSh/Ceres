package com.xinian.ceres.mixin.network.pipeline.compression;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import com.xinian.ceres.common.network.compression.CeresMinecraftCompressDecoder;
import com.xinian.ceres.common.network.compression.CeresMinecraftCompressEncoder;
import com.xinian.ceres.common.network.compression.CeresMinecraftCompressorFactory;
import com.xinian.ceres.common.network.util.CeresNatives;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 连接压缩Mixin
 * 替换Minecraft的压缩处理为Ceres的优化版本
 */
@Mixin(Connection.class)
public class CeresConnectionCompressionMixin {
    @Shadow
    private Channel channel;

    /**
     * 注入到setupCompression方法，替换为Ceres的压缩实现
     *
     * @param compressionThreshold 压缩阈值
     * @param validate 是否验证压缩数据
     * @param ci 回调信息
     */
    @Inject(method = "setupCompression", at = @At("HEAD"), cancellable = true)
    public void setCompressionThreshold(int compressionThreshold, boolean validate, CallbackInfo ci) {
        if (!CeresConfig.COMMON.enableCompression.get()) {
            // 如果在配置中禁用了压缩，则使用原始方法
            return;
        }

        try {
            if (compressionThreshold == -1) {
                // 禁用压缩
                if (this.channel.pipeline().get("decompress") != null) {
                    this.channel.pipeline().remove("decompress");
                }
                if (this.channel.pipeline().get("compress") != null) {
                    this.channel.pipeline().remove("compress");
                }

                Ceres.LOGGER.debug("Disabled packet compression");
            } else {
                // 获取或创建压缩处理器
                CeresMinecraftCompressDecoder decoder = (CeresMinecraftCompressDecoder) channel.pipeline()
                        .get("decompress");
                CeresMinecraftCompressEncoder encoder = (CeresMinecraftCompressEncoder) channel.pipeline()
                        .get("compress");

                if (decoder != null && encoder != null) {
                    // 更新现有处理器的阈值
                    decoder.setThreshold(compressionThreshold);
                    encoder.setThreshold(compressionThreshold);

                    Ceres.LOGGER.debug("Updated compression threshold to {} bytes", compressionThreshold);
                } else {
                    // 创建新的压缩处理器
                    int level = CeresConfig.COMMON.compressionLevel.get();

                    // 使用CeresNatives创建压缩器
                    CeresNatives.CeresCompressor compressor = CeresNatives.compress.create(level);

                    // 创建编码器和解码器
                    encoder = new CeresMinecraftCompressEncoder(compressionThreshold, compressor);
                    decoder = new CeresMinecraftCompressDecoder(compressionThreshold, validate, compressor);

                    // 添加到管道
                    channel.pipeline().addBefore("decoder", "decompress", decoder);
                    channel.pipeline().addBefore("encoder", "compress", encoder);

                    Ceres.LOGGER.debug("Enabled packet compression with threshold {} bytes and level {}",
                            compressionThreshold, level);

                    // 记录压缩器实现信息
                    if (CeresConfig.COMMON.enableLogging.get()) {
                        CeresMinecraftCompressorFactory.logImplementationDetails();
                    }
                }
            }

            // 取消原始方法执行
            ci.cancel();
        } catch (Exception e) {
            Ceres.LOGGER.error("Failed to setup Ceres compression, falling back to vanilla: {}", e.getMessage());
            if (CeresConfig.COMMON.enableLogging.get()) {
                e.printStackTrace();
            }
            // 不取消回调，让原始方法处理
        }
    }

    /**
     * 获取当前压缩阈值
     *
     * @return 压缩阈值，如果未启用压缩则返回-1
     */
    public int getCurrentCompressionThreshold() {
        CeresMinecraftCompressEncoder encoder = (CeresMinecraftCompressEncoder) channel.pipeline()
                .get("compress");
        return encoder != null ? encoder.getThreshold() : -1;
    }

    /**
     * 获取当前压缩级别
     *
     * @return 压缩级别，如果未启用压缩则返回-1
     */
    public int getCurrentCompressionLevel() {
        return CeresConfig.COMMON.compressionLevel.get();
    }
}

