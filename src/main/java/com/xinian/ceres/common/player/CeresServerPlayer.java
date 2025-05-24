package com.xinian.ceres.common.player;

/**
 * 扩展服务器玩家功能的接口，用于优化区块加载和网络传输
 */
public interface CeresServerPlayer {
    /**
     * 设置玩家是否需要重新加载区块
     * 当玩家视距变化或传送到新位置时，此标志应设为true
     *
     * @param needsChunksReloaded 如果需要重新加载区块则为true
     */
    void setNeedsChunksReloaded(boolean needsChunksReloaded);

    /**
     * 获取玩家的当前视距
     * 这可能与服务器配置的视距不同，因为玩家可能有自定义设置
     *
     * @return 玩家的视距（以区块为单位）
     */
    int getPlayerViewDistance();

    /**
     * 检查玩家是否需要重新加载区块
     *
     * @return 如果玩家需要重新加载区块则为true
     */
    boolean getNeedsChunksReloaded();

    /**
     * 获取玩家上次区块重载时间
     * 用于限制频繁的区块重载请求
     *
     * @return 上次区块重载的时间戳（毫秒）
     */
    default long getLastChunkReloadTime() {
        return 0L;
    }

    /**
     * 设置玩家上次区块重载时间
     *
     * @param time 时间戳（毫秒）
     */
    default void setLastChunkReloadTime(long time) {
        // 默认实现为空
    }

    /**
     * 检查是否可以为此玩家重新加载区块
     * 用于防止过于频繁的区块重载请求
     *
     * @param currentTime 当前时间戳
     * @param cooldownMs 冷却时间（毫秒）
     * @return 如果可以重新加载区块则为true
     */
    default boolean canReloadChunks(long currentTime, long cooldownMs) {
        return currentTime - getLastChunkReloadTime() >= cooldownMs;
    }
}


