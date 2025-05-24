package com.xinian.ceres.network;

import com.xinian.ceres.CeresConfig;

/**
 * 标记接口，用于标识已经经过Ceres优化的数据包
 * 这些数据包在NettyOptimizer中会得到特殊处理
 */
public interface OptimizedPacket {
    /**
     * 检查当前是否使用现代模式
     */
    static boolean isModernMode() {
        return CeresConfig.COMMON.optimizationMode.get() == CeresConfig.OptimizationMode.MODERN;
    }
}
