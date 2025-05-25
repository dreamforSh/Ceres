package com.xinian.ceres.common.network.compression;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import com.xinian.ceres.common.network.util.CeresNatives.CeresCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Minecraft网络数据解压解码器
 * 使用CeresCompressor对入站数据进行解压处理
 */
public class CeresMinecraftCompressDecoder extends ByteToMessageDecoder {

    private static final int UNCOMPRESSED_CAP = 8 * 1024 * 1024; // 8MiB
    private static final AtomicLong TOTAL_COMPRESSED_BYTES = new AtomicLong(0);
    private static final AtomicLong TOTAL_UNCOMPRESSED_BYTES = new AtomicLong(0);
    private static final AtomicLong PACKETS_DECOMPRESSED = new AtomicLong(0);

    private int threshold;
    private final boolean validate;
    private final CeresCompressor compressor;

    /**
     * 创建一个新的Minecraft解压解码器
     *
     * @param threshold 压缩阈值（字节数）
     * @param validate 是否验证压缩数据
     * @param compressor 用于解压的压缩器
     */
    public CeresMinecraftCompressDecoder(int threshold, boolean validate, CeresCompressor compressor) {
        this.threshold = threshold;
        this.validate = validate;
        this.compressor = compressor;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() != 0) {
            FriendlyByteBuf packetBuf = new FriendlyByteBuf(in);
            int claimedUncompressedSize = packetBuf.readVarInt();

            if (claimedUncompressedSize == 0) {

                ByteBuf uncompressedData = packetBuf.readBytes(packetBuf.readableBytes());
                out.add(uncompressedData);


                if (CeresConfig.COMMON.enableLogging.get()) {
                    TOTAL_UNCOMPRESSED_BYTES.addAndGet(uncompressedData.readableBytes());
                }
            } else {
                if (validate) {
                    if (claimedUncompressedSize < this.threshold) {
                        throw new DecoderException("Badly compressed packet - size of " + claimedUncompressedSize + " is below server threshold of " + this.threshold);
                    }

                    if (claimedUncompressedSize > UNCOMPRESSED_CAP) {
                        throw new DecoderException("Badly compressed packet - size of " + claimedUncompressedSize + " is larger than maximum of " + UNCOMPRESSED_CAP);
                    }
                }

                int compressedSize = packetBuf.readableBytes();


                ByteBuf uncompressed = ctx.alloc().buffer(claimedUncompressedSize);
                try {

                    compressor.inflate(in, uncompressed, claimedUncompressedSize);
                    out.add(uncompressed);
                    in.clear();


                    if (CeresConfig.COMMON.enableLogging.get()) {
                        TOTAL_COMPRESSED_BYTES.addAndGet(compressedSize);
                        TOTAL_UNCOMPRESSED_BYTES.addAndGet(claimedUncompressedSize);
                        PACKETS_DECOMPRESSED.incrementAndGet();

                        if (PACKETS_DECOMPRESSED.get() % 1000 == 0) {
                            logCompressionStats();
                        }
                    }
                } catch (Exception e) {
                    uncompressed.release();
                    throw e;
                }
            }
        }
    }

    /**
     * 设置压缩阈值
     *
     * @param threshold 新的压缩阈值（字节数）
     */
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
        compressor.close();
    }

    /**
     * 获取当前压缩阈值
     *
     * @return 压缩阈值（字节数）
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * 获取压缩器实例
     *
     * @return 压缩器
     */
    public CeresCompressor getCompressor() {
        return compressor;
    }

    /**
     * 记录压缩统计信息
     */
    private void logCompressionStats() {
        long compressed = TOTAL_COMPRESSED_BYTES.get();
        long uncompressed = TOTAL_UNCOMPRESSED_BYTES.get();
        long packets = PACKETS_DECOMPRESSED.get();

        if (compressed > 0 && uncompressed > 0) {
            double ratio = (double) compressed / uncompressed;
            Ceres.LOGGER.debug("Decompression stats: {} packets, {}/{} bytes, ratio: {:.2f}%",
                    packets, compressed, uncompressed, ratio * 100);
        }
    }

    /**
     * 获取解压统计信息
     *
     * @return 统计信息字符串
     */
    public static String getDecompressionStats() {
        long compressed = TOTAL_COMPRESSED_BYTES.get();
        long uncompressed = TOTAL_UNCOMPRESSED_BYTES.get();
        long packets = PACKETS_DECOMPRESSED.get();

        if (compressed == 0 || uncompressed == 0) {
            return "No packets decompressed yet";
        }

        double ratio = (double) compressed / uncompressed;
        double saved = uncompressed - compressed;

        return String.format(
                "Decompressed %d packets, %d KB → %d KB (%.1f%% ratio, saved %.1f KB)",
                packets,
                compressed / 1024,
                uncompressed / 1024,
                ratio * 100,
                saved / 1024
        );
    }

    /**
     * 重置统计信息
     */
    public static void resetStats() {
        TOTAL_COMPRESSED_BYTES.set(0);
        TOTAL_UNCOMPRESSED_BYTES.set(0);
        PACKETS_DECOMPRESSED.set(0);
    }
}
