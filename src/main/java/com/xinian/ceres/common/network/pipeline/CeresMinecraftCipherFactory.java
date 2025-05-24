package com.xinian.ceres.common.network.pipeline;

import com.google.common.base.Preconditions;
import com.xinian.ceres.Ceres;
import com.xinian.ceres.common.network.util.CeresNatives;
import com.xinian.ceres.common.network.util.CeresNatives.CeresCipher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * Minecraft加密组件工厂
 * 用于创建和管理加密/解密组件
 */
public class CeresMinecraftCipherFactory {
    private static final CeresNatives.CipherFactory CIPHER_FACTORY = CeresNatives.cipher;

    /**
     * 创建加密编码器
     *
     * @param key 加密密钥
     * @return 加密编码器
     * @throws GeneralSecurityException 如果创建加密器失败
     */
    public static CeresMinecraftCipherEncoder createEncoder(SecretKey key) throws GeneralSecurityException {
        CeresCipher cipher = CIPHER_FACTORY.forEncryption(key);
        return new CeresMinecraftCipherEncoder(cipher);
    }

    /**
     * 创建解密解码器
     *
     * @param key 解密密钥
     * @return 解密解码器
     * @throws GeneralSecurityException 如果创建解密器失败
     */
    public static CeresMinecraftCipherDecoder createDecoder(SecretKey key) throws GeneralSecurityException {
        CeresCipher cipher = CIPHER_FACTORY.forDecryption(key);
        return new CeresMinecraftCipherDecoder(cipher);
    }

    /**
     * 从字节数组创建密钥
     *
     * @param keyBytes 密钥字节数组
     * @return 密钥对象
     */
    public static SecretKey createKey(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 获取当前使用的密码器工厂
     *
     * @return 密码器工厂
     */
    public static CeresNatives.CipherFactory getCipherFactory() {
        return CIPHER_FACTORY;
    }

    /**
     * 获取当前使用的密码器实现名称
     *
     * @return 密码器实现名称
     */
    public static String getCipherImplementationName() {
        return "CeresCipher";
    }

    /**
     * 记录当前使用的密码器实现信息
     */
    public static void logImplementationDetails() {
        Ceres.LOGGER.info("Using cipher implementation: {}", CIPHER_FACTORY.getLoadedVariant());
    }

    /**
     * 检查是否支持本地加密实现
     *
     * @return 如果支持本地加密实现则为true
     */
    public static boolean isNativeSupported() {
        return !CIPHER_FACTORY.getLoadedVariant().equals("Java");
    }

    /**
     * 获取加密实现的详细信息
     *
     * @return 加密实现的详细信息字符串
     */
    public static String getImplementationDetails() {
        return String.format("Cipher: %s (%s)",
                getCipherImplementationName(),
                CIPHER_FACTORY.getLoadedVariant());
    }

    /**
     * 创建测试密钥
     * 仅用于测试目的，不应在生产环境中使用
     *
     * @return 测试密钥
     */
    public static SecretKey createTestKey() {
        byte[] testKey = new byte[16];
        for (int i = 0; i < testKey.length; i++) {
            testKey[i] = (byte) i;
        }
        return createKey(testKey);
    }

    /**
     * 测试加密功能
     * 验证加密和解密是否正常工作
     *
     * @return 如果测试成功则为true
     */
    public static boolean testEncryption() {
        try {
            SecretKey testKey = createTestKey();
            CeresCipher encryptCipher = CIPHER_FACTORY.forEncryption(testKey);
            CeresCipher decryptCipher = CIPHER_FACTORY.forDecryption(testKey);

            // 创建测试数据
            String testString = "Ceres encryption test";
            byte[] testData = testString.getBytes(StandardCharsets.UTF_8);

            // 创建ByteBuf
            ByteBuf originalBuf = Unpooled.wrappedBuffer(testData);
            ByteBuf encryptedBuf = Unpooled.buffer(testData.length);
            ByteBuf decryptedBuf = Unpooled.buffer(testData.length);

            // 复制原始数据到加密缓冲区
            originalBuf.resetReaderIndex();
            encryptedBuf.writeBytes(originalBuf);

            // 加密
            encryptCipher.process(encryptedBuf);

            // 解密
            encryptedBuf.resetReaderIndex();
            decryptedBuf.writeBytes(encryptedBuf);
            decryptCipher.process(decryptedBuf);

            // 验证结果
            byte[] result = new byte[decryptedBuf.readableBytes()];
            decryptedBuf.resetReaderIndex();
            decryptedBuf.readBytes(result);

            String resultString = new String(result, StandardCharsets.UTF_8);
            boolean success = testString.equals(resultString);

            // 清理资源
            originalBuf.release();
            encryptedBuf.release();
            decryptedBuf.release();
            encryptCipher.close();
            decryptCipher.close();

            if (success) {
                Ceres.LOGGER.debug("Encryption test successful");
            } else {
                Ceres.LOGGER.error("Encryption test failed: result mismatch");
            }

            return success;
        } catch (Exception e) {
            Ceres.LOGGER.error("Encryption test failed: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
