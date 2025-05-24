package com.xinian.ceres.common.network.compression;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import com.xinian.ceres.common.network.util.CeresNatives;
import com.xinian.ceres.common.network.util.CeresNatives.CeresCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.FriendlyByteBuf;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Minecraft网络数据压缩编码器
 * 使用CeresCompressor对出站数据进行压缩处理
 */
public class CeresMinecraftCompressEncoder extends MessageToByteEncoder<ByteBuf> {

    private static final AtomicLong TOTAL_UNCOMPRESSED_BYTES = new AtomicLong(0);
    private static final AtomicLong TOTAL_COMPRESSED_BYTES = new AtomicLong(0);
    private static final AtomicLong PACKETS_COMPRESSED = new AtomicLong(0);
    private static final AtomicLong PACKETS_SKIPPED = new AtomicLong(0);

    private int threshold;
    private final CeresCompressor compressor;

    /**
     * 创建一个新的Minecraft压缩编码器
     *
     * @param threshold 压缩阈值（字节数）
     * @param compressor 用于压缩的压缩器
     */
    public CeresMinecraftCompressEncoder(int threshold, CeresCompressor compressor) {
        this.threshold = threshold;
        this.compressor = compressor;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        FriendlyByteBuf wrappedBuf = new FriendlyByteBuf(out);
        int uncompressedSize = msg.readableBytes();

        if (uncompressedSize < threshold) {
            // 小于阈值的数据包不压缩
            wrappedBuf.writeVarInt(0);
            out.writeBytes(msg);

            // 更新统计信息
            if (CeresConfig.COMMON.enableLogging.get()) {
                TOTAL_UNCOMPRESSED_BYTES.addAndGet(uncompressedSize);
                PACKETS_SKIPPED.incrementAndGet();
            }
        } else {
            // 写入未压缩大小
            wrappedBuf.writeVarInt(uncompressedSize);

            // 压缩数据
            int startIndex = out.writerIndex();

            // 读取源数据
            byte[] sourceData = new byte[msg.readableBytes()];
            msg.readBytes(sourceData);

            // 压缩数据
            compressor.deflate(msg, out);

            // 更新统计信息
            if (CeresConfig.COMMON.enableLogging.get()) {
                int compressedSize = out.writerIndex() - startIndex;
                TOTAL_UNCOMPRESSED_BYTES.addAndGet(uncompressedSize);
                TOTAL_COMPRESSED_BYTES.addAndGet(compressedSize);
                PACKETS_COMPRESSED.incrementAndGet();

                if (PACKETS_COMPRESSED.get() % 1000 == 0) {
                    logCompressionStats();
                }
            }
        }
    }

    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect)
            throws Exception {
        // 我们分配要压缩的字节数加1字节。这涵盖了两种情况：
        //
        // - 压缩
        //    根据经验，如果数据压缩良好，压缩后的大小通常小于原始大小。
        //    但在最坏情况下，压缩后的数据可能比原始数据大。
        // - 未压缩
        //    这是相当明显的 - 我们将比未压缩大小多一个。
        int initialBufferSize = msg.readableBytes() + 1;
        return preferDirect ?
                ctx.alloc().directBuffer(initialBufferSize) :
                ctx.alloc().heapBuffer(initialBufferSize);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        compressor.close();
    }

    /**
     * 设置压缩阈值
     *
     * @param threshold 新的压缩阈值（字节数）
     */
    public void setThreshold(int threshold) {
        this.threshold = threshold;
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
        long uncompressed = TOTAL_UNCOMPRESSED_BYTES.get();
        long compressed = TOTAL_COMPRESSED_BYTES.get();
        long packetsCompressed = PACKETS_COMPRESSED.get();
        long packetsSkipped = PACKETS_SKIPPED.get();

        if (compressed > 0 && uncompressed > 0) {
            double ratio = (double) compressed / uncompressed;
            Ceres.LOGGER.debug("Compression stats: {} compressed, {} skipped, {}/{} bytes, ratio: {:.2f}%",
                    packetsCompressed, packetsSkipped, compressed, uncompressed, ratio * 100);
        }
    }

    /**
     * 获取压缩统计信息
     *
     * @return 统计信息字符串
     */
    public static String getCompressionStats() {
        long uncompressed = TOTAL_UNCOMPRESSED_BYTES.get();
        long compressed = TOTAL_COMPRESSED_BYTES.get();
        long packetsCompressed = PACKETS_COMPRESSED.get();
        long packetsSkipped = PACKETS_SKIPPED.get();

        if (compressed == 0 || uncompressed == 0) {
            return "No packets compressed yet";
        }

        double ratio = (double) compressed / uncompressed;
        double saved = uncompressed - compressed;

        return String.format(
                "Compressed %d packets (%d skipped), %d KB → %d KB (%.1f%% ratio, saved %.1f KB)",
                packetsCompressed,
                packetsSkipped,
                uncompressed / 1024,
                compressed / 1024,
                ratio * 100,
                saved / 1024
        );
    }

    /**
     * 重置统计信息
     */
    public static void resetStats() {
        TOTAL_UNCOMPRESSED_BYTES.set(0);
        TOTAL_COMPRESSED_BYTES.set(0);
        PACKETS_COMPRESSED.set(0);
        PACKETS_SKIPPED.set(0);
    }
}
