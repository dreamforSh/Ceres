// PacketCompressor.java
package com.xinian.ceres.network;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import com.xinian.ceres.common.compression.CeresCompressionManager;
import com.xinian.ceres.common.compression.CeresCompressionType;
import com.xinian.ceres.common.compression.CeresLibdeflate;
import com.xinian.ceres.common.compression.CeresLibdeflateCompressor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class PacketCompressor {

    private static final AtomicLong TOTAL_BYTES_BEFORE = new AtomicLong(0);
    private static final AtomicLong TOTAL_BYTES_AFTER = new AtomicLong(0);
    private static final AtomicLong PACKETS_COMPRESSED = new AtomicLong(0);
    private static final AtomicLong PACKETS_SKIPPED = new AtomicLong(0);
    private static final AtomicLong COMPRESSION_TIME = new AtomicLong(0);
    private static final AtomicLong DECOMPRESSION_TIME = new AtomicLong(0);


    private static final ThreadLocal<Deflater> DEFLATER = ThreadLocal.withInitial(() ->
            new Deflater(CeresConfig.COMMON.compressionLevel.get()));


    private static final ConcurrentHashMap<Thread, CeresLibdeflateCompressor> LIBDEFLATE_COMPRESSORS =
            new ConcurrentHashMap<>();


    public static byte[] compressData(byte[] data) {

        if (!CeresConfig.COMMON.enableCompression.get()) {
            return data;
        }

        int threshold = CeresConfig.COMMON.minPacketSizeToCompress.get();
        if (data.length < threshold) {
            return data;
        }

        long startTime = System.nanoTime();
        try {
            byte[] compressedData;


            CeresConfig.CompressionEngine engine = CeresConfig.COMMON.compressionEngine.get();
            if (engine == CeresConfig.CompressionEngine.AUTO) {

                if (CeresLibdeflate.isAvailable() && CeresConfig.COMMON.useNativeCompression.get()) {
                    compressedData = compressWithLibdeflate(data);
                } else {
                    compressedData = compressWithJava(data);
                }
            } else if (engine == CeresConfig.CompressionEngine.LIBDEFLATE &&
                    CeresLibdeflate.isAvailable() &&
                    CeresConfig.COMMON.useNativeCompression.get()) {
                compressedData = compressWithLibdeflate(data);
            } else {
                compressedData = compressWithJava(data);
            }


            synchronized (PacketCompressor.class) {
                TOTAL_BYTES_BEFORE.addAndGet(data.length);
                TOTAL_BYTES_AFTER.addAndGet(compressedData.length);
                COMPRESSION_TIME.addAndGet(System.nanoTime() - startTime);

                if (compressedData == data) {
                    PACKETS_SKIPPED.incrementAndGet();
                } else {
                    PACKETS_COMPRESSED.incrementAndGet();
                }
            }

            return compressedData;
        } catch (Exception e) {
            Ceres.LOGGER.error("Failed to compress data: {}", e.getMessage());
            if (CeresConfig.COMMON.enableLogging.get()) {
                e.printStackTrace();
            }
            return data;
        }
    }


    private static byte[] compressWithLibdeflate(byte[] data) {
        try {

            Thread currentThread = Thread.currentThread();
            CeresLibdeflateCompressor compressor = LIBDEFLATE_COMPRESSORS.computeIfAbsent(
                    currentThread,
                    t -> new CeresLibdeflateCompressor(CeresConfig.COMMON.advancedCompressionLevel.get())
            );


            CeresCompressionType format = CeresConfig.COMMON.compressionFormat.get();


            long bound = compressor.getCompressBound(data.length, format);
            if (bound > Integer.MAX_VALUE) {

                return data;
            }


            byte[] output = new byte[(int)bound];


            int compressedSize = compressor.compress(data, output, format);


            if (compressedSize <= 0 || compressedSize >= data.length) {
                if (CeresConfig.COMMON.enableLogging.get()) {
                    Ceres.LOGGER.debug("Compression ineffective for {} bytes, skipping", data.length);
                }
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


    private static byte[] compressWithJava(byte[] data) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(data.length);


            int level = CeresConfig.COMMON.compressionLevel.get();
            GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream) {
                {
                    def.setLevel(level);
                }
            };


            gzipStream.write(data);
            gzipStream.close();


            byte[] compressedData = byteStream.toByteArray();


            if (compressedData.length >= data.length) {
                if (CeresConfig.COMMON.enableLogging.get()) {
                    Ceres.LOGGER.debug("Compression ineffective for {} bytes, skipping", data.length);
                }
                return data;
            }

            if (CeresConfig.COMMON.enableLogging.get()) {
                Ceres.LOGGER.debug("Compressed {} bytes to {} bytes with Java ({}% reduction)",
                        data.length, compressedData.length,
                        Math.round((1 - (float) compressedData.length / data.length) * 100));
            }

            return compressedData;
        } catch (IOException e) {
            Ceres.LOGGER.error("Failed to compress data with Java: {}", e.getMessage());
            return data;
        }
    }


    public static byte[] decompressData(byte[] compressedData, boolean isCompressed) {
        if (!isCompressed) {
            return compressedData;
        }

        long startTime = System.nanoTime();
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(compressedData);
            GZIPInputStream gzipStream = new GZIPInputStream(byteStream);


            ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = gzipStream.read(buffer)) > 0) {
                resultStream.write(buffer, 0, length);
            }

            gzipStream.close();
            resultStream.close();

            byte[] decompressedData = resultStream.toByteArray();


            DECOMPRESSION_TIME.addAndGet(System.nanoTime() - startTime);

            if (CeresConfig.COMMON.enableLogging.get()) {
                Ceres.LOGGER.debug("Decompressed {} bytes to {} bytes",
                        compressedData.length, decompressedData.length);
            }

            return decompressedData;
        } catch (IOException e) {
            Ceres.LOGGER.error("Failed to decompress data: {}", e.getMessage());
            return compressedData;
        }
    }


    public static String getCompressionStats() {
        long totalBefore = TOTAL_BYTES_BEFORE.get();
        long totalAfter = TOTAL_BYTES_AFTER.get();
        long packetsCompressed = PACKETS_COMPRESSED.get();
        long packetsSkipped = PACKETS_SKIPPED.get();
        long compressionTimeNs = COMPRESSION_TIME.get();
        long decompressionTimeNs = DECOMPRESSION_TIME.get();

        if (packetsCompressed == 0) {
            return "No packets compressed yet";
        }

        double compressionRatio = (double) totalAfter / totalBefore;
        double savedBytes = totalBefore - totalAfter;
        double avgCompressionTimeMs = packetsCompressed > 0 ?
                (double) compressionTimeNs / (packetsCompressed * 1_000_000) : 0;
        double avgDecompressionTimeMs = packetsCompressed > 0 ?
                (double) decompressionTimeNs / (packetsCompressed * 1_000_000) : 0;

        if (CeresConfig.CLIENT.showDetailedStats.get()) {
            return String.format(
                    "Compressed %d packets (%d skipped), %d KB → %d KB (%.1f%% ratio, saved %.1f KB, %.2f/%.2f ms)",
                    packetsCompressed,
                    packetsSkipped,
                    totalBefore / 1024,
                    totalAfter / 1024,
                    (1 - compressionRatio) * 100,
                    savedBytes / 1024,
                    avgCompressionTimeMs,
                    avgDecompressionTimeMs
            );
        } else {
            return String.format(
                    "Compressed %d packets (%d skipped), %d KB → %d KB (%.1f%% ratio, saved %.1f KB)",
                    packetsCompressed,
                    packetsSkipped,
                    totalBefore / 1024,
                    totalAfter / 1024,
                    (1 - compressionRatio) * 100,
                    savedBytes / 1024
            );
        }
    }


    public static void resetStats() {
        TOTAL_BYTES_BEFORE.set(0);
        TOTAL_BYTES_AFTER.set(0);
        PACKETS_COMPRESSED.set(0);
        PACKETS_SKIPPED.set(0);
        COMPRESSION_TIME.set(0);
        DECOMPRESSION_TIME.set(0);
    }


    public static void shutdown() {

        for (CeresLibdeflateCompressor compressor : LIBDEFLATE_COMPRESSORS.values()) {
            try {
                compressor.close();
            } catch (Exception e) {

            }
        }
        LIBDEFLATE_COMPRESSORS.clear();


        DEFLATER.remove();
    }
}
