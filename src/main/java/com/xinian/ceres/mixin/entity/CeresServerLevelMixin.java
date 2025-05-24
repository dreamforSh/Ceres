
package com.xinian.ceres.mixin.entity;

import com.xinian.ceres.common.entity.CeresWorldEntityByChunkAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;


@Mixin(ServerLevel.class)
public class CeresServerLevelMixin implements CeresWorldEntityByChunkAccess {
    @Shadow
    @Final
    private PersistentEntitySectionManager<Entity> entityManager;

    @Override
    public Collection<Entity> getEntitiesInChunk(int chunkX, int chunkZ) {
        return ((CeresWorldEntityByChunkAccess) this.entityManager.sectionStorage).getEntitiesInChunk(chunkX, chunkZ);
    }
}

