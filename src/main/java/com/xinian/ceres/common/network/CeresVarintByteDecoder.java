package com.xinian.ceres.common.network;

import io.netty.util.ByteProcessor;


public class CeresVarintByteDecoder implements ByteProcessor {
    private int readVarint;
    private int bytesRead;
    private DecodeResult result = DecodeResult.TOO_SHORT;

    @Override
    public boolean process(byte k) {
        if (k == 0 && bytesRead == 0) {
            // 暂时认为它是无效的，但有可能被后续字节修正
            result = DecodeResult.RUN_OF_ZEROES;
            return true;
        }
        if (result == DecodeResult.RUN_OF_ZEROES) {
            return false;
        }
        readVarint |= (k & 0x7F) << bytesRead++ * 7;
        if (bytesRead > 3) {
            result = DecodeResult.TOO_BIG;
            return false;
        }
        if ((k & 0x80) != 128) {
            result = DecodeResult.SUCCESS;
            return false;
        }
        return true;
    }


    public int getReadVarint() {
        return readVarint;
    }


    public int getBytesRead() {
        return bytesRead;
    }


    public DecodeResult getResult() {
        return result;
    }

    public void reset() {
        readVarint = 0;
        bytesRead = 0;
        result = DecodeResult.TOO_SHORT;
    }


    public enum DecodeResult {
        /** 解码成功 */
        SUCCESS,
        /** 数据不足，需要更多字节 */
        TOO_SHORT,
        /** 变长整数过大，超过支持的范围 */
        TOO_BIG,
        /** 遇到一串零字节 */
        RUN_OF_ZEROES
    }
}

