package com.xinian.ceres.mixin.network.pipeline;

import com.xinian.ceres.common.network.CeresVarintByteDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Varint21FrameDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

import static com.xinian.ceres.common.network.util.exception.CeresWellKnownExceptions.BAD_LENGTH_CACHED;
import static com.xinian.ceres.common.network.util.exception.CeresWellKnownExceptions.VARINT_BIG_CACHED;


@Mixin(Varint21FrameDecoder.class)
public class CeresVarint21FrameDecoderMixin {
    private final CeresVarintByteDecoder ceres$reader = new CeresVarintByteDecoder();

    /**
     * @author Andrew Steinborn (原作者)
     * @author Xinian (Ceres适配)
     * @reason 使用优化的Velocity变长整数解码器，减少边界检查
     */
    @Overwrite
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (!ctx.channel().isActive()) {
            in.clear();
            return;
        }

        ceres$reader.reset();

        int varintEnd = in.forEachByte(ceres$reader);
        if (varintEnd == -1) {

            if (ceres$reader.getResult() == CeresVarintByteDecoder.DecodeResult.RUN_OF_ZEROES) {

                in.clear();
            }
            return;
        }

        if (ceres$reader.getResult() == CeresVarintByteDecoder.DecodeResult.RUN_OF_ZEROES) {

            in.readerIndex(varintEnd);
        } else if (ceres$reader.getResult() == CeresVarintByteDecoder.DecodeResult.SUCCESS) {
            int readVarint = ceres$reader.getReadVarint();
            int bytesRead = ceres$reader.getBytesRead();
            if (readVarint < 0) {
                in.clear();
                throw BAD_LENGTH_CACHED;
            } else if (readVarint == 0) {

                in.readerIndex(varintEnd + 1);
            } else {
                int minimumRead = bytesRead + readVarint;
                if (in.isReadable(minimumRead)) {
                    out.add(in.retainedSlice(varintEnd + 1, readVarint));
                    in.skipBytes(minimumRead);
                }
            }
        } else if (ceres$reader.getResult() == CeresVarintByteDecoder.DecodeResult.TOO_BIG) {
            in.clear();
            throw VARINT_BIG_CACHED;
        }
    }
}
