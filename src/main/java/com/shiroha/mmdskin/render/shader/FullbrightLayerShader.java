package com.shiroha.mmdskin.render.shader;

import com.shiroha.mmdskin.util.AssetsUtil;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL46C;

/** Draws texture layers without Minecraft world-light attenuation. */
public final class FullbrightLayerShader {
    private static volatile FullbrightLayerShader instance;

    private int program;
    private int positionLocation;
    private int uvLocation;
    private int projectionLocation;
    private int modelViewLocation;
    private int samplerLocation;
    private int intensityLocation;
    private int opacityLocation;
    private int depthBiasLocation;

    private FullbrightLayerShader() {
    }

    public static FullbrightLayerShader getOrCreate() {
        FullbrightLayerShader current = instance;
        if (current != null && current.program > 0) {
            return current;
        }
        synchronized (FullbrightLayerShader.class) {
            current = instance;
            if (current == null || current.program <= 0) {
                current = new FullbrightLayerShader();
                if (!current.init()) {
                    return null;
                }
                instance = current;
            }
            return current;
        }
    }

    private boolean init() {
        program = ShaderCompiler.compileRenderProgram(
                AssetsUtil.getAssetsAsString("shader/fullbright_layer.vert.glsl"),
                AssetsUtil.getAssetsAsString("shader/fullbright_layer.frag.glsl"),
                "MMD fullbright layer");
        if (program <= 0) {
            return false;
        }
        positionLocation = GL46C.glGetAttribLocation(program, "Position");
        uvLocation = GL46C.glGetAttribLocation(program, "UV0");
        projectionLocation = GL46C.glGetUniformLocation(program, "ProjMat");
        modelViewLocation = GL46C.glGetUniformLocation(program, "ModelViewMat");
        samplerLocation = GL46C.glGetUniformLocation(program, "Sampler0");
        intensityLocation = GL46C.glGetUniformLocation(program, "Intensity");
        opacityLocation = GL46C.glGetUniformLocation(program, "Opacity");
        depthBiasLocation = GL46C.glGetUniformLocation(program, "DepthBias");
        return true;
    }

    public void use(FloatBuffer projection, FloatBuffer modelView) {
        GL46C.glUseProgram(program);
        projection.position(0);
        modelView.position(0);
        GL46C.glUniformMatrix4fv(projectionLocation, false, projection);
        GL46C.glUniformMatrix4fv(modelViewLocation, false, modelView);
        GL46C.glUniform1i(samplerLocation, 0);
    }

    public void setAppearance(float intensity, float opacity, float depthBias) {
        GL46C.glUniform1f(intensityLocation, intensity);
        GL46C.glUniform1f(opacityLocation, opacity);
        GL46C.glUniform1f(depthBiasLocation, depthBias);
    }

    public int positionLocation() {
        return positionLocation;
    }

    public int uvLocation() {
        return uvLocation;
    }
}
