// CeresVarIntUtil.java
package com.xinian.ceres.common.network.util;

/**
 * 变长整数工具类
 * 提供变长整数长度计算功能
 *
 * <p>将整数位数映射到变长整数所需的字节数，从0到32位。</p>
 */
public class CeresVarIntUtil {
    private static final int[] VARINT_EXACT_BYTE_LENGTHS = new int[33];

    static {
        for (int i = 0; i <= 32; ++i) {
            VARINT_EXACT_BYTE_LENGTHS[i] = (int) Math.ceil((31d - (i - 1)) / 7d);
        }
        VARINT_EXACT_BYTE_LENGTHS[32] = 1; // 0的特殊情况
    }

    /**
     * 计算整数值编码为变长整数所需的字节数
     *
     * @param value 需要计算长度的整数值
     * @return 编码为变长整数所需的字节数
     */
    public static int getVarIntLength(int value) {
        return VARINT_EXACT_BYTE_LENGTHS[Integer.numberOfLeadingZeros(value)];
    }

    /**
     * 计算最大可以用指定字节数表示的变长整数值
     *
     * @param bytes 字节数
     * @return 可表示的最大值
     */
    public static int getMaxValueForBytes(int bytes) {
        if (bytes <= 0) {
            return 0;
        }
        if (bytes >= 5) {
            return Integer.MAX_VALUE;
        }
        return (1 << (bytes * 7)) - 1;
    }

    /**
     * 检查变长整数是否有效
     *
     * @param value 要检查的值
     * @param maxBytes 最大允许字节数
     * @return 如果变长整数有效则为true
     */
    public static boolean isValidVarInt(int value, int maxBytes) {
        return getVarIntLength(value) <= maxBytes;
    }
}

