package com.xinian.ceres.common.compression;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;


public class CeresLibdeflateDecompressor implements Closeable, AutoCloseable {
    static {
        CeresLibdeflate.ensureAvailable();
        initIDs();
    }

    private final long ctx;
    private long availInBytes = -1;
    private boolean closed = false;


    public CeresLibdeflateDecompressor() {
        this.ctx = allocate();
    }

    private void ensureNotClosed() {
        if (this.closed) {
            throw new IllegalStateException("Decompressor already closed.");
        }
    }

    /**
     * 检索并清除表示zlib流结束的读入字节数，用于基于字节数组的解压缩API。
     *
     * @return 先前解压缩操作读入的字节数
     * @throws IllegalStateException 如果没有进行解压缩操作
     */
    public long readStreamBytes() {
        long bytes = availInBytes;
        if (bytes == -1) {
            throw new IllegalStateException("No byte array decompression done yet!");
        }
        availInBytes = -1;
        return bytes;
    }

    /**
     * 将给定的{@code in}数组解压缩到{@code out}数组中。
     * 此方法假设{@code out}的长度是未压缩输出的大小。
     * 如果不是这样，请使用{@link #decompressUnknownSize(byte[], byte[], CeresCompressionType)}代替。
     *
     * @param in 包含压缩数据的源数组
     * @param out 将保存解压缩数据的目标数组
     * @param type 要使用的压缩容器
     * @throws DataFormatException 如果提供的数据已损坏，或者数据成功解压缩但小于输出缓冲区的大小
     */
    public void decompress(byte[] in, byte[] out, CeresCompressionType type) throws DataFormatException {
        decompress(in, out, type, out.length);
    }

    /**
     * 将给定的{@code in}数组解压缩到{@code out}数组中。
     * 此方法假设已知数据的未压缩大小。
     *
     * @param in 包含压缩数据的源数组
     * @param out 将保存解压缩数据的目标数组
     * @param type 要使用的压缩容器
     * @param uncompressedSize 数据的已知大小
     * @throws DataFormatException 如果提供的数据已损坏，或者数据成功解压缩但不是{@code uncompressedSize}
     */
    public void decompress(byte[] in, byte[] out, CeresCompressionType type, int uncompressedSize)
            throws DataFormatException {
        ensureNotClosed();
        if (uncompressedSize > out.length) {
            throw new IndexOutOfBoundsException(
                    "uncompressedSize(" + uncompressedSize + ") > out(" + out.length + ")");
        }
        decompressBothHeap(
                in, 0, in.length, out, 0, out.length, type.getNativeType(), uncompressedSize);
    }

    /**
     * 将给定的{@code in}数组解压缩到{@code out}数组中。
     * 此方法假设已知数据的未压缩大小。
     *
     * @param in 包含压缩数据的源数组
     * @param inOff 源数组的偏移量
     * @param inLen 从偏移量开始的源数组长度
     * @param out 将保存解压缩数据的目标数组
     * @param outOff 源数组的偏移量
     * @param outLen 输出数组的长度
     * @param type 要使用的压缩容器
     * @param uncompressedSize 数据的已知大小，也被视为从{@code outOff}开始的输出数组长度
     * @throws DataFormatException 如果提供的数据已损坏，或者数据成功解压缩但不是{@code uncompressedSize}
     */
    public void decompress(
            byte[] in,
            int inOff,
            int inLen,
            byte[] out,
            int outOff,
            int outLen,
            CeresCompressionType type,
            int uncompressedSize)
            throws DataFormatException {
        ensureNotClosed();

        checkBounds(in.length, inOff, inLen);
        checkBounds(out.length, outOff, outLen);
        decompressBothHeap(
                in, inOff, inLen, out, outOff, outLen, type.getNativeType(), uncompressedSize);
    }

    /**
     * 将给定的{@code in}数组解压缩到{@code out}数组中。
     * 此方法假设数据的未压缩大小未知。
     * 请注意，当数据的未压缩大小未知时，不建议使用libdeflate的解压缩器，
     * 因为libdeflate没有流API。如果你需要流API，
     * 最好使用{@code java.util.zip}中的{@code Deflater}和{@code Inflater}类。
     *
     * @param in 包含压缩数据的源数组
     * @param out 将保存解压缩数据的目标数组
     * @param type 要使用的压缩容器
     * @return 一个正的非零整数，表示未压缩输出的大小，如果给定的输出缓冲区太小，则为-1
     * @throws DataFormatException 如果提供的数据已损坏
     */
    public long decompressUnknownSize(byte[] in, byte[] out, CeresCompressionType type)
            throws DataFormatException {
        ensureNotClosed();
        return decompressBothHeap(in, 0, in.length, out, 0, out.length, type.getNativeType(), -1);
    }

    /**
     * 将给定的{@code in}数组解压缩到{@code out}数组中。
     * 此方法假设数据的未压缩大小未知。
     * 请注意，当数据的未压缩大小未知时，不建议使用libdeflate的解压缩器，
     * 因为libdeflate没有流API。如果你需要流API，
     * 最好使用{@code java.util.zip}中的{@code Deflater}和{@code Inflater}类。
     *
     * @param in 包含压缩数据的源数组
     * @param inOff 源数组的偏移量
     * @param inLen 从偏移量开始的源数组长度
     * @param out 将保存解压缩数据的目标数组
     * @param outOff 源数组的偏移量
     * @param outLen 从{@code outOff}开始的源数组长度
     * @param type 要使用的压缩容器
     * @return 一个正的非零整数，表示未压缩输出的大小，如果给定的输出缓冲区太小，则为-1
     * @throws DataFormatException 如果提供的数据已损坏
     */
    public long decompressUnknownSize(
            byte[] in, int inOff, int inLen, byte[] out, int outOff, int outLen, CeresCompressionType type)
            throws DataFormatException {
        ensureNotClosed();

        checkBounds(in.length, inOff, inLen);
        checkBounds(out.length, outOff, outLen);
        return decompressBothHeap(in, inOff, inLen, out, outOff, outLen, type.getNativeType(), -1);
    }

    private long decompress0(
            ByteBuffer in, ByteBuffer out, CeresCompressionType type, int uncompressedSize)
            throws DataFormatException {
        ensureNotClosed();
        int nativeType = type.getNativeType();

        int inAvail = in.remaining();
        int outAvail = out.remaining();

        if (uncompressedSize < -1) {
            throw new IndexOutOfBoundsException("uncompressedSize = " + uncompressedSize);
        }
        if (uncompressedSize > outAvail) {
            throw new IndexOutOfBoundsException(
                    "uncompressedSize(" + uncompressedSize + ") > outAvail(" + outAvail + ")");
        }


        long outRealSize;
        if (in.isDirect()) {
            if (out.isDirect()) {
                outRealSize =
                        decompressBothDirect(
                                in,
                                in.position(),
                                inAvail,
                                out,
                                out.position(),
                                outAvail,
                                nativeType,
                                uncompressedSize);
            } else {
                outRealSize =
                        decompressOnlySourceDirect(
                                in,
                                in.position(),
                                inAvail,
                                out.array(),
                                byteBufferArrayPosition(out),
                                outAvail,
                                nativeType,
                                uncompressedSize);
            }
        } else {
            int inPos = byteBufferArrayPosition(in);
            if (out.isDirect()) {
                outRealSize =
                        decompressOnlyDestinationDirect(
                                in.array(),
                                inPos,
                                inAvail,
                                out,
                                out.position(),
                                outAvail,
                                nativeType,
                                uncompressedSize);
            } else {
                outRealSize =
                        decompressBothHeap(
                                in.array(),
                                inPos,
                                inAvail,
                                out.array(),
                                byteBufferArrayPosition(out),
                                outAvail,
                                nativeType,
                                uncompressedSize);
            }
        }

        if (uncompressedSize != -1) {
            outRealSize = uncompressedSize;
        }
        out.position((int) (out.position() + outRealSize));
        in.position((int) (in.position() + this.readStreamBytes()));
        return outRealSize;
    }

    /**
     * 将给定的{@code in} ByteBuffer解压缩到{@code out} ByteBuffer中。
     * 当解压缩操作完成时，输出缓冲区的{@code position}将增加产生的字节数，
     * 输入{@code position}将增加读取的字节数。
     * 此函数假设未压缩数据的大小是缓冲区中剩余的字节数（限制减去其位置）。
     *
     * @param in 要解压缩的源字节缓冲区
     * @param out 将保存解压缩数据的目标缓冲区
     * @param type 使用的压缩容器
     * @throws DataFormatException 如果提供的数据已损坏，或者数据解压缩为无效大小
     */
    public void decompress(ByteBuffer in, ByteBuffer out, CeresCompressionType type)
            throws DataFormatException {
        decompress0(in, out, type, out.remaining());
    }

    /**
     * 将给定的{@code in} ByteBuffer解压缩到{@code out} ByteBuffer中。
     * 当解压缩操作完成时，输出缓冲区的{@code position}将增加产生的字节数，
     * 输入{@code position}将增加读取的字节数。
     *
     * @param in 要解压缩的源字节缓冲区
     * @param out 将保存解压缩数据的目标缓冲区
     * @param type 使用的压缩容器
     * @param uncompressedSize 已知的未压缩大小
     * @throws DataFormatException 如果提供的数据已损坏，或者数据解压缩为无效大小
     */
    public void decompress(ByteBuffer in, ByteBuffer out, CeresCompressionType type, int uncompressedSize)
            throws DataFormatException {
        decompress0(in, out, type, uncompressedSize);
    }

    /**
     * 将给定的{@code in} ByteBuffer解压缩到{@code out} ByteBuffer中。
     * 当解压缩操作完成时，输出缓冲区的{@code position}将增加产生的字节数，
     * 输入{@code position}将增加读取的字节数。
     *
     * <p>请注意，当数据的未压缩大小未知时，不建议使用libdeflate的解压缩器，
     * 因为libdeflate没有流API。如果你需要流API，
     * 最好使用{@code java.util.zip}中的{@code Deflater}和{@code Inflater}类。
     *
     * @param in 要解压缩的源字节缓冲区
     * @param out 将保存解压缩数据的目标缓冲区
     * @param type 使用的压缩容器
     * @return 一个正的非零整数，表示未压缩输出的大小，如果给定的输出缓冲区太小，则为-1
     * @throws DataFormatException 如果提供的数据已损坏，或者数据解压缩为无效大小
     */
    public long decompressUnknownSize(ByteBuffer in, ByteBuffer out, CeresCompressionType type)
            throws DataFormatException {
        return decompress0(in, out, type, -1);
    }

    @Override
    public void close() {
        ensureNotClosed();
        free(this.ctx);
        this.closed = true;
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

    /* 本地函数声明。 */
    private static native void initIDs();

    private static native long allocate();

    private static native void free(long ctx);

    private native long decompressBothHeap(
            byte[] in,
            int inPos,
            int inSize,
            byte[] out,
            int outPos,
            int outSize,
            int type,
            int knownSize)
            throws DataFormatException;

    private native long decompressOnlyDestinationDirect(
            byte[] in,
            int inPos,
            int inSize,
            ByteBuffer out,
            int outPos,
            int outSize,
            int type,
            int knownSize)
            throws DataFormatException;

    private native long decompressOnlySourceDirect(
            ByteBuffer in,
            int inPos,
            int inSize,
            byte[] out,
            int outPos,
            int outSize,
            int type,
            int knownSize)
            throws DataFormatException;

    private native long decompressBothDirect(
            ByteBuffer in,
            int inPos,
            int inSize,
            ByteBuffer out,
            int outPos,
            int outSize,
            int type,
            int knownSize)
            throws DataFormatException;
}

