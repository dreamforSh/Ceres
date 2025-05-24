package com.xinian.ceres.common.network.util.exception;

import io.netty.handler.codec.DecoderException;

/**
 * 静默解码器异常
 *
 * <p>一种特殊用途的异常，当我们想要指示解码错误但不希望在日志中看到大量堆栈跟踪时抛出。</p>
 */
public class CeresQuietDecoderException extends DecoderException {

    /**
     * 创建一个静默解码器异常
     *
     * @param message 异常消息
     */
    public CeresQuietDecoderException(String message) {
        super(message);
    }

    /**
     * 重写填充堆栈跟踪方法，返回自身而不生成堆栈跟踪
     * 这可以显著减少日志大小和处理开销
     *
     * @return 此异常实例
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}

