package com.xinian.ceres.common.compression;

import java.util.EnumMap;
import java.util.Map;

/**
 * 定义压缩类型枚举
 * 用于指定压缩算法的格式
 */
public enum CeresCompressionType {
    DEFLATE,
    ZLIB,
    GZIP;


    private static final Map<CeresCompressionType, Integer> NATIVE_MAPPINGS;

    static {
        NATIVE_MAPPINGS = new EnumMap<>(CeresCompressionType.class);
        NATIVE_MAPPINGS.put(DEFLATE, 0);
        NATIVE_MAPPINGS.put(ZLIB, 1);
        NATIVE_MAPPINGS.put(GZIP, 2);
    }

    /**
     * 获取对应的本地类型ID
     * @return 本地类型ID
     */
    int getNativeType() {
        Integer type = NATIVE_MAPPINGS.get(this);
        assert type != null
                : "No native type associated with " + this + " - this is a bug in Ceres";
        return type;
    }

    /**
     * 根据本地类型ID获取对应的压缩类型
     * @param nativeType 本地类型ID
     * @return 对应的压缩类型，如果不存在则返回null
     */
    public static CeresCompressionType fromNativeType(int nativeType) {
        for (Map.Entry<CeresCompressionType, Integer> entry : NATIVE_MAPPINGS.entrySet()) {
            if (entry.getValue() == nativeType) {
                return entry.getKey();
            }
        }
        return null;
    }
}

