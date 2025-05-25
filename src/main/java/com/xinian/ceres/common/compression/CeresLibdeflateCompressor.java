package com.xinian.ceres.common.compression;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;

/**
 * 表示一个{@code libdeflate}压缩器。
 * 这个类包含用于字节数组、NIO ByteBuffer的压缩方法，
 * 以及计算给定压缩格式和字节数的最大可能边界的能力。
 *
 * <p><strong>线程安全性</strong>：libdeflate压缩器不是线程安全的，
 * 但每个线程使用多个压缩器是允许的。
 */
public class CeresLibdeflateCompressor implements Closeable, AutoCloseable {
    private static final int MINIMUM_COMPRESSION_LEVEL = 0;
    private static final int MAXIMUM_COMPRESSION_LEVEL = 12;
    private static final int DEFAULT_COMPRESSION_LEVEL = 6;

    static {
        CeresLibdeflate.ensureAvailable();
    }

    final long ctx;
    private boolean closed = false;

    /** 使用默认压缩级别创建一个新的压缩器。 */
    public CeresLibdeflateCompressor() {
        this(Deflater.DEFAULT_COMPRESSION);
    }

    /**
     * 使用指定的压缩级别创建一个新的压缩器。
     *
     * @param level 要使用的压缩级别，从0到12
     * @throws IllegalArgumentException 如果级别不在范围内
     */
    public CeresLibdeflateCompressor(int level) {
        if (level == Deflater.DEFAULT_COMPRESSION) {
            level = DEFAULT_COMPRESSION_LEVEL;
        }
        if (level < MINIMUM_COMPRESSION_LEVEL || level > MAXIMUM_COMPRESSION_LEVEL) {
            throw new IllegalArgumentException("invalid compression level, must be between 0 and 12");
        }
        this.ctx = allocate(level);
    }

    void ensureNotClosed() {
        if (this.closed) {
            throw new IllegalStateException("Compressor already closed.");
        }
    }

    /**
     * 将{@code in}数组的全部内容压缩到{@code out}数组中。
     *
     * @param in 要压缩的源数组
     * @param out 将保存压缩数据的目标数组
     * @param type 要使用的压缩容器
     * @return 一个正的非零整数，表示压缩输出的大小，如果给定的输出缓冲区太小，则为零
     */
    public int compress(byte[] in, byte[] out, CeresCompressionType type) {
        ensureNotClosed();
        return (int) compressBothHeap(ctx, in, 0, in.length, out, 0, out.length, type.getNativeType());
    }

    /**
     * 将给定的{@code in}数组压缩到{@code out}数组中。
     *
     * @param in 要压缩的源数组
     * @param inOff 源数组的偏移量
     * @param inLen 从偏移量开始的源数组长度
     * @param out 将保存压缩数据的目标数组
     * @param outOff 源数组的偏移量
     * @param outLen 从{@code outOff}开始的源数组长度
     * @param type 要使用的压缩容器
     * @return 一个正的非零整数，表示压缩输出的大小，如果给定的输出缓冲区太小，则为零
     * @throws IllegalArgumentException 如果给定的偏移量和长度超出范围或表示负范围
     */
    public int compress(
            byte[] in, int inOff, int inLen, byte[] out, int outOff, int outLen, CeresCompressionType type) {
        ensureNotClosed();
        checkBounds(in.length, inOff, inLen);
        checkBounds(out.length, outOff, outLen);
        return (int) compressBothHeap(ctx, in, inOff, inLen, out, outOff, outLen, type.getNativeType());
    }

    /**
     * 将给定的{@code in} ByteBuffer压缩到{@code out} ByteBuffer中。
     * 当压缩操作完成时，输出缓冲区的{@code position}将增加产生的字节数，
     * 输入{@code position}将增加剩余的字节数。
     *
     * @param in 要压缩的源字节缓冲区
     * @param out 将保存压缩数据的目标缓冲区
     * @param type 要使用的压缩容器
     * @return 一个正的非零整数，表示压缩输出的大小，如果给定的输出缓冲区太小，则为零
     */
    public int compress(ByteBuffer in, ByteBuffer out, CeresCompressionType type) {
        ensureNotClosed();
        int nativeType = type.getNativeType();


        long result;
        int inAvail = in.remaining();
        if (in.isDirect()) {
            if (out.isDirect()) {
                result =
                        compressBothDirect(
                                ctx, in, in.position(), inAvail, out, out.position(), out.remaining(), nativeType);
            } else {
                result =
                        compressOnlySourceDirect(
                                ctx,
                                in,
                                in.position(),
                                inAvail,
                                out.array(),
                                byteBufferArrayPosition(out),
                                out.remaining(),
                                nativeType);
            }
        } else {
            int inPos = byteBufferArrayPosition(in);
            if (out.isDirect()) {
                result =
                        compressOnlyDestinationDirect(
                                ctx, in.array(), inPos, inAvail, out, out.position(), out.remaining(), nativeType);
            } else {
                result =
                        compressBothHeap(
                                ctx,
                                in.array(),
                                inPos,
                                inAvail,
                                out.array(),
                                byteBufferArrayPosition(out),
                                out.remaining(),
                                nativeType);
            }
        }

        out.position((int) (out.position() + result));
        in.position(in.position() + inAvail);
        return (int) result;
    }

    /** 关闭压缩器。对压缩器的任何进一步操作都将失败。 */
    @Override
    public void close() {
        ensureNotClosed();
        free(this.ctx);
        this.closed = true;
    }

    /**
     * 返回使用此指定压缩器和指定格式压缩长度小于或等于{@code count}的任何缓冲区可能产生的压缩数据字节数的最坏情况上限。
     *
     * <p>请注意，在许多应用程序中，此函数不是必需的。对于基于块的压缩，通常最好单独存储每个块的未压缩大小，
     * 并以未压缩形式存储任何未压缩到小于其原始大小的块。
     *
     * @param count 计算上限的最大字节数
     * @param type 要使用的压缩类型
     * @return 上限
     */
    public long getCompressBound(long count, CeresCompressionType type) {
        ensureNotClosed();
        return getCompressBound(ctx, count, type.getNativeType());
    }

    /**
     * 返回使用libdeflate支持的任何压缩选项使用指定格式压缩长度小于或等于{@code count}的任何缓冲区可能产生的压缩数据字节数的最坏情况上限。
     *
     * <p>请注意，在许多应用程序中，此函数不是必需的。对于基于块的压缩，通常最好单独存储每个块的未压缩大小，
     * 并以未压缩形式存储任何未压缩到小于其原始大小的块。
     *
     * <p>此方法可以安全地从多个线程同时使用。
     *
     * @param count 计算上限的最大字节数
     * @param type 要使用的压缩类型
     * @return 上限
     */
    public static long getGenericCompressionBound(long count, CeresCompressionType type) {
        // 对于通用方法，我们可以传递NULL给libdeflate_*_compress_bound函数。
        return getCompressBound(0, count, type.getNativeType());
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


    private static native long allocate(int level);

    private static native void free(long ctx);

    static native long compressBothHeap(
            long ctx, byte[] in, int inPos, int inSize, byte[] out, int outPos, int outSize, int type);

    static native long compressOnlyDestinationDirect(
            long ctx,
            byte[] in,
            int inPos,
            int inSize,
            ByteBuffer out,
            int outPos,
            int outSize,
            int type);

    static native long compressOnlySourceDirect(
            long ctx,
            ByteBuffer in,
            int inPos,
            int inSize,
            byte[] out,
            int outPos,
            int outSize,
            int type);

    static native long compressBothDirect(
            long ctx,
            ByteBuffer in,
            int inPos,
            int inSize,
            ByteBuffer out,
            int outPos,
            int outSize,
            int type);

    private static native long getCompressBound(long ctx, long count, int type);
}

