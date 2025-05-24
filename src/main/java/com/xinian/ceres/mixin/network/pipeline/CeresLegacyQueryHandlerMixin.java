package com.xinian.ceres.mixin.network.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.server.network.LegacyQueryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修复LegacyQueryHandler中的安全问题
 * 防止在通道不活跃时处理消息
 */
@Mixin(LegacyQueryHandler.class)
public abstract class CeresLegacyQueryHandlerMixin {
    /**
     * 在channelRead方法开始时检查通道是否活跃
     * 如果不活跃，则清除消息并取消处理
     */
    @Inject(method = "channelRead", at = @At(value = "HEAD"), cancellable = true)
    public void channelRead(ChannelHandlerContext ctx, Object msg, CallbackInfo ci) throws Exception {
        if (!ctx.channel().isActive()) {
            ((ByteBuf) msg).clear();
            ci.cancel();
        }
    }
}

