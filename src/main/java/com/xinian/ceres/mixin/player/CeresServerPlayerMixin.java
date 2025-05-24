package com.xinian.ceres.mixin.player;

import com.xinian.ceres.common.player.CeresServerPlayer;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 服务器玩家Mixin
 * 扩展ServerPlayer以支持视距优化和区块重载功能
 */
@Mixin(ServerPlayer.class)
@Implements(@Interface(iface = CeresServerPlayer.class, prefix = "ceres$", unique = true))
public class CeresServerPlayerMixin implements CeresServerPlayer {
    @Unique
    private int playerViewDistance = -1;

    @Unique
    private boolean needsChunksReloaded = false;

    @Unique
    private long lastChunkReloadTime = 0L;

    /**
     * 在玩家更新选项时注入，检测视距变化并标记需要重新加载区块
     */
    @Inject(method = "updateOptions", at = @At("HEAD"))
    public void updateOptions(ServerboundClientInformationPacket packet, CallbackInfo ci) {
        // 检查视距是否变化
        needsChunksReloaded = (playerViewDistance != packet.viewDistance());
        playerViewDistance = packet.viewDistance();

        if (needsChunksReloaded) {
            // 记录区块重载时间
            lastChunkReloadTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean getNeedsChunksReloaded() {
        return needsChunksReloaded;
    }

    @Override
    public void setNeedsChunksReloaded(boolean needsChunksReloaded) {
        this.needsChunksReloaded = needsChunksReloaded;

        if (needsChunksReloaded) {
            // 记录区块重载时间
            lastChunkReloadTime = System.currentTimeMillis();
        }
    }

    @Override
    public int getPlayerViewDistance() {
        return playerViewDistance;
    }

    @Override
    public long getLastChunkReloadTime() {
        return lastChunkReloadTime;
    }

    @Override
    public void setLastChunkReloadTime(long time) {
        this.lastChunkReloadTime = time;
    }

    /**
     * 检查是否应该重新加载区块
     * 在玩家传送或视距变化时调用
     *
     * @param currentTime 当前时间
     * @param cooldownMs 冷却时间（毫秒）
     * @return 如果应该重新加载区块则为true
     */
    @Unique
    public boolean ceres$canReloadChunks(long currentTime, long cooldownMs) {
        return currentTime - lastChunkReloadTime >= cooldownMs;
    }
}
