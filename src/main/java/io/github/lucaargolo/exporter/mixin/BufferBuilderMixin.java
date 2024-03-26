package io.github.lucaargolo.exporter.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import io.github.lucaargolo.exporter.ExporterClient;
import org.spongepowered.asm.mixin.Mixin;
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
            int glId = ExporterClient.MARKED_CONSUMERS.getInt(this);
            ExporterClient.captureVertex(glId, x, y, z);
            ExporterClient.captureUv(glId, texU, texV);
            ExporterClient.captureRgb(glId, red, green, blue, alpha);
            ExporterClient.captureNormal(glId, normalX, normalY, normalZ);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "endVertex", locals = LocalCapture.CAPTURE_FAILSOFT)
    public void test(CallbackInfo ci) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            int glId = ExporterClient.MARKED_CONSUMERS.getInt(this);
            ExporterClient.endCapture(glId);
        }
    }

}
