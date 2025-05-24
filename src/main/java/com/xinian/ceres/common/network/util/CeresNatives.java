package com.xinian.ceres.common.network.util;

import com.xinian.ceres.Ceres;
import io.netty.buffer.ByteBuf;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Ceres本地工具类
 * 提供加密和压缩功能的纯Java实现
 */
public class CeresNatives {

    public static final CipherFactory cipher = new CipherFactory();
    public static final CompressorFactory compress = new CompressorFactory();

    /**
     * 密码器工厂
     * 用于创建加密和解密组件
     */
    public static class CipherFactory {
        /**
         * 获取加载的变体名称
         * @return 实现名称
         */
        public String getLoadedVariant() {
            return "Java";
        }

        /**
         * 创建加密器
         * @param key 加密密钥
         * @return 加密密码器
         * @throws GeneralSecurityException 如果创建失败
         */
        public CeresCipher forEncryption(SecretKey key) throws GeneralSecurityException {
            Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, cipher.getParameters());
            return new JavaCeresCipher(cipher);
        }

        /**
         * 创建解密器
         * @param key 解密密钥
         * @return 解密密码器
         * @throws GeneralSecurityException 如果创建失败
         */
        public CeresCipher forDecryption(SecretKey key) throws GeneralSecurityException {
            Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, cipher.getParameters());
            return new JavaCeresCipher(cipher);
        }
    }

    /**
     * 压缩器工厂
     * 用于创建压缩和解压组件
     */
    public static class CompressorFactory {
        /**
         * 获取加载的变体名称
         * @return 实现名称
         */
        public String getLoadedVariant() {
            return "Java";
        }

        /**
         * 创建压缩器
         * @param level 压缩级别
         * @return 压缩器
         */
        public CeresCompressor create(int level) {
            return new JavaCeresCompressor(level);
        }
    }

    /**
     * 密码器接口
     * 定义加密和解密操作
     */
    public interface CeresCipher {
        /**
         * 处理ByteBuf中的数据
         * @param buf 要处理的ByteBuf
         */
        void process(ByteBuf buf);

        /**
         * 关闭密码器并释放资源
         */
        void close();
    }

    /**
     * Java实现的密码器
     * 使用JDK的Cipher类
     */
    private static class JavaCeresCipher implements CeresCipher {
        private final Cipher cipher;

        public JavaCeresCipher(Cipher cipher) {
            this.cipher = cipher;
        }

        @Override
        public void process(ByteBuf buf) {
            int readableBytes = buf.readableBytes();
            if (readableBytes == 0) {
                return;
            }

            byte[] bytes;
            int readerIndex = buf.readerIndex();

            if (buf.hasArray()) {
                // 如果ByteBuf有底层数组，直接使用
                bytes = buf.array();
                int arrayOffset = buf.arrayOffset() + readerIndex;

                try {
                    cipher.update(bytes, arrayOffset, readableBytes, bytes, arrayOffset);
                } catch (Exception e) {
                    Ceres.LOGGER.error("Failed to process buffer", e);
                    throw new RuntimeException("Failed to process buffer", e);
                }
            } else {
                // 否则，复制数据到临时数组
                bytes = new byte[readableBytes];
                buf.getBytes(readerIndex, bytes);

                try {
                    byte[] processed = cipher.update(bytes);
                    if (processed != null && processed.length > 0) {
                        buf.setBytes(readerIndex, processed);
                    }
                } catch (Exception e) {
                    Ceres.LOGGER.error("Failed to process buffer", e);
                    throw new RuntimeException("Failed to process buffer", e);
                }
            }
        }

        @Override
        public void close() {
            // Java Cipher不需要显式关闭
        }
    }

    /**
     * 压缩器接口
     * 定义压缩和解压操作
     */
    public interface CeresCompressor {
        /**
         * 压缩数据
         * @param source 源ByteBuf
         * @param destination 目标ByteBuf
         */
        void deflate(ByteBuf source, ByteBuf destination);

        /**
         * 解压数据
         * @param source 源ByteBuf
         * @param destination 目标ByteBuf
         * @param uncompressedSize 解压后的大小
         */
        void inflate(ByteBuf source, ByteBuf destination, int uncompressedSize);

        /**
         * 关闭压缩器并释放资源
         */
        void close();
    }

    /**
     * Java实现的压缩器
     * 使用JDK的Deflater和Inflater类
     */
    private static class JavaCeresCompressor implements CeresCompressor {
        private final int level;
        private final Deflater deflater;
        private final Inflater inflater;

        public JavaCeresCompressor(int level) {
            this.level = level;
            this.deflater = new Deflater(level);
            this.inflater = new Inflater();
        }

        @Override
        public void deflate(ByteBuf source, ByteBuf destination) {
            int readableBytes = source.readableBytes();
            if (readableBytes == 0) {
                return;
            }

            byte[] sourceBytes = new byte[readableBytes];
            source.readBytes(sourceBytes);

            deflater.setInput(sourceBytes);
            deflater.finish();

            byte[] buffer = new byte[8192]; // 8KB缓冲区
            while (!deflater.finished()) {
                int bytesCompressed = deflater.deflate(buffer);
                if (bytesCompressed > 0) {
                    destination.writeBytes(buffer, 0, bytesCompressed);
                }
            }

            deflater.reset();
        }

        @Override
        public void inflate(ByteBuf source, ByteBuf destination, int uncompressedSize) {
            int readableBytes = source.readableBytes();
            if (readableBytes == 0) {
                return;
            }

            byte[] sourceBytes = new byte[readableBytes];
            source.readBytes(sourceBytes);

            inflater.setInput(sourceBytes);

            byte[] buffer = new byte[uncompressedSize];
            try {
                int bytesDecompressed = inflater.inflate(buffer);
                destination.writeBytes(buffer, 0, bytesDecompressed);
            } catch (Exception e) {
                Ceres.LOGGER.error("Failed to decompress data", e);
                throw new RuntimeException("Failed to decompress data", e);
            } finally {
                inflater.reset();
            }
        }

        @Override
        public void close() {
            deflater.end();
            inflater.end();
        }
    }
}
