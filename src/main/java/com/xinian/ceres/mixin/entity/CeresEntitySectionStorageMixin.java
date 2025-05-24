// CeresEntitySectionStorageMixin.java
package com.xinian.ceres.mixin.entity;

import com.xinian.ceres.common.entity.CeresWorldEntityByChunkAccess;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(EntitySectionStorage.class)
public abstract class CeresEntitySectionStorageMixin implements CeresWorldEntityByChunkAccess {

    @Shadow
    @Final
    private Long2ObjectMap<EntitySection<Entity>> sections;

    @Override
    public Collection<Entity> getEntitiesInChunk(final int chunkX, final int chunkZ) {
        final LongSortedSet set = this.getChunkSections(chunkX, chunkZ);
        if (set.isEmpty()) {
            // 区块中没有实体分区
            return List.of();
        }

        final List<Entity> entities = new ArrayList<>();
        final LongIterator sectionsIterator = set.iterator();
        while (sectionsIterator.hasNext()) {
            final long key = sectionsIterator.nextLong();
            final EntitySection<Entity> value = this.sections.get(key);
            if (value != null && value.getStatus().isAccessible()) {
                entities.addAll(((CeresEntitySectionAccessor<Entity>) value).getStorage());
            }
        }

        return entities;
    }

    @Shadow
    protected abstract LongSortedSet getChunkSections(int chunkX, int chunkZ);
}

