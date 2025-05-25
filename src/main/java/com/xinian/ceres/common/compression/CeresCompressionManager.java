package com.xinian.ceres.common.compression;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import com.xinian.ceres.CeresConfig.CommonConfig.*;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Deflater;


public class CeresCompressionManager {

    private static final AtomicLong TOTAL_BYTES_BEFORE = new AtomicLong(0);
    private static final AtomicLong TOTAL_BYTES_AFTER = new AtomicLong(0);
    private static final AtomicLong TOTAL_PACKETS = new AtomicLong(0);
    private static final AtomicLong TOTAL_TIME_SPENT = new AtomicLong(0);


    private static CeresConfig.CompressionEngine currentEngine = CeresConfig.CompressionEngine.JAVA;


    private static final ThreadLocal<CeresLibdeflateCompressor> LIBDEFLATE_COMPRESSOR =
            ThreadLocal.withInitial(() -> {
                try {
                    return new CeresLibdeflateCompressor(CeresConfig.COMMON.advancedCompressionLevel.get());
                } catch (Exception e) {
                    Ceres.LOGGER.warn("Failed to create libdeflate compressor, falling back to Java", e);
                    return null;
                }
            });


    private static final ThreadLocal<Deflater> JAVA_DEFLATER =
            ThreadLocal.withInitial(() -> new Deflater(CeresConfig.COMMON.compressionLevel.get()));


    public static void init() {

        CeresConfig.CompressionEngine configEngine = CeresConfig.COMMON.compressionEngine.get();

        if (configEngine == CeresConfig.CompressionEngine.AUTO) {

            if (CeresLibdeflate.isAvailable() && CeresConfig.COMMON.useNativeCompression.get()) {
                currentEngine = CeresConfig.CompressionEngine.LIBDEFLATE;
                Ceres.LOGGER.info("Using LIBDEFLATE compression engine");
            } else {
                currentEngine = CeresConfig.CompressionEngine.JAVA;
                Ceres.LOGGER.info("Using JAVA compression engine");
            }
        } else {

            if (configEngine == CeresConfig.CompressionEngine.LIBDEFLATE &&
                    (!CeresLibdeflate.isAvailable() || !CeresConfig.COMMON.useNativeCompression.get())) {
                Ceres.LOGGER.warn("LIBDEFLATE engine requested but not available, falling back to JAVA");
                currentEngine = CeresConfig.CompressionEngine.JAVA;
            } else {
                currentEngine = configEngine;
                Ceres.LOGGER.info("Using {} compression engine as configured", currentEngine);
            }
        }


        Ceres.LOGGER.info("Compression format: {}", CeresConfig.COMMON.compressionFormat.get());
        Ceres.LOGGER.info("Compression level: {}",
                currentEngine == CeresConfig.CompressionEngine.LIBDEFLATE ?
                        CeresConfig.COMMON.advancedCompressionLevel.get() :
                        CeresConfig.COMMON.compressionLevel.get());
        Ceres.LOGGER.info("Adaptive compression: {}", CeresConfig.COMMON.enableAdaptiveCompression.get());
        Ceres.LOGGER.info("Minimum packet size to compress: {} bytes", CeresConfig.COMMON.minPacketSizeToCompress.get());
    }


    public static byte[] compressData(byte[] data) {

        if (!CeresConfig.COMMON.enableCompression.get()) {
            return data;
        }


        int threshold = CeresConfig.COMMON.minPacketSizeToCompress.get();
        if (data.length < threshold) {
            return data;
        }

        long startTime = System.nanoTime();
        byte[] result;

        try {

            if (currentEngine == CeresConfig.CompressionEngine.LIBDEFLATE && CeresLibdeflate.isAvailable()) {
                result = compressWithLibdeflate(data);
            } else {
                result = compressWithJava(data);
            }


            TOTAL_BYTES_BEFORE.addAndGet(data.length);
            TOTAL_BYTES_AFTER.addAndGet(result.length);
            TOTAL_PACKETS.incrementAndGet();
            TOTAL_TIME_SPENT.addAndGet(System.nanoTime() - startTime);

            return result;
        } catch (Exception e) {
            Ceres.LOGGER.error("Compression failed: {}", e.getMessage());
            if (CeresConfig.COMMON.enableLogging.get()) {
                e.printStackTrace();
            }
            return data;
        }
    }

    /**
     * 使用libdeflate压缩数据
     *
     * @param data 要压缩的数据
     * @return 压缩后的数据
     */
    private static byte[] compressWithLibdeflate(byte[] data) {
        CeresLibdeflateCompressor compressor = LIBDEFLATE_COMPRESSOR.get();
        if (compressor == null) {

            return compressWithJava(data);
        }

        try {

            CeresCompressionType format = CeresConfig.COMMON.compressionFormat.get();


            long bound = compressor.getCompressBound(data.length, format);
            if (bound > Integer.MAX_VALUE) {

                return compressWithJava(data);
            }


            byte[] output = new byte[(int)bound];


            int compressedSize = compressor.compress(data, output, format);
            if (compressedSize <= 0 || compressedSize >= data.length) {

                return data;
            }


            byte[] result = new byte[compressedSize];
            System.arraycopy(output, 0, result, 0, compressedSize);

            if (CeresConfig.COMMON.enableLogging.get()) {
                Ceres.LOGGER.debug("Compressed {} bytes to {} bytes with libdeflate ({}% reduction)",
                        data.length, result.length,
                        Math.round((1 - (float) result.length / data.length) * 100));
            }

            return result;
        } catch (Exception e) {
            Ceres.LOGGER.warn("libdeflate compression failed, falling back to Java: {}", e.getMessage());
            return compressWithJava(data);
        }
    }

    /**
     * 使用Java压缩数据
     *
     * @param data 要压缩的数据
     * @return 压缩后的数据
     */
    private static byte[] compressWithJava(byte[] data) {
        try {
            Deflater deflater = JAVA_DEFLATER.get();
            deflater.reset();
            deflater.setInput(data);
            deflater.finish();

            byte[] output = new byte[data.length];
            int compressedSize = deflater.deflate(output);

            if (compressedSize <= 0 || compressedSize >= data.length) {
                // 压缩失败或无效，返回原始数据
                return data;
            }

            byte[] result = new byte[compressedSize];
            System.arraycopy(output, 0, result, 0, compressedSize);

            if (CeresConfig.COMMON.enableLogging.get()) {
                Ceres.LOGGER.debug("Compressed {} bytes to {} bytes with Java ({}% reduction)",
                        data.length, result.length,
                        Math.round((1 - (float) result.length / data.length) * 100));
            }

            return result;
        } catch (Exception e) {
            Ceres.LOGGER.error("Java compression failed: {}", e.getMessage());
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

        long startTime = System.nanoTime();

        try {
            byte[] result;


            if (currentEngine == CeresConfig.CompressionEngine.LIBDEFLATE && CeresLibdeflate.isAvailable()) {
                result = decompressWithLibdeflate(compressedData);
            } else {
                result = decompressWithJava(compressedData);
            }


            TOTAL_TIME_SPENT.addAndGet(System.nanoTime() - startTime);

            return result;
        } catch (Exception e) {
            Ceres.LOGGER.error("Decompression failed: {}", e.getMessage());
            if (CeresConfig.COMMON.enableLogging.get()) {
                e.printStackTrace();
            }
            return compressedData;
        }
    }

    /**
     * 使用libdeflate解压数据
     *
     * @param compressedData 压缩后的数据
     * @return 解压后的数据
     */
    private static byte[] decompressWithLibdeflate(byte[] compressedData) {
        try (CeresLibdeflateDecompressor decompressor = new CeresLibdeflateDecompressor()) {

            CeresCompressionType format = CeresConfig.COMMON.compressionFormat.get();


            byte[] output = new byte[compressedData.length * 4];


            long decompressedSize = decompressor.decompressUnknownSize(compressedData, output, format);
            if (decompressedSize <= 0) {

                return decompressWithJava(compressedData);
            }


            byte[] result = new byte[(int)decompressedSize];
            System.arraycopy(output, 0, result, 0, (int)decompressedSize);

            if (CeresConfig.COMMON.enableLogging.get()) {
                Ceres.LOGGER.debug("Decompressed {} bytes to {} bytes with libdeflate",
                        compressedData.length, result.length);
            }

            return result;
        } catch (Exception e) {
            Ceres.LOGGER.warn("libdeflate decompression failed, falling back to Java: {}", e.getMessage());
            return decompressWithJava(compressedData);
        }
    }

    /**
     * 使用Java解压数据
     *
     * @param compressedData 压缩后的数据
     * @return 解压后的数据
     */
    private static byte[] decompressWithJava(byte[] compressedData) {
        try {
            java.util.zip.Inflater inflater = new java.util.zip.Inflater();
            inflater.setInput(compressedData);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedData.length * 4);
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && inflater.needsInput()) {
                    break;
                }
                outputStream.write(buffer, 0, count);
            }
            inflater.end();

            byte[] result = outputStream.toByteArray();

            if (CeresConfig.COMMON.enableLogging.get()) {
                Ceres.LOGGER.debug("Decompressed {} bytes to {} bytes with Java",
                        compressedData.length, result.length);
            }

            return result;
        } catch (Exception e) {
            Ceres.LOGGER.error("Java decompression failed: {}", e.getMessage());
            return compressedData;
        }
    }


    public static String getCompressionStats() {
        long totalBefore = TOTAL_BYTES_BEFORE.get();
        long totalAfter = TOTAL_BYTES_AFTER.get();
        long packets = TOTAL_PACKETS.get();
        long timeSpent = TOTAL_TIME_SPENT.get();

        if (packets == 0) {
            return "No packets compressed yet";
        }

        double compressionRatio = (double) totalAfter / totalBefore;
        double savedBytes = totalBefore - totalAfter;
        double avgTimeMs = (double) timeSpent / (packets * 1_000_000); // 纳秒转毫秒

        return String.format(
                "Compressed %d packets, %d KB → %d KB (%.1f%% reduction, saved %.1f KB, avg %.2f ms/packet)",
                packets,
                totalBefore / 1024,
                totalAfter / 1024,
                (1 - compressionRatio) * 100,
                savedBytes / 1024,
                avgTimeMs
        );
    }


    public static void resetStats() {
        TOTAL_BYTES_BEFORE.set(0);
        TOTAL_BYTES_AFTER.set(0);
        TOTAL_PACKETS.set(0);
        TOTAL_TIME_SPENT.set(0);
    }


    public static CeresConfig.CompressionEngine getCurrentEngine() {
        return currentEngine;
    }


    public static void shutdown() {

        JAVA_DEFLATER.remove();


        CeresLibdeflateCompressor compressor = LIBDEFLATE_COMPRESSOR.get();
        if (compressor != null) {
            try {
                compressor.close();
            } catch (Exception e) {

            }
        }
        LIBDEFLATE_COMPRESSOR.remove();
    }
}
