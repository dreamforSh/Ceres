package com.xinian.ceres.common.compression;

import java.nio.ByteBuffer;
import java.util.zip.Checksum;

/**
 * 等同于{@link java.util.zip.Adler32}，但使用libdeflate的Adler-32例程。
 * 因此，这个类的性能可能比JDK版本更好。
 */
public class CeresLibdeflateAdler32 implements Checksum {
    static {
        CeresLibdeflate.ensureAvailable();
    }

    private int adler32 = 1;

    @Override
    public void update(int b) {
        byte[] tmp = new byte[] {(byte) b};
        adler32 = adler32Heap(adler32, tmp, 0, 1);
    }

    public void update(byte[] b) {
        adler32 = adler32Heap(adler32, b, 0, b.length);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        checkBounds(b.length, off, len);
        adler32 = adler32Heap(adler32, b, off, len);
    }

    public void update(ByteBuffer buffer) {
        int pos = buffer.position();
        int limit = buffer.limit();
        int remaining = limit - pos;
        if (buffer.hasArray()) {
            adler32 = adler32Heap(adler32, buffer.array(), byteBufferArrayPosition(buffer), remaining);
        } else if (buffer.isDirect()) {
            adler32 = adler32Direct(adler32, buffer, pos, remaining);
        } else {
            // 复制这个数组
            byte[] data = new byte[remaining];
            buffer.get(data);
            adler32 = adler32Heap(adler32, data, 0, data.length);
        }
        buffer.position(limit);
    }

    @Override
    public long getValue() {
        return ((long) adler32 & 0xffffffffL);
    }

    @Override
    public void reset() {
        adler32 = 1;
    }

    /**
     * 检查边界
     * @param arrLength 数组长度
     * @param off 偏移量
     * @param len 长度
     */
    private static void checkBounds(int arrLength, int off, int len) {
        if (off < 0 || len < 0 || off + len > arrLength) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * 获取ByteBuffer的数组位置
     * @param buffer ByteBuffer
     * @return 数组位置
     */
    private static int byteBufferArrayPosition(ByteBuffer buffer) {
        return buffer.position() + buffer.arrayOffset();
    }

    private static native int adler32Heap(long adler32, byte[] array, int off, int len);

    private static native int adler32Direct(long adler32, ByteBuffer buf, int off, int len);
}

