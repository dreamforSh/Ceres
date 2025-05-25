package com.xinian.ceres.common.compression;

import java.nio.ByteBuffer;

/**
 * Ceres libdeflate Java工具类
 * 提供用于libdeflate操作的辅助方法
 */
class CeresLibdeflateJavaUtils {

    private CeresLibdeflateJavaUtils() {}

    /**
     * 检查数组边界是否有效
     *
     * @param backingLen 底层数组长度
     * @param userOffset 用户指定的偏移量
     * @param userLen 用户指定的长度
     * @throws IndexOutOfBoundsException 如果边界无效
     */
    static void checkBounds(int backingLen, int userOffset, int userLen) {
        if (userOffset < 0) {
            throw new IndexOutOfBoundsException("userOffset = " + userOffset);
        }
        if (userLen < 0) {
            throw new IndexOutOfBoundsException("userLen = " + userLen);
        }
        int fullRange = userLen + userOffset;
        if (fullRange > backingLen) {
            throw new IndexOutOfBoundsException(
                    "userOffset+userLen(" + fullRange + ") > backingLen(" + backingLen + ")");
        }
    }

    /**
     * 计算ByteBuffer的实际数组位置
     *
     * @param buffer ByteBuffer对象
     * @return 实际数组位置
     */
    static int byteBufferArrayPosition(ByteBuffer buffer) {
        return buffer.arrayOffset() + buffer.position();
    }

    /**
     * 检查ByteBuffer是否有效
     *
     * @param buffer 要检查的ByteBuffer
     * @throws NullPointerException 如果buffer为null
     * @throws IllegalArgumentException 如果buffer不是直接缓冲区也不是基于数组的缓冲区
     */
    static void checkBuffer(ByteBuffer buffer) {
        if (buffer == null) {
            throw new NullPointerException("Buffer cannot be null");
        }
        if (!buffer.isDirect() && !buffer.hasArray()) {
            throw new IllegalArgumentException("Buffer must be direct or have a backing array");
        }
    }

    /**
     * 安全地获取ByteBuffer的剩余字节数
     *
     * @param buffer ByteBuffer对象
     * @return 剩余字节数
     */
    static int safeRemaining(ByteBuffer buffer) {
        return buffer != null ? buffer.remaining() : 0;
    }

    /**
     * 计算压缩比率
     *
     * @param originalSize 原始大小
     * @param compressedSize 压缩后大小
     * @return 压缩比率（压缩后大小/原始大小）
     */
    static double compressionRatio(long originalSize, long compressedSize) {
        return originalSize > 0 ? (double) compressedSize / originalSize : 1.0;
    }

    /**
     * 计算压缩节省的百分比
     *
     * @param originalSize 原始大小
     * @param compressedSize 压缩后大小
     * @return 节省的百分比（1 - 压缩比率）* 100
     */
    static double compressionSavingsPercent(long originalSize, long compressedSize) {
        return (1.0 - compressionRatio(originalSize, compressedSize)) * 100.0;
    }

    /**
     * 格式化字节大小为人类可读的字符串
     *
     * @param bytes 字节数
     * @return 格式化后的字符串（例如：1.23 KB, 4.56 MB）
     */
    static String formatByteSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}

