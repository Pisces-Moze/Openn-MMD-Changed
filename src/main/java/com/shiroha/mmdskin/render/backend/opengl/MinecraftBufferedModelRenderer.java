package com.shiroha.mmdskin.render.backend.opengl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shiroha.mmdskin.compat.iris.IrisCompat;
import com.shiroha.mmdskin.config.ConfigManager;
import com.shiroha.mmdskin.render.material.ModelMaterial;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Submits CPU-skinned PMX triangles through Minecraft/Iris entity buffers. */
final class MinecraftBufferedModelRenderer {
    private static final int SUB_MESH_STRIDE = 20;
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final ResourceLocation MISSING = TextureManager.INTENTIONAL_MISSING_TEXTURE;

    private MinecraftBufferedModelRenderer() {}

    static void render(OpenGlModelInstance target, Entity entity, float yaw, float pitch,
                       Vector3f translation, PoseStack stack, int packedLight,
                       MultiBufferSource buffers) {
        refresh(target);
        stack.pushPose();
        Quaternionf rotation = target.workingQuaternion();
        stack.mulPose(rotation.identity().rotateY(-yaw * ((float) Math.PI / 180.0f)));
        stack.mulPose(rotation.identity().rotateX(pitch * ((float) Math.PI / 180.0f)));
        stack.translate(translation.x, translation.y, translation.z);
        float scale = target.modelScaleValue();
        stack.scale(scale, scale, scale);

        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        int overlay = entity instanceof LivingEntity living
                ? LivingEntityRenderer.getOverlayCoords(living, 0.0f)
                : OverlayTexture.NO_OVERLAY;
        boolean shadowPass = IrisCompat.isRenderingShadows();

        for (int subMesh = 0; subMesh < target.subMeshCount; subMesh++) {
            int base = subMesh * SUB_MESH_STRIDE;
            int materialId = target.subMeshDataBuf.getInt(base);
            int first = target.subMeshDataBuf.getInt(base + 4);
            int count = target.subMeshDataBuf.getInt(base + 8);
            float baseAlpha = target.subMeshDataBuf.getFloat(base + 12);
            boolean visible = target.subMeshDataBuf.get(base + 16) != 0;
            boolean bothFace = target.subMeshDataBuf.get(base + 17) != 0;
            if (!visible || materialId < 0 || materialId >= target.mats.length) continue;

            float alpha = target.effectiveMaterialAlpha(materialId, baseAlpha);
            if (alpha < 0.001f) continue;
            ModelMaterial material = target.mats[materialId];
            ResourceLocation texture = material.minecraftTexture != null
                    ? material.minecraftTexture : MISSING;
            boolean cull = !bothFace && !"off".equalsIgnoreCase(material.lilCull);

            float lightFloor = Math.max(material.baseLightFloor(), material.unlitStrength());
            int materialLight = applyLightFloor(packedLight, lightFloor);
            emitTriangles(buffers.getBuffer(PmxRenderTypes.base(texture, cull,
                            material.lilRenderMode, material.lilAlphaCutoff)),
                    pose, normal, target, first, count, materialLight, overlay, alpha,
                    1.0f, 1.0f, 1.0f, false);

            if (!shadowPass && material.lilUseOutline && !material.isFacialFeature()) {
                float outlineWidth = material.lilOutlineWidth >= 0.0f
                        ? material.lilOutlineWidth * 0.01f
                        : ConfigManager.getToonOutlineWidth();
                float outlineAlpha = alpha * material.lilOutlineColor[3];
                if (outlineWidth > 0.000001f && outlineAlpha > 0.001f) {
                    ResourceLocation outlineTexture = material.minecraftOutlineTexture != null
                            ? material.minecraftOutlineTexture : texture;
                    emitOutlineTriangles(buffers.getBuffer(PmxRenderTypes.pmxOutline(outlineTexture)),
                            pose, normal, target, first, count, FULL_BRIGHT,
                            outlineAlpha, outlineWidth,
                            material.lilOutlineColor[0], material.lilOutlineColor[1],
                            material.lilOutlineColor[2]);
                }
            }

            if (!shadowPass && material.hasExplicitEmissionConfiguration()) {
                for (ModelMaterial.MinecraftEmissionLayer layer : material.minecraftEmissionLayers) {
                    if (layer.texture == null || layer.strength <= 0.001f) continue;
                    float glowAlpha = alpha * Math.min(1.0f, layer.strength);
                emitTriangles(buffers.getBuffer(PmxRenderTypes.emission(
                                    layer.texture, cull)),
                        pose, normal, target, first, count, FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, glowAlpha,
                            layer.color[0], layer.color[1], layer.color[2], true);
                }
            }
        }
        stack.popPose();
    }

    private static void refresh(OpenGlModelInstance target) {
        var backend = target.nativeBackendPort();
        long model = target.nativeModelHandle();
        int vectorBytes = target.vertexCount * 12;
        int uvBytes = target.vertexCount * 8;
        target.posBuffer.clear();
        target.norBuffer.clear();
        target.uv0Buffer.clear();
        backend.copyNativeDataToBuffer(target.posBuffer, backend.getPositionDataAddress(model), vectorBytes);
        backend.copyNativeDataToBuffer(target.norBuffer, backend.getNormalDataAddress(model), vectorBytes);
        backend.copyNativeDataToBuffer(target.uv0Buffer, backend.getUvDataAddress(model), uvBytes);
        target.loadMaterialMorphResults();
        target.subMeshDataBuf.clear();
        backend.batchGetSubMeshData(model, target.subMeshDataBuf);
    }

    private static void emitTriangles(VertexConsumer output, Matrix4f pose, Matrix3f normal,
                                      OpenGlModelInstance target, int first, int count,
                                      int light, int overlay, float alpha,
                                      float red, float green, float blue,
                                      boolean stableLightNormal) {
        int end = count - count % 3;
        for (int i = 0; i < end; i++) {
            int vertex = index(target.indexBuffer, first + i, target.indexElementSize);
            emit(output, pose, normal, target.posBuffer, target.norBuffer,
                    target.uv0Buffer, vertex, light, overlay, alpha,
                    red, green, blue, stableLightNormal);
        }
    }

    /** lilToon-style inverted hull: expand along authored normals and reverse winding. */
    private static void emitOutlineTriangles(VertexConsumer output, Matrix4f pose, Matrix3f normal,
                                             OpenGlModelInstance target, int first, int count,
                                             int light, float alpha, float width,
                                             float red, float green, float blue) {
        int end = count - count % 3;
        for (int i = 0; i < end; i += 3) {
            int a = index(target.indexBuffer, first + i, target.indexElementSize);
            int b = index(target.indexBuffer, first + i + 1, target.indexElementSize);
            int c = index(target.indexBuffer, first + i + 2, target.indexElementSize);
            emitOutline(output, pose, normal, target.posBuffer, target.norBuffer,
                    target.uv0Buffer, a, light, alpha, width, red, green, blue);
            emitOutline(output, pose, normal, target.posBuffer, target.norBuffer,
                    target.uv0Buffer, c, light, alpha, width, red, green, blue);
            emitOutline(output, pose, normal, target.posBuffer, target.norBuffer,
                    target.uv0Buffer, b, light, alpha, width, red, green, blue);
        }
    }

    private static void emitOutline(VertexConsumer output, Matrix4f pose, Matrix3f normal,
                                    ByteBuffer positions, ByteBuffer normals, ByteBuffer uvs,
                                    int vertex, int light, float alpha, float width,
                                    float red, float green, float blue) {
        int vector = vertex * 12;
        int uv = vertex * 8;
        float nx = normals.getFloat(vector);
        float ny = normals.getFloat(vector + 4);
        float nz = normals.getFloat(vector + 8);
        float lengthSquared = nx * nx + ny * ny + nz * nz;
        if (lengthSquared > 1.0E-8f) {
            float inverseLength = (float) (1.0 / Math.sqrt(lengthSquared));
            nx *= inverseLength;
            ny *= inverseLength;
            nz *= inverseLength;
        }
        output.vertex(pose,
                        positions.getFloat(vector) + nx * width,
                        positions.getFloat(vector + 4) + ny * width,
                        positions.getFloat(vector + 8) + nz * width)
                .color(channel(red), channel(green), channel(blue),
                        Math.round(Math.min(1.0f, alpha) * 255.0f))
                .uv(uvs.getFloat(uv), 1.0f - uvs.getFloat(uv + 4))
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, -nx, -ny, -nz)
                .endVertex();
    }

    private static int index(ByteBuffer data, int index, int size) {
        int offset = index * size;
        return switch (size) {
            case 1 -> Byte.toUnsignedInt(data.get(offset));
            case 2 -> Short.toUnsignedInt(data.getShort(offset));
            case 4 -> data.getInt(offset);
            default -> throw new IllegalStateException("Unsupported PMX index size: " + size);
        };
    }

    private static void emit(VertexConsumer output, Matrix4f pose, Matrix3f normal,
                             ByteBuffer positions, ByteBuffer normals, ByteBuffer uvs,
                             int vertex, int light, int overlay, float alpha,
                             float red, float green, float blue,
                             boolean stableLightNormal) {
        int vector = vertex * 12;
        int uv = vertex * 8;
        VertexConsumer pending = output
                .vertex(pose, positions.getFloat(vector), positions.getFloat(vector + 4),
                        positions.getFloat(vector + 8))
                .color(channel(red), channel(green), channel(blue),
                        Math.round(Math.min(1.0f, alpha) * 255.0f))
                // MC-MMD-rust flips decoded images vertically before the legacy OpenGL
                // upload. Minecraft's DynamicTexture keeps the source row order, so the
                // buffered path must mirror V to preserve the PMX-authored UV layout.
                .uv(uvs.getFloat(uv), 1.0f - uvs.getFloat(uv + 4))
                .overlayCoords(overlay)
                .uv2(light);
        if (stableLightNormal) {
            pending.normal(0.0f, 1.0f, 0.0f);
        } else {
            pending.normal(normal, normals.getFloat(vector), normals.getFloat(vector + 4),
                    normals.getFloat(vector + 8));
        }
        pending.endVertex();
    }

    private static int channel(float value) {
        return Math.round(Math.max(0.0f, Math.min(1.0f, value)) * 255.0f);
    }

    private static int applyLightFloor(int packedLight, float floor) {
        if (floor <= 0.0f) return packedLight;
        int minimum = Math.round(Math.min(1.0f, floor) * 15.0f);
        return LightTexture.pack(
                Math.max(LightTexture.block(packedLight), minimum),
                Math.max(LightTexture.sky(packedLight), minimum));
    }
}
