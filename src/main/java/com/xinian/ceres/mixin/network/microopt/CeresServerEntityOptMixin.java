package com.xinian.ceres.mixin.network.microopt;

import com.google.common.collect.ImmutableList;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;
/**
 * 服务器实体微优化Mixin
 * 优化实体乘客列表的初始化
 */
@Mixin(ServerEntity.class)
public class CeresServerEntityOptMixin {
    /**
     * 使用Guava的ImmutableList替代JDK的空集合
     *
     * <p>这是一个微小的优化，但在大多数情况下，实体的乘客列表通常是空的。
     * 此外，它使用的是Guava的ImmutableList类型，但构造函数使用的是JDK（Java 9之前）的空集合。
     * 通过在这里使用Guava的集合类型，这个检查通常可以简化为简单的引用相等检查，这是非常廉价的。</p>
     *
     * @return 空的不可变列表
     */
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Collections;emptyList()Ljava/util/List;"))
    public List<Entity> construct$initialPassengersListIsGuavaImmutableList() {
        return ImmutableList.of();
    }
}
