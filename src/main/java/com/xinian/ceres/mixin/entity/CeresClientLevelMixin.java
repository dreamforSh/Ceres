package com.xinian.ceres.mixin.entity;

import com.xinian.ceres.common.entity.CeresWorldEntityByChunkAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

/**
 * 为客户端世界提供按区块快速访问实体的能力
 * 用于优化网络传输和渲染性能
 */
@Mixin(ClientLevel.class)
@OnlyIn(Dist.CLIENT)
public class CeresClientLevelMixin implements CeresWorldEntityByChunkAccess {
    @Shadow
    @Final
    private TransientEntitySectionManager<Entity> entityStorage;

    @Override
    public Collection<Entity> getEntitiesInChunk(int chunkX, int chunkZ) {
        return ((CeresWorldEntityByChunkAccess) this.entityStorage.sectionStorage).getEntitiesInChunk(chunkX, chunkZ);
    }
}

