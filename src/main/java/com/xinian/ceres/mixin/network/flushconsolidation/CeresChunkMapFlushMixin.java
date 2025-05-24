package com.xinian.ceres.mixin.network.flushconsolidation;

import com.xinian.ceres.common.network.util.CeresAutoFlushUtil;
import com.xinian.ceres.common.player.CeresServerPlayer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.PlayerMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 区块地图刷新优化Mixin
 *
 * <p>优化区块发送逻辑，使用刷新合并技术一次性向客户端发送所有区块，
 * 并以螺旋顺序加载区块。对于服务器高负载或玩家快速飞行时特别有用。</p>
 *
 * <p>注意：由于某种原因，Mojang在同一个方法中同时处理区块加载和卸载数据包。
 * 这就是为什么当玩家离开一个区域时，必须始终向玩家发送区块的原因。</p>
 */
@Mixin(ChunkMap.class)
public abstract class CeresChunkMapFlushMixin {
    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    int viewDistance;

    @Shadow
    @Final
    private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    @Shadow
    @Final
    private PlayerMap playerMap;

    @Shadow
    @Final
    private ChunkMap.DistanceManager distanceManager;

    @Shadow
    public static boolean isChunkInRange(int x1, int y1, int x2, int y2, int maxDistance) {
        throw new AssertionError("Shadow method not overridden");
    }

    /**
     * 重写玩家状态更新方法，避免发送重复区块
     *
     * @param player 玩家
     * @param added 是否添加玩家
     * @author solonovamax (原作者)
     * @author Xinian (Ceres适配)
     * @reason 优化区块发送
     */
    @Overwrite
    public void updatePlayerStatus(ServerPlayer player, boolean added) {
        boolean skipPlayer = this.skipPlayer(player);
        boolean isWatchingWorld = !this.playerMap.ignoredOrUnknown(player);

        int chunkPosX = SectionPos.blockToSectionCoord(player.getBlockX());
        int chunkPosZ = SectionPos.blockToSectionCoord(player.getBlockZ());

        CeresAutoFlushUtil.setAutoFlush(player, false);

        try {
            if (added) {
                this.playerMap.addPlayer(ChunkPos.asLong(chunkPosX, chunkPosZ), player, skipPlayer);
                this.updatePlayerPos(player);

                if (!skipPlayer) {
                    this.distanceManager.addPlayer(SectionPos.of(player), player);
                }

                // 如果添加玩家，发送螺旋区块观察数据包
                sendSpiralChunkWatchPackets(player);
            } else {
                SectionPos chunkSectionPos = player.getLastSectionPos();
                this.playerMap.removePlayer(chunkSectionPos.chunk().toLong(), player);

                if (isWatchingWorld) {
                    this.distanceManager.removePlayer(chunkSectionPos, player);
                }

                // 如果移除玩家，卸载区块
                unloadChunks(player, chunkPosX, chunkPosZ, viewDistance);
            }
        } finally {
            CeresAutoFlushUtil.setAutoFlush(player, true);
        }
    }

    /**
     * 重写玩家移动方法，支持刷新合并和优化区块发送
     *
     * @param player 玩家
     * @author Andrew Steinborn (原作者)
     * @author Xinian (Ceres适配)
     * @reason 添加刷新合并支持并优化区块发送
     */
    @Overwrite
    public void move(ServerPlayer player) {
        // TODO: 通过只考虑玩家附近的实体来进一步优化这个方法
        //       使用FastChunkEntityAccess魔法来实现
        for (ChunkMap.TrackedEntity entityTracker : this.entityMap.values()) {
            if (entityTracker.entity == player) {
                entityTracker.updatePlayers(this.level.players());
            } else {
                entityTracker.updatePlayer(player);
            }
        }

        SectionPos oldPos = player.getLastSectionPos();
        SectionPos newPos = SectionPos.of(player);
        boolean isWatchingWorld = this.playerMap.ignored(player);
        boolean noChunkGen = this.skipPlayer(player);
        boolean movedSections = !oldPos.equals(newPos);

        if (movedSections || isWatchingWorld != noChunkGen) {
            this.updatePlayerPos(player);

            if (!isWatchingWorld) {
                this.distanceManager.removePlayer(oldPos, player);
            }

            if (!noChunkGen) {
                this.distanceManager.addPlayer(newPos, player);
            }

            if (!isWatchingWorld && noChunkGen) {
                this.playerMap.ignorePlayer(player);
            }

            if (isWatchingWorld && !noChunkGen) {
                this.playerMap.unIgnorePlayer(player);
            }

            long oldChunkPos = ChunkPos.asLong(oldPos.getX(), oldPos.getZ());
            long newChunkPos = ChunkPos.asLong(newPos.getX(), newPos.getZ());
            this.playerMap.updatePlayer(oldChunkPos, newChunkPos, player);
        }

        // 玩家*始终*需要发送区块，因为出于某种原因，区块加载和卸载数据包都由同一个方法处理
        if (player.level == this.level)
            this.sendChunkWatchPackets(oldPos, player);
    }

    /**
     * 在实体追踪开始时禁用自动刷新
     */
    @Inject(method = "tick()V", at = @At("HEAD"))
    public void disableAutoFlushForEntityTracking(CallbackInfo info) {
        for (ServerPlayer player : level.players()) {
            CeresAutoFlushUtil.setAutoFlush(player, false);
        }
    }

    /**
     * 在实体追踪结束时启用自动刷新
     */
    @Inject(method = "tick()V", at = @At("RETURN"))
    public void enableAutoFlushForEntityTracking(CallbackInfo info) {
        for (ServerPlayer player : level.players()) {
            CeresAutoFlushUtil.setAutoFlush(player, true);
        }
    }

    /**
     * @param player 玩家
     * @param pos 要发送的区块位置
     * @param mutableObject 新的可变对象
     * @param oldWithinViewDistance 区块之前是否在玩家视距内
     * @param newWithinViewDistance 区块现在是否在玩家视距内
     */
    @Shadow
    protected abstract void updateChunkTracking(ServerPlayer player, ChunkPos pos, MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject,
                                                boolean oldWithinViewDistance, boolean newWithinViewDistance);

    @Shadow
    protected abstract boolean skipPlayer(ServerPlayer player);

    @Shadow
    protected abstract SectionPos updatePlayerPos(ServerPlayer serverPlayerEntity);

    /**
     * 发送区块观察数据包
     *
     * @param oldPos 旧位置
     * @param player 玩家
     */
    private void sendChunkWatchPackets(SectionPos oldPos, ServerPlayer player) {
        CeresAutoFlushUtil.setAutoFlush(player, false);
        try {
            int oldChunkX = oldPos.x();
            int oldChunkZ = oldPos.z();

            int newChunkX = SectionPos.blockToSectionCoord(player.getBlockX());
            int newChunkZ = SectionPos.blockToSectionCoord(player.getBlockZ());

            int playerViewDistance = getPlayerViewDistance(player); // +1作为缓冲

            if (shouldReloadAllChunks(player)) { // 玩家更新了视距，卸载区块并重新发送（只卸载不可见的区块）
                if (player instanceof CeresServerPlayer ceresPlayer)
                    ceresPlayer.setNeedsChunksReloaded(false);

                for (int curX = newChunkX - viewDistance - 1; curX <= newChunkX + viewDistance + 1; ++curX) {
                    for (int curZ = newChunkZ - viewDistance - 1; curZ <= newChunkZ + viewDistance + 1; ++curZ) {
                        ChunkPos chunkPos = new ChunkPos(curX, curZ);
                        boolean inNew = isChunkInRange(curX, curZ, newChunkX, newChunkZ, playerViewDistance);

                        this.updateChunkTracking(player, chunkPos, new MutableObject<>(), true, inNew);
                    }
                }

                // 发送新区块
                sendSpiralChunkWatchPackets(player);
            } else if (Math.abs(oldChunkX - newChunkX) > playerViewDistance * 2 ||
                    Math.abs(oldChunkZ - newChunkZ) > playerViewDistance * 2) {
                // 如果玩家不在旧区块附近，发送所有新区块并卸载旧区块

                // 卸载之前的区块
                // 区块卸载数据包很轻量，所以我们可以这样做
                unloadChunks(player, oldChunkX, oldChunkZ, viewDistance);

                // 发送新区块
                sendSpiralChunkWatchPackets(player);
            } else {
                int minSendChunkX = Math.min(newChunkX, oldChunkX) - playerViewDistance - 1;
                int minSendChunkZ = Math.min(newChunkZ, oldChunkZ) - playerViewDistance - 1;
                int maxSendChunkX = Math.max(newChunkX, oldChunkX) + playerViewDistance + 1;
                int maxSendChunkZ = Math.max(newChunkZ, oldChunkZ) + playerViewDistance + 1;

                // 我们发送玩家所在位置到玩家当前位置范围内的*所有*区块
                // 这是因为#updateChunkTracking方法也会卸载区块
                // 对于视距外的区块，它不做任何事情
                for (int curX = minSendChunkX; curX <= maxSendChunkX; ++curX) {
                    for (int curZ = minSendChunkZ; curZ <= maxSendChunkZ; ++curZ) {
                        ChunkPos chunkPos = new ChunkPos(curX, curZ);
                        boolean inOld = isChunkInRange(curX, curZ, oldChunkX, oldChunkZ, playerViewDistance);
                        boolean inNew = isChunkInRange(curX, curZ, newChunkX, newChunkZ, playerViewDistance);
                        this.updateChunkTracking(player, chunkPos, new MutableObject<>(), inOld, inNew);
                    }
                }
            }
        } finally {
            CeresAutoFlushUtil.setAutoFlush(player, true);
        }
    }

    /**
     * 以螺旋方式向客户端发送区块观察数据包，适用于该区域没有加载区块的玩家
     *
     * @param player 玩家
     */
    private void sendSpiralChunkWatchPackets(ServerPlayer player) {
        int chunkPosX = SectionPos.blockToSectionCoord(player.getBlockX());
        int chunkPosZ = SectionPos.blockToSectionCoord(player.getBlockZ());

        // +1是因为mc在发送区块时会加1
        int playerViewDistance = getPlayerViewDistance(player) + 1;

        int x = 0, z = 0, dx = 0, dz = -1;
        int t = playerViewDistance * 2;
        int maxI = t * t * 2;
        for (int i = 0; i < maxI; i++) {
            if ((-playerViewDistance <= x) && (x <= playerViewDistance) && (-playerViewDistance <= z) && (z <= playerViewDistance)) {
                boolean inNew = isChunkInRange(chunkPosX + x, chunkPosZ + z, chunkPosX, chunkPosZ, playerViewDistance);

                this.updateChunkTracking(player,
                        new ChunkPos(chunkPosX + x, chunkPosZ + z),
                        new MutableObject<>(), false, inNew
                );
            }
            if ((x == z) || ((x < 0) && (x == -z)) || ((x > 0) && (x == 1 - z))) {
                t = dx;
                dx = -dz;
                dz = t;
            }
            x += dx;
            z += dz;
        }
    }

    /**
     * 卸载区块
     *
     * @param player 玩家
     * @param chunkPosX 区块X坐标
     * @param chunkPosZ 区块Z坐标
     * @param distance 距离
     */
    private void unloadChunks(ServerPlayer player, int chunkPosX, int chunkPosZ, int distance) {
        for (int curX = chunkPosX - distance - 1; curX <= chunkPosX + distance + 1; ++curX) {
            for (int curZ = chunkPosZ - distance - 1; curZ <= chunkPosZ + distance + 1; ++curZ) {
                ChunkPos chunkPos = new ChunkPos(curX, curZ);

                this.updateChunkTracking(player, chunkPos, new MutableObject<>(), true, false);
            }
        }
    }

    /**
     * 获取玩家视距
     *
     * @param playerEntity 玩家实体
     * @return 玩家视距
     */
    private int getPlayerViewDistance(ServerPlayer playerEntity) {
        return playerEntity instanceof CeresServerPlayer ceresPlayerEntity
                ? ceresPlayerEntity.getPlayerViewDistance() != -1
                // 如果是-1，则视距尚未设置
                // 我们*实际上*需要发送视距+1，因为mc不会渲染与未加载区块相邻的区块
                ? Math.min(this.viewDistance,
                ceresPlayerEntity.getPlayerViewDistance() +
                        1)
                : this.viewDistance : this.viewDistance;
    }

    /**
     * 检查是否应该重新加载所有区块
     *
     * @param playerEntity 玩家实体
     * @return 如果应该重新加载所有区块则为true
     */
    private boolean shouldReloadAllChunks(ServerPlayer playerEntity) {
        return playerEntity instanceof CeresServerPlayer ceresPlayerEntity && ceresPlayerEntity.getNeedsChunksReloaded();
    }
}
