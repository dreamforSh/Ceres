package com.xinian.ceres.mixin.network.flushconsolidation;

import com.xinian.ceres.common.network.CeresConfigurableAutoFlush;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * 优化Connection类，添加跳过自动刷新和尽可能使用void promises的能力
 */
@Mixin(Connection.class)
public abstract class CeresConnectionFlushMixin implements CeresConfigurableAutoFlush {
    @Shadow
    private Channel channel;
    private AtomicBoolean autoFlush;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initAddedFields(CallbackInfo ci) {
        this.autoFlush = new AtomicBoolean(true);
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/network/Connection;channel:Lio/netty/channel/Channel;", opcode = Opcodes.GETFIELD))
    public Channel disableForcedFlushEveryTick(Connection clientConnection) {
        return null;
    }
    @Override
    public void setShouldAutoFlush(boolean shouldAutoFlush) {
        boolean prev = this.autoFlush.getAndSet(shouldAutoFlush);
        if (!prev && shouldAutoFlush) {
            this.channel.flush();
        }
    }

    @Override
    public boolean getShouldAutoFlush() {
        return this.autoFlush.get();
    }

    @Override
    public void flushQueue() {
        if (this.channel != null && this.channel.isActive()) {
            this.channel.flush();
        }
    }
}
