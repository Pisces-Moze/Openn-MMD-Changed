package com.shiroha.mmdskin.render.shader;

import com.shiroha.mmdskin.util.AssetsUtil;

/**
 * OpenGL/GLSL compatibility port of the avatar-oriented lilToon material model.
 * Unity-specific passes are intentionally replaced by Minecraft-safe equivalents.
 */
public final class LilToonShaderCpu extends ToonShaderCpu {
    private static final String LILTOON_FRAGMENT_SHADER =
            AssetsUtil.getAssetsAsString("shader/liltoon_compat_main.frag.glsl");

    @Override
    protected String getMainFragmentShader() {
        return LILTOON_FRAGMENT_SHADER;
    }

    @Override
    protected String getShaderName() {
        return "LilToonCompatShaderCpu";
    }
}
