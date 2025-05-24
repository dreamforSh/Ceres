package com.xinian.ceres.mixin;

import com.xinian.ceres.Ceres;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Ceres Mixin插件
 * 用于控制Mixin的应用条件和兼容性
 */
public class CeresMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        Ceres.LOGGER.debug("Ceres Mixin plugin loaded for package: {}", mixinPackage);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    /**
     * 决定是否应该应用特定的Mixin
     * 用于处理与其他模组的兼容性问题
     *
     * @param targetClassName 目标类名
     * @param mixinClassName Mixin类名
     * @return 如果应该应用Mixin则为true
     */
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // 这个Mixin与Immersive Portals不兼容
        if (mixinClassName.contains("optimization.CeresChunkMapMixin")) {
            boolean shouldApply = LoadingModList.get().getModFileById("imm_ptl_core") == null;
            if (!shouldApply) {
                Ceres.LOGGER.info("Detected Immersive Portals mod, disabling CeresChunkMapMixin for compatibility");
            }
            return shouldApply;
        }

        // 检查与其他模组的兼容性
        if (mixinClassName.contains("pipeline.CeresVarint21FrameDecoderMixin")) {
            boolean shouldApply = !isIncompatibleNetworkModLoaded();
            if (!shouldApply) {
                Ceres.LOGGER.info("Detected incompatible network mod, disabling CeresVarint21FrameDecoderMixin");
            }
            return shouldApply;
        }

        // 默认应用所有其他Mixin
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // 不需要实现
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // 不需要实现
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // 不需要实现
    }

    /**
     * 检查是否加载了与网络优化不兼容的模组
     *
     * @return 如果加载了不兼容的模组则为true
     */
    private boolean isIncompatibleNetworkModLoaded() {
        // 检查已知的不兼容模组
        return LoadingModList.get().getModFileById("someNetworkMod") != null ||
                LoadingModList.get().getModFileById("anotherNetworkMod") != null;
    }

    /**
     * 获取已加载的模组列表的字符串表示
     * 用于调试目的
     *
     * @return 已加载模组的字符串表示
     */
    private String getLoadedModsString() {
        StringBuilder sb = new StringBuilder();
        LoadingModList.get().getMods().forEach(modInfo ->
                sb.append(modInfo.getModId()).append(":").append(modInfo.getVersion()).append(", ")
        );
        return sb.toString();
    }
}
