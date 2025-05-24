package com.xinian.ceres.common.network.util.exception;

/**
 * 预定义的网络异常常量
 * 用于避免重复创建相同的异常实例
 */
public enum CeresWellKnownExceptions {
    ;

    /**
     * 数据包长度错误的预缓存异常
     */
    public static final CeresQuietDecoderException BAD_LENGTH_CACHED = new CeresQuietDecoderException("Bad packet length");

    /**
     * 变长整数过大的预缓存异常
     */
    public static final CeresQuietDecoderException VARINT_BIG_CACHED = new CeresQuietDecoderException("VarInt too big");

    /**
     * 数据包ID无效的预缓存异常
     */
    public static final CeresQuietDecoderException INVALID_PACKET_ID = new CeresQuietDecoderException("Invalid packet ID");

    /**
     * 数据包解码失败的预缓存异常
     */
    public static final CeresQuietDecoderException DECODE_FAILED = new CeresQuietDecoderException("Packet decode failed");
}

