package io.github.lucaargolo.exporter.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.lucaargolo.exporter.ExporterClient;
import org.joml.Vector3f;
import org.joml.Vector4i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BufferBuilder.class)
public class BufferBuilderMixin {

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "vertex(FFFFFFFFFIIFFF)V", locals = LocalCapture.CAPTURE_FAILSOFT)
    public void test(float x, float y, float z, float red, float green, float blue, float alpha, float texU, float texV, int overlayUV, int lightmapUV, float normalX, float normalY, float normalZ, CallbackInfo ci) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            ExporterClient.captureVertex(ExporterClient.MARKED_CONSUMERS.getInt(this), x, y, z, texU, texV);
        }
    }

}
