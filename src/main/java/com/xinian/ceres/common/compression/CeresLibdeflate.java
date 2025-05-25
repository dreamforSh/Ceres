package com.xinian.ceres.common.compression;

import com.xinian.ceres.Ceres;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Ceres的libdeflate库加载器
 * 负责加载本地库并提供可用性检查
 */
public class CeresLibdeflate {
    private static final String OS_SYSTEM_PROPERTY =
            System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    private static final String OS;
    private static final String ARCH = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
    private static final String NATIVE_LIB_PATH = System.getProperty("ceres.libdeflate_path", "");
    private static Throwable unavailabilityCause;

    static {
        if (OS_SYSTEM_PROPERTY.startsWith("mac")) {
            OS = "darwin";
        } else if (OS_SYSTEM_PROPERTY.startsWith("win")) {
            OS = "windows";
        } else {
            OS = OS_SYSTEM_PROPERTY;
        }

        String path = NATIVE_LIB_PATH.isEmpty() ? "/" + determineLoadPath() : NATIVE_LIB_PATH;

        try {
            copyAndLoadNative(path);
            // 库可用
            unavailabilityCause = null;
        } catch (Throwable e) {
            unavailabilityCause = e;
            Ceres.LOGGER.warn("Failed to load native libdeflate library: {}", e.getMessage());
            if (Ceres.LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    private static void copyAndLoadNative(String path) {
        try {
            InputStream nativeLib = CeresLibdeflate.class.getResourceAsStream(path);
            if (nativeLib == null) {
                // 如果用户尝试从绝对路径加载本地库
                Path libPath = Paths.get(path);
                if (Files.exists(libPath) && Files.isRegularFile(libPath)) {
                    nativeLib = new FileInputStream(path);
                } else {
                    throw new IllegalStateException("Native library " + path + " not found.");
                }
            }

            Path tempFile = createTemporaryNativeFilename(path.substring(path.lastIndexOf('.')));
            Files.copy(nativeLib, tempFile, StandardCopyOption.REPLACE_EXISTING);
            Runtime.getRuntime()
                    .addShutdownHook(
                            new Thread(
                                    () -> {
                                        try {
                                            Files.deleteIfExists(tempFile);
                                        } catch (IOException ignored) {

                                        }
                                    }));

            try {
                System.load(tempFile.toAbsolutePath().toString());
            } catch (UnsatisfiedLinkError e) {
                throw new RuntimeException("Unable to load native " + tempFile.toAbsolutePath(), e);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy natives", e);
        }
    }

    private static Path createTemporaryNativeFilename(String ext) throws IOException {
        return Files.createTempFile("ceres-native-", ext);
    }

    private static String determineLoadPath() {
        return OS + "/" + ARCH + "/libdeflate_jni" + determineDylibSuffix();
    }

    private static String determineDylibSuffix() {
        if (OS.startsWith("darwin")) {
            return ".dylib";
        } else if (OS.startsWith("win")) {
            return ".dll";
        } else {
            return ".so";
        }
    }

    /**
     * 检查libdeflate库是否可用
     * @return 如果库可用则返回true
     */
    public static boolean isAvailable() {
        return unavailabilityCause == null;
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
        if (unavailabilityCause != null) {
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
        return "libdeflate " + getLibdeflateVersion();
    }

    /**
     * 获取libdeflate库的版本
     * 此方法需要本地实现
     * @return 版本字符串
     */
    private static native String getLibdeflateVersion();
}
