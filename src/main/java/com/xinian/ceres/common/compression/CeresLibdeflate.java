package com.xinian.ceres.common.compression;

import com.xinian.ceres.Ceres;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ceres的libdeflate库加载器 - 仅支持Windows x86_64平台
 * 负责加载本地库并提供可用性检查
 */
public class CeresLibdeflate {
    // 库文件名称
    private static final String LIBRARY_NAME = "libdeflate_jni";
    private static final String LIBRARY_FILE = LIBRARY_NAME + ".dll";

    // 资源路径
    private static final String RESOURCE_PATH = "/windows/x86_64/" + LIBRARY_FILE;

    // 本地库路径，可通过系统属性覆盖
    private static final String NATIVE_LIB_PATH = System.getProperty("ceres.libdeflate_path", "");

    // 库加载状态
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);
    private static Throwable unavailabilityCause;

    // 临时文件引用，用于清理
    private static Path extractedLibPath = null;

    // 静态初始化块，尝试加载库
    static {
        try {
            Ceres.LOGGER.info("Attempting to load libdeflate native library...");
            Ceres.LOGGER.info("System.getProperty(\"os.name\"): {}", System.getProperty("os.name"));
            Ceres.LOGGER.info("System.getProperty(\"os.arch\"): {}", System.getProperty("os.arch"));
            Ceres.LOGGER.info("Custom library path: {}", NATIVE_LIB_PATH.isEmpty() ? "not set" : NATIVE_LIB_PATH);

            loadNativeLibrary();

            // 不再尝试调用getLibdeflateVersion方法
            unavailabilityCause = null;
            LOADED.set(true);
            Ceres.LOGGER.info("Successfully loaded libdeflate native library");
        } catch (Throwable e) {
            unavailabilityCause = e;
            LOADED.set(false);
            Ceres.LOGGER.error("Failed to load native libdeflate library: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载本地库
     * 尝试多种加载策略
     */
    private static void loadNativeLibrary() throws IOException {
        // 如果指定了自定义路径，尝试直接加载
        if (!NATIVE_LIB_PATH.isEmpty()) {
            try {
                File libFile = new File(NATIVE_LIB_PATH);
                Ceres.LOGGER.info("Attempting to load from custom path: {}", libFile.getAbsolutePath());
                Ceres.LOGGER.info("File exists: {}", libFile.exists());

                System.load(libFile.getAbsolutePath());
                Ceres.LOGGER.info("Loaded libdeflate from custom path: {}", libFile.getAbsolutePath());
                return;
            } catch (UnsatisfiedLinkError e) {
                Ceres.LOGGER.error("Failed to load from custom path: {}", e.getMessage());
                Ceres.LOGGER.info("Falling back to resource extraction");
            }
        }

        // 尝试从资源中提取库
        Ceres.LOGGER.info("Attempting to load from resource: {}", RESOURCE_PATH);
        InputStream libStream = CeresLibdeflate.class.getResourceAsStream(RESOURCE_PATH);

        if (libStream == null) {
            Ceres.LOGGER.error("Resource not found: {}", RESOURCE_PATH);

            // 列出可用的资源
            try {
                InputStream rootStream = CeresLibdeflate.class.getResourceAsStream("/");
                if (rootStream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(rootStream));
                    String line;
                    Ceres.LOGGER.info("Available resources in root:");
                    while ((line = reader.readLine()) != null) {
                        Ceres.LOGGER.info("  {}", line);
                    }
                } else {
                    Ceres.LOGGER.info("Cannot list resources in root directory");
                }
            } catch (Exception e) {
                Ceres.LOGGER.error("Error listing resources: {}", e.getMessage());
            }

            throw new FileNotFoundException("Native library not found in resources: " + RESOURCE_PATH);
        }

        try {
            // 创建临时文件
            extractedLibPath = createTempLibraryFile(LIBRARY_FILE);
            Ceres.LOGGER.info("Created temp file: {}", extractedLibPath);

            // 复制库文件到临时位置
            Files.copy(libStream, extractedLibPath, StandardCopyOption.REPLACE_EXISTING);
            Ceres.LOGGER.info("Copied library to temp file, size: {} bytes", Files.size(extractedLibPath));

            // 加载库
            System.load(extractedLibPath.toAbsolutePath().toString());

            // 添加关闭钩子以清理临时文件
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (extractedLibPath != null) {
                        Files.deleteIfExists(extractedLibPath);
                    }
                } catch (IOException e) {
                    // 忽略关闭时的错误
                }
            }));

            Ceres.LOGGER.info("Loaded libdeflate from extracted resource: {}", extractedLibPath);
        } catch (IOException e) {
            Ceres.LOGGER.error("Error extracting library: {}", e.getMessage());

            // 尝试直接通过系统库路径加载
            try {
                Ceres.LOGGER.info("Attempting to load from system library path");
                System.loadLibrary(LIBRARY_NAME);
                Ceres.LOGGER.info("Loaded libdeflate from system library path");
            } catch (UnsatisfiedLinkError ule) {
                Ceres.LOGGER.error("Failed to load from system library path: {}", ule.getMessage());
                throw new IOException("Failed to load native library: " + e.getMessage(), e);
            }
        } finally {
            libStream.close();
        }
    }

    /**
     * 创建临时库文件
     */
    private static Path createTempLibraryFile(String fileName) throws IOException {

        Path tempDir = Files.createTempDirectory("ceres-natives-");


        Path tempFile = tempDir.resolve(fileName);


        tempFile.toFile().deleteOnExit();
        tempDir.toFile().deleteOnExit();

        return tempFile;
    }

    /**
     * 检查libdeflate库是否可用
     * @return 如果库可用则返回true
     */
    public static boolean isAvailable() {
        return LOADED.get();
    }

    /**
     * 获取库不可用的原因
     * @return 不可用原因的异常，如果库可用则返回null
     */
    public static Throwable unavailabilityCause() {
        return unavailabilityCause;
    }

    /**
     * 确保libdeflate库可用
     * @throws RuntimeException 如果库不可用
     */
    public static void ensureAvailable() {
        if (!isAvailable()) {
            throw new RuntimeException("libdeflate JNI library unavailable", unavailabilityCause);
        }
    }

    /**
     * 获取库的版本信息
     * @return 版本信息字符串
     */
    public static String getVersionInfo() {
        if (!isAvailable()) {
            return "libdeflate unavailable";
        }

        return "libdeflate (native library loaded)";
    }
}
