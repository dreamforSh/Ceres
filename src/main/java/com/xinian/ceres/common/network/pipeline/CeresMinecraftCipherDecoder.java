package com.xinian.ceres.common.network.pipeline;

import com.google.common.base.Preconditions;
import com.xinian.ceres.common.network.util.CeresNatives.CeresCipher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * Minecraft网络数据解密解码器
 * 使用CeresCipher对入站数据进行解密处理
 */
public class CeresMinecraftCipherDecoder extends MessageToMessageDecoder<ByteBuf> {

    private final CeresCipher cipher;
    private long bytesProcessed = 0;

    /**
     * 创建一个新的Minecraft解密解码器
     *
     * @param cipher 用于解密的密码器
     */
    public CeresMinecraftCipherDecoder(CeresCipher cipher) {
        this.cipher = Preconditions.checkNotNull(cipher, "cipher");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ByteBuf copy = ctx.alloc().buffer(in.readableBytes()).writeBytes(in);
        try {

            cipher.process(copy);

            bytesProcessed += copy.readableBytes();

            out.add(copy);
        } catch (Exception e) {
            copy.release();
            throw e;
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        cipher.close();
    }

    /**
     * 获取已处理的字节数
     *
     * @return 已解密的字节总数
     */
    public long getBytesProcessed() {
        return bytesProcessed;
    }

    /**
     * 获取使用的密码器
     *
     * @return 密码器实例
     */
    public CeresCipher getCipher() {
        return cipher;
    }
}


