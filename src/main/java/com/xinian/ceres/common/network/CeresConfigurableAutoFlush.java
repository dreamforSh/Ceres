package com.xinian.ceres.common.network;

/**
 * 可配置自动刷新的网络连接接口
 * 用于优化网络数据包的发送时机
 */
public interface CeresConfigurableAutoFlush {
    /**
     * 设置是否应该自动刷新网络连接
     *
     * <p>当设置为false时，数据包会被缓存直到手动调用flush，
     * 这可以减少网络开销并提高性能。</p>
     *
     * <p>当设置为true时，每个数据包都会立即发送。</p>
     *
     * @param shouldAutoFlush 如果应该自动刷新则为true
     */
    void setShouldAutoFlush(boolean shouldAutoFlush);

    /**
     * 获取当前自动刷新设置
     *
     * @return 如果启用了自动刷新则为true
     */
    default boolean getShouldAutoFlush() {
        return true;
    }

    /**
     * 手动刷新缓冲的数据包
     * 当自动刷新被禁用时使用此方法发送累积的数据包
     */
    default void flushQueue() {

    }
}
