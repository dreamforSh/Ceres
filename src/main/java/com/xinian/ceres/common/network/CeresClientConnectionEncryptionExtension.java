package com.xinian.ceres.common.network;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

/**
 * 客户端连接加密扩展接口
 * 提供网络连接加密功能
 */
public interface CeresClientConnectionEncryptionExtension {
    /**
     * 设置连接加密
     *
     * <p>使用提供的密钥配置加密处理器，以保护网络通信安全。</p>
     * <p>此方法通常在身份验证完成后调用。</p>
     *
     * @param key 用于加密和解密的密钥
     * @throws GeneralSecurityException 如果加密设置失败
     */
    void setupEncryption(SecretKey key) throws GeneralSecurityException;

    /**
     * 检查连接是否已加密
     *
     * @return 如果连接已加密则为true
     */
    default boolean isEncrypted() {
        return false;
    }

    /**
     * 获取当前使用的加密算法
     *
     * @return 加密算法名称，如果未加密则返回null
     */
    default String getEncryptionAlgorithm() {
        return null;
    }
}
