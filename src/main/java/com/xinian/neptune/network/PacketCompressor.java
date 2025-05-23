package com.xinian.neptune.network;

import com.xinian.neptune.Neptune;
import com.xinian.neptune.NeptuneConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 数据包压缩工具类
 * 负责压缩和解压网络数据包
 */
public class PacketCompressor {
    // 统计信息
    private static long totalBytesBeforeCompression = 0;
    private static long totalBytesAfterCompression = 0;
    private static int packetsCompressed = 0;

    /**
     * 压缩数据
     *
     * @param data 原始数据
     * @return 压缩后的数据，如果不需要压缩则返回原始数据
     */
    public static byte[] compressData(byte[] data) {
        // 检查是否启用压缩
        if (!NeptuneConfig.COMMON.enableCompression.get()) {
            return data;
        }

        // 检查数据大小是否达到压缩阈值
        int threshold = NeptuneConfig.COMMON.compressionThreshold.get();
        if (data.length < threshold) {
            return data;
        }

        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(data.length);

            // 创建GZIP输出流，设置压缩级别
            int level = NeptuneConfig.COMMON.compressionLevel.get();
            GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream) {
                {
                    def.setLevel(level);
                }
            };

            // 写入数据并关闭流
            gzipStream.write(data);
            gzipStream.close();

            // 获取压缩后的数据
            byte[] compressedData = byteStream.toByteArray();

            // 更新统计信息
            synchronized (PacketCompressor.class) {
                totalBytesBeforeCompression += data.length;
                totalBytesAfterCompression += compressedData.length;
                packetsCompressed++;
            }

            // 如果压缩后的数据更大，则返回原始数据
            if (compressedData.length >= data.length) {
                if (NeptuneConfig.COMMON.enableLogging.get()) {
                    Neptune.LOGGER.debug("Compression ineffective for {} bytes, skipping", data.length);
                }
                return data;
            }

            if (NeptuneConfig.COMMON.enableLogging.get()) {
                Neptune.LOGGER.debug("Compressed {} bytes to {} bytes ({}% reduction)",
                        data.length, compressedData.length,
                        Math.round((1 - (float)compressedData.length / data.length) * 100));
            }

            return compressedData;
        } catch (IOException e) {
            Neptune.LOGGER.error("Failed to compress data: {}", e.getMessage());
            return data;
        }
    }

    /**
     * 解压数据
     *
     * @param compressedData 压缩后的数据
     * @param isCompressed 数据是否已压缩
     * @return 解压后的数据
     */
    public static byte[] decompressData(byte[] compressedData, boolean isCompressed) {
        if (!isCompressed) {
            return compressedData;
        }

        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(compressedData);
            GZIPInputStream gzipStream = new GZIPInputStream(byteStream);

            // 读取解压后的数据
            ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = gzipStream.read(buffer)) > 0) {
                resultStream.write(buffer, 0, length);
            }

            gzipStream.close();
            resultStream.close();

            byte[] decompressedData = resultStream.toByteArray();

            if (NeptuneConfig.COMMON.enableLogging.get()) {
                Neptune.LOGGER.debug("Decompressed {} bytes to {} bytes",
                        compressedData.length, decompressedData.length);
            }

            return decompressedData;
        } catch (IOException e) {
            Neptune.LOGGER.error("Failed to decompress data: {}", e.getMessage());
            return compressedData;
        }
    }

    /**
     * 获取压缩统计信息
     *
     * @return 压缩统计信息字符串
     */
    public static String getCompressionStats() {
        if (packetsCompressed == 0) {
            return "No packets compressed yet";
        }

        double compressionRatio = (double) totalBytesAfterCompression / totalBytesBeforeCompression;
        double savedBytes = totalBytesBeforeCompression - totalBytesAfterCompression;

        return String.format(
                "Compressed %d packets, %d KB → %d KB (%.1f%% reduction, saved %.1f KB)",
                packetsCompressed,
                totalBytesBeforeCompression / 1024,
                totalBytesAfterCompression / 1024,
                (1 - compressionRatio) * 100,
                savedBytes / 1024
        );
    }

    /**
     * 重置统计信息
     */
    public static void resetStats() {
        totalBytesBeforeCompression = 0;
        totalBytesAfterCompression = 0;
        packetsCompressed = 0;
    }
}

