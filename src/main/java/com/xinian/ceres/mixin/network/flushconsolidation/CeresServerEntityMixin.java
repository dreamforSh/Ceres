package com.xinian.ceres.mixin.network.flushconsolidation;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static com.xinian.ceres.common.network.util.CeresAutoFlushUtil.setAutoFlush;
/**
 * 服务器实体Mixin
 * 在添加实体配对时优化网络刷新
 */
@Mixin(ServerEntity.class)
public class CeresServerEntityMixin {
    /**
     * 在添加配对开始时禁用自动刷新
     */
    @Inject(at = @At("HEAD"), method = "addPairing")
    public void addPairing$disableAutoFlush(ServerPlayer player, CallbackInfo ci) {
        setAutoFlush(player, false);
    }
    /**
     * 在添加配对结束时重新启用自动刷新
     */
    @Inject(at = @At("RETURN"), method = "addPairing")
    public void addPairing$reenableAutoFlush(ServerPlayer player, CallbackInfo ci) {
        setAutoFlush(player, true);
    }
}
