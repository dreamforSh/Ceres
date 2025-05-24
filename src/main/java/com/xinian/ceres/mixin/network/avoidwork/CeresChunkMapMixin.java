package com.xinian.ceres.mixin.network.avoidwork;

import com.xinian.ceres.Ceres;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 区块地图优化Mixin - 安全版本
 * 仅添加日志记录，不修改原版行为
 */
@Mixin(ChunkMap.class)
public class CeresChunkMapMixin {
    @Shadow
    @Final
    private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    /**
     * 仅添加日志记录，不修改原版行为
     */
    @Inject(method = "playerLoadedChunk", at = @At("HEAD"))
    public void onPlayerLoadedChunk(ServerPlayer player, MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject, LevelChunk chunk, CallbackInfo ci) {
        if (Ceres.LOGGER.isDebugEnabled()) {
            Ceres.LOGGER.debug("Player {} loaded chunk at {}, {}",
                    player.getName().getString(),
                    chunk.getPos().x,
                    chunk.getPos().z);
        }
    }
}
