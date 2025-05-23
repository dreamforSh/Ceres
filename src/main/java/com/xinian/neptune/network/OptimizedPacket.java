package com.xinian.neptune.network;

import com.xinian.neptune.NeptuneConfig;

/**
 * 标记接口，用于标识已经经过Neptune优化的数据包
 * 这些数据包在NettyOptimizer中会得到特殊处理
 */
public interface OptimizedPacket {
    /**
     * 检查当前是否使用现代模式
     */
    static boolean isModernMode() {
        return NeptuneConfig.COMMON.optimizationMode.get() == NeptuneConfig.OptimizationMode.MODERN;
    }
}
