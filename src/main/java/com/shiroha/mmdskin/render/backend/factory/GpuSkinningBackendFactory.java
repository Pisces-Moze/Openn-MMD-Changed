package com.shiroha.mmdskin.render.backend.factory;

import com.shiroha.mmdskin.bridge.runtime.NativeRenderBackendPort;
import com.shiroha.mmdskin.bridge.runtime.PlatformCapabilityPort;
import com.shiroha.mmdskin.model.runtime.ModelInstance;
import com.shiroha.mmdskin.render.backend.gpu.GpuSkinningModelInstance;
import com.shiroha.mmdskin.render.backend.mode.ModelInstanceFactory;
import com.shiroha.mmdskin.render.backend.mode.RenderCategory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * GPU 蒙皮模型工厂。
 */
public class GpuSkinningBackendFactory implements ModelInstanceFactory {
    private static final Logger logger = LogManager.getLogger();

    private static final int PRIORITY = 10;
    private final NativeRenderBackendPort nativeRenderBackendPort;
    private final PlatformCapabilityPort platformCapabilityPort;

    public GpuSkinningBackendFactory(NativeRenderBackendPort nativeRenderBackendPort,
                                     PlatformCapabilityPort platformCapabilityPort) {
        if (nativeRenderBackendPort == null) {
            throw new IllegalArgumentException("nativeRenderBackendPort cannot be null");
        }
        if (platformCapabilityPort == null) {
            throw new IllegalArgumentException("platformCapabilityPort cannot be null");
        }
        this.nativeRenderBackendPort = nativeRenderBackendPort;
        this.platformCapabilityPort = platformCapabilityPort;
    }

    @Override
    public RenderCategory getCategory() {
        return RenderCategory.GPU_SKINNING;
    }

    @Override
    public String getModeName() {
        return "GPU蒙皮";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isAvailable() {
        // The legacy GPU backend writes directly through OpenGL and therefore cannot
        // participate in Minecraft/Iris RenderType passes. Keep it unavailable until
        // its skinned output can be submitted through a VertexConsumer as well.
        return false;
    }

    @Override
    public boolean isEnabledInCurrentEnvironment() {
        return false;
    }

    @Override
    public ModelInstance createModel(String modelFilename, String modelDir, boolean isPMD, long layerCount) {
        if (!isAvailable()) {
            logger.warn("GPU 蒙皮不可用，无法创建模型");
            return null;
        }

        try {
            return GpuSkinningModelInstance.create(nativeRenderBackendPort, modelFilename, modelDir, isPMD, layerCount);
        } catch (Exception e) {
            logger.error("GPU 蒙皮模型创建失败: {}", modelFilename, e);
            return null;
        }
    }

    @Override
    public ModelInstance createModelFromHandle(long modelHandle, String modelDir) {
        try {
            return GpuSkinningModelInstance.createFromHandle(nativeRenderBackendPort, modelHandle, modelDir);
        } catch (Exception e) {
            logger.error("GPU 蒙皮模型（从句柄）创建失败", e);
            return null;
        }
    }
}
