package com.xinian.ceres.common.network.pipeline;

import com.google.common.base.Preconditions;
import com.xinian.ceres.common.network.util.CeresNatives.CeresCipher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * Minecraft网络数据加密编码器
 * 使用CeresCipher对出站数据进行加密处理
 */
public class CeresMinecraftCipherEncoder extends MessageToMessageEncoder<ByteBuf> {

    private final CeresCipher cipher;
    private long bytesProcessed = 0;

    /**
     * 创建一个新的Minecraft加密编码器
     *
     * @param cipher 用于加密的密码器
     */
    public CeresMinecraftCipherEncoder(CeresCipher cipher) {
        this.cipher = Preconditions.checkNotNull(cipher, "cipher");
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        ByteBuf copy = ctx.alloc().buffer(msg.readableBytes());
        try {
            copy.writeBytes(msg);
            // 加密数据
            cipher.process(copy);
            // 更新统计信息
            bytesProcessed += copy.readableBytes();
            // 添加到输出列表
            out.add(copy);
        } catch (Exception e) {
            copy.release(); // 如果抛出异常，copy将永远不会被使用
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
     * @return 已加密的字节总数
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
