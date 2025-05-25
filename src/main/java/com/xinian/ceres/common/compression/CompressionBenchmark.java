package com.xinian.ceres.common.compression;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import com.xinian.ceres.CeresConfig.CommonConfig.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;


public class CompressionBenchmark {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final int[] TEST_SIZES = {1024, 8 * 1024, 64 * 1024, 256 * 1024, 1024 * 1024};
    private static final int WARMUP_ITERATIONS = 3;
    private static final int TEST_ITERATIONS = 5;

    public static void runBenchmarkForPlayer(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6Running Ceres compression benchmark..."));


        StringBuilder results = new StringBuilder();
        results.append("§6Ceres Compression Benchmark Results:\n");


        for (int size : TEST_SIZES) {
            results.append(String.format("§e--- Testing %s data ---\n", formatSize(size)));


            byte[] testData = generateTestData(size);


            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                compressWithJava(testData);
                if (CeresLibdeflate.isAvailable()) {
                    compressWithLibdeflate(testData);
                }
            }


            long javaStartTime = System.nanoTime();
            byte[] javaCompressed = null;
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                javaCompressed = compressWithJava(testData);
            }
            long javaTime = (System.nanoTime() - javaStartTime) / TEST_ITERATIONS;


            long libdeflateStartTime = System.nanoTime();
            byte[] libdeflateCompressed = null;
            boolean libdeflateAvailable = CeresLibdeflate.isAvailable();

            if (libdeflateAvailable) {
                for (int i = 0; i < TEST_ITERATIONS; i++) {
                    libdeflateCompressed = compressWithLibdeflate(testData);
                }
            }
            long libdeflateTime = libdeflateAvailable ?
                    (System.nanoTime() - libdeflateStartTime) / TEST_ITERATIONS : 0;

            // 添加结果
            double javaRatio = javaCompressed != null ?
                    (double) javaCompressed.length / testData.length * 100 : 0;

            results.append(String.format("§aJava: %s → %s (%.1f%%) in %.2f ms\n",
                    formatSize(testData.length),
                    formatSize(javaCompressed != null ? javaCompressed.length : 0),
                    javaRatio,
                    javaTime / 1_000_000.0));

            if (libdeflateAvailable && libdeflateCompressed != null) {
                double libdeflateRatio = (double) libdeflateCompressed.length / testData.length * 100;
                double speedup = (double) javaTime / libdeflateTime;

                results.append(String.format("§aLibdeflate: %s → %s (%.1f%%) in %.2f ms\n",
                        formatSize(testData.length),
                        formatSize(libdeflateCompressed.length),
                        libdeflateRatio,
                        libdeflateTime / 1_000_000.0));

                results.append(String.format("§6Libdeflate is %.2fx faster than Java\n", speedup));
            } else {
                results.append("§cLibdeflate not available\n");
            }

            results.append("\n");
        }

        // 发送结果
        String[] lines = results.toString().split("\n");
        for (String line : lines) {
            player.sendSystemMessage(Component.literal(line));
        }

        // 发送建议
        if (CeresLibdeflate.isAvailable()) {
            player.sendSystemMessage(Component.literal("§6Recommendation: Use LIBDEFLATE engine for best performance"));
        } else {
            player.sendSystemMessage(Component.literal("§6Recommendation: Install native libraries for better performance"));
        }
    }


    public static void runBenchmark() {
        Ceres.LOGGER.info("Running compression benchmark...");


        for (int size : TEST_SIZES) {
            Ceres.LOGGER.info("--- Testing {} data ---", formatSize(size));


            byte[] testData = generateTestData(size);


            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                compressWithJava(testData);
                if (CeresLibdeflate.isAvailable()) {
                    compressWithLibdeflate(testData);
                }
            }


            long javaStartTime = System.nanoTime();
            byte[] javaCompressed = null;
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                javaCompressed = compressWithJava(testData);
            }
            long javaTime = (System.nanoTime() - javaStartTime) / TEST_ITERATIONS;


            long libdeflateStartTime = System.nanoTime();
            byte[] libdeflateCompressed = null;
            boolean libdeflateAvailable = CeresLibdeflate.isAvailable();

            if (libdeflateAvailable) {
                for (int i = 0; i < TEST_ITERATIONS; i++) {
                    libdeflateCompressed = compressWithLibdeflate(testData);
                }
            }
            long libdeflateTime = libdeflateAvailable ?
                    (System.nanoTime() - libdeflateStartTime) / TEST_ITERATIONS : 0;


            double javaRatio = javaCompressed != null ?
                    (double) javaCompressed.length / testData.length * 100 : 0;

            Ceres.LOGGER.info("Java: {} → {} ({:.1f}%) in {:.2f} ms",
                    formatSize(testData.length),
                    formatSize(javaCompressed != null ? javaCompressed.length : 0),
                    javaRatio,
                    javaTime / 1_000_000.0);

            if (libdeflateAvailable && libdeflateCompressed != null) {
                double libdeflateRatio = (double) libdeflateCompressed.length / testData.length * 100;
                double speedup = (double) javaTime / libdeflateTime;

                Ceres.LOGGER.info("Libdeflate: {} → {} ({:.1f}%) in {:.2f} ms",
                        formatSize(testData.length),
                        formatSize(libdeflateCompressed.length),
                        libdeflateRatio,
                        libdeflateTime / 1_000_000.0);

                Ceres.LOGGER.info("Libdeflate is {:.2f}x faster than Java", speedup);
            } else {
                Ceres.LOGGER.info("Libdeflate not available");
            }
        }


        if (CeresLibdeflate.isAvailable()) {
            Ceres.LOGGER.info("Recommendation: Use LIBDEFLATE engine for best performance");
        } else {
            Ceres.LOGGER.info("Recommendation: Install native libraries for better performance");
        }
    }


    private static byte[] generateTestData(int size) {
        byte[] data = new byte[size];
        Random random = new Random(42);


        for (int i = 0; i < size; i++) {
            if (i % 16 == 0) {

                random.setSeed(i / 16 + 42);
            }
            data[i] = (byte) (random.nextInt(256) & 0xFF);
        }

        return data;
    }


    private static byte[] compressWithJava(byte[] data) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(data.length);

            // 创建GZIP输出流，设置压缩级别
            int level = CeresConfig.COMMON.compressionLevel.get();
            GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream) {
                {
                    def.setLevel(level);
                }
            };

            // 写入数据并关闭流
            gzipStream.write(data);
            gzipStream.close();

            // 获取压缩后的数据
            return byteStream.toByteArray();
        } catch (Exception e) {
            Ceres.LOGGER.error("Failed to compress data with Java: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 使用libdeflate压缩数据
     *
     * @param data 原始数据
     * @return 压缩后的数据
     */
    private static byte[] compressWithLibdeflate(byte[] data) {
        if (!CeresLibdeflate.isAvailable()) {
            return null;
        }

        try (CeresLibdeflateCompressor compressor = new CeresLibdeflateCompressor(
                CeresConfig.COMMON.advancedCompressionLevel.get())) {

            // 获取压缩格式
            CeresCompressionType format = CeresConfig.COMMON.compressionFormat.get();

            // 计算压缩边界
            long bound = compressor.getCompressBound(data.length, format);
            if (bound > Integer.MAX_VALUE) {
                Ceres.LOGGER.error("Compression bound too large: {}", bound);
                return null;
            }

            // 创建输出缓冲区
            byte[] output = new byte[(int)bound];

            // 压缩数据
            int compressedSize = compressor.compress(data, output, format);
            if (compressedSize <= 0) {
                Ceres.LOGGER.error("Compression failed, returned size: {}", compressedSize);
                return null;
            }

            // 创建结果数组
            byte[] result = new byte[compressedSize];
            System.arraycopy(output, 0, result, 0, compressedSize);

            return result;
        } catch (Exception e) {
            Ceres.LOGGER.error("Failed to compress data with libdeflate: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 格式化大小为人类可读的字符串
     *
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return DECIMAL_FORMAT.format(bytes / 1024.0) + " KB";
        } else {
            return DECIMAL_FORMAT.format(bytes / (1024.0 * 1024.0)) + " MB";
        }
    }
}
