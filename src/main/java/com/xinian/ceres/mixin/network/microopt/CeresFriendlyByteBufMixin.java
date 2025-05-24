package com.xinian.ceres.mixin.network.microopt;

import com.xinian.ceres.common.network.util.CeresVarIntUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
/**
 * 数据包写入的多个微优化
 * 优化变长整数编码和字符串写入性能
 */
@Mixin(FriendlyByteBuf.class)
public abstract class CeresFriendlyByteBufMixin extends ByteBuf {
    @Shadow
    @Final
    private ByteBuf source;
    @Shadow
    public abstract int writeCharSequence(CharSequence charSequence, Charset charset);
    /**
     * 使用优化的VarInt字节大小查找表
     *
     * @param value 要计算大小的整数值
     * @return 编码为VarInt所需的字节数
     * @author Andrew (原作者)
     * @author Xinian (Ceres适配)
     * @reason 使用优化的VarInt字节大小查找表
     */
    @Overwrite
    public static int getVarIntSize(int value) {
        return CeresVarIntUtil.getVarIntLength(value);
    }
    /**
     * 使用{@link ByteBuf#writeCharSequence(CharSequence, Charset)}以获得更好的性能，
     * 并使用{@link ByteBufUtil#utf8Bytes(CharSequence)}提前计算字节大小
     *
     * @param string 要写入的字符串
     * @param i 最大字节长度
     * @return 友好字节缓冲区
     * @author Andrew (原作者)
     * @author Xinian (Ceres适配)
     * @reason 使用更高效的字符串编码方法
     */
    @Overwrite
    public FriendlyByteBuf writeUtf(String string, int i) {
        int utf8Bytes = ByteBufUtil.utf8Bytes(string);
        if (utf8Bytes > i) {
            throw new EncoderException("String too big (was " + utf8Bytes + " bytes encoded, max " + i + ")");
        } else {
            this.writeVarInt(utf8Bytes);
            this.writeCharSequence(string, StandardCharsets.UTF_8);
            return new FriendlyByteBuf(source);
        }
    }
    /**
     * 优化的VarInt写入
     *
     * @param value 要写入的整数值
     * @return 友好字节缓冲区
     * @author Andrew (原作者)
     * @author Xinian (Ceres适配)
     * @reason 优化VarInt写入
     */
    @Overwrite
    public FriendlyByteBuf writeVarInt(int value) {
        // 显式处理一字节和两字节的情况，因为它们是代理最常写入的VarInt大小，
        // 以改善内联。
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            source.writeByte(value);
        } else if ((value & (0xFFFFFFFF << 14)) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            source.writeShort(w);
        } else {
            writeVarIntFull(source, value);
        }
        return new FriendlyByteBuf(source);
    }
    /**
     * 完整的VarInt写入实现
     *
     * @param buf 字节缓冲区
     * @param value 要写入的整数值
     */
    private static void writeVarIntFull(ByteBuf buf, int value) {
        // 参见 https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            buf.writeByte(value);
        } else if ((value & (0xFFFFFFFF << 14)) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            buf.writeShort(w);
        } else if ((value & (0xFFFFFFFF << 21)) == 0) {
            int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
            buf.writeMedium(w);
        } else if ((value & (0xFFFFFFFF << 28)) == 0) {
            int w = (value & 0x7F | 0x80) << 24 | (((value >>> 7) & 0x7F | 0x80) << 16)
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | (value >>> 21);
            buf.writeInt(w);
        } else {
            int w = (value & 0x7F | 0x80) << 24 | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | ((value >>> 21) & 0x7F | 0x80);
            buf.writeInt(w);
            buf.writeByte(value >>> 28);
        }
    }
}
