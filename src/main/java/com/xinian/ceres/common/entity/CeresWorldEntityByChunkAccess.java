package com.xinian.ceres.common.entity;

import net.minecraft.world.entity.Entity;

import java.util.Collection;

/**
 * 提供按区块访问实体的接口
 * 用于优化实体查询操作，减少网络传输量
 */
public interface CeresWorldEntityByChunkAccess {
    /**
     * 获取指定区块中的所有实体
     *
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @return 该区块中的实体集合
     */
    Collection<Entity> getEntitiesInChunk(final int chunkX, final int chunkZ);

    /**
     * 检查指定区块是否有实体
     *
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @return 如果区块中有实体则返回true
     */
    default boolean hasEntitiesInChunk(final int chunkX, final int chunkZ) {
        Collection<Entity> entities = getEntitiesInChunk(chunkX, chunkZ);
        return entities != null && !entities.isEmpty();
    }

    /**
     * 获取指定区块中的实体数量
     *
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @return 该区块中的实体数量
     */
    default int getEntityCountInChunk(final int chunkX, final int chunkZ) {
        Collection<Entity> entities = getEntitiesInChunk(chunkX, chunkZ);
        return entities != null ? entities.size() : 0;
    }
}

