package com.shiroha.mmdskin.render.pipeline;

import com.mojang.blaze3d.vertex.BufferUploader;
import org.lwjgl.opengl.GL46C;

/** Restores raw OpenGL state so Oculus/Iris can safely render following entities. */
public final class OpenGlStateSnapshot {
    private final int program = GL46C.glGetInteger(GL46C.GL_CURRENT_PROGRAM);
    private final int vao = GL46C.glGetInteger(GL46C.GL_VERTEX_ARRAY_BINDING);
    private final int arrayBuffer = GL46C.glGetInteger(GL46C.GL_ARRAY_BUFFER_BINDING);
    private final int elementBuffer = GL46C.glGetInteger(GL46C.GL_ELEMENT_ARRAY_BUFFER_BINDING);
    private final int activeTexture = GL46C.glGetInteger(GL46C.GL_ACTIVE_TEXTURE);
    private final int texture0;
    private final boolean blend = GL46C.glIsEnabled(GL46C.GL_BLEND);
    private final boolean depth = GL46C.glIsEnabled(GL46C.GL_DEPTH_TEST);
    private final boolean cull = GL46C.glIsEnabled(GL46C.GL_CULL_FACE);
    private final boolean depthMask = GL46C.glGetBoolean(GL46C.GL_DEPTH_WRITEMASK);
    private final int depthFunc = GL46C.glGetInteger(GL46C.GL_DEPTH_FUNC);
    private final int cullFace = GL46C.glGetInteger(GL46C.GL_CULL_FACE_MODE);
    private final int frontFace = GL46C.glGetInteger(GL46C.GL_FRONT_FACE);
    private final int equationRgb = GL46C.glGetInteger(GL46C.GL_BLEND_EQUATION_RGB);
    private final int equationAlpha = GL46C.glGetInteger(GL46C.GL_BLEND_EQUATION_ALPHA);
    private final int srcRgb = GL46C.glGetInteger(GL46C.GL_BLEND_SRC_RGB);
    private final int dstRgb = GL46C.glGetInteger(GL46C.GL_BLEND_DST_RGB);
    private final int srcAlpha = GL46C.glGetInteger(GL46C.GL_BLEND_SRC_ALPHA);
    private final int dstAlpha = GL46C.glGetInteger(GL46C.GL_BLEND_DST_ALPHA);
    private OpenGlStateSnapshot() {
        GL46C.glActiveTexture(GL46C.GL_TEXTURE0);
        texture0 = GL46C.glGetInteger(GL46C.GL_TEXTURE_BINDING_2D);
        GL46C.glActiveTexture(activeTexture);
    }
    public static OpenGlStateSnapshot capture() { return new OpenGlStateSnapshot(); }
    public void restore() {
        enabled(GL46C.GL_BLEND, blend); enabled(GL46C.GL_DEPTH_TEST, depth);
        enabled(GL46C.GL_CULL_FACE, cull); GL46C.glDepthMask(depthMask);
        GL46C.glDepthFunc(depthFunc); GL46C.glCullFace(cullFace); GL46C.glFrontFace(frontFace);
        GL46C.glBlendEquationSeparate(equationRgb, equationAlpha);
        GL46C.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        GL46C.glActiveTexture(GL46C.GL_TEXTURE0); GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, texture0);
        GL46C.glActiveTexture(activeTexture); GL46C.glBindVertexArray(vao);
        GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, arrayBuffer);
        GL46C.glBindBuffer(GL46C.GL_ELEMENT_ARRAY_BUFFER, elementBuffer);
        GL46C.glUseProgram(program); BufferUploader.reset();
    }
    private static void enabled(int cap, boolean value) {
        if (value) GL46C.glEnable(cap); else GL46C.glDisable(cap);
    }
}
