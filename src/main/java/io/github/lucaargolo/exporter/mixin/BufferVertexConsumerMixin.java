package io.github.lucaargolo.exporter.mixin;

import com.mojang.blaze3d.vertex.BufferVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.ExporterClient;
import io.github.lucaargolo.exporter.RenderInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BufferVertexConsumer.class)
public interface BufferVertexConsumerMixin {

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "vertex")
    default void captureVertex(double x, double y, double z, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            RenderInfo info = ExporterClient.MARKED_CONSUMERS.get(this);
            ExporterClient.captureVertex(info, (float) x, (float) y, (float) z);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "uv")
    default void captureUv(float u, float v, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            RenderInfo info = ExporterClient.MARKED_CONSUMERS.get(this);
            ExporterClient.captureUv(info, u, v);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "color")
    default void captureColor(int red, int green, int blue, int alpha, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            RenderInfo info = ExporterClient.MARKED_CONSUMERS.get(this);
            ExporterClient.captureRgb(info, red/255f, green/255f, blue/255f, alpha/255f);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "normal")
    default void captureNormal(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            RenderInfo info = ExporterClient.MARKED_CONSUMERS.get(this);
            ExporterClient.captureNormal(info, x, y, z);
        }
    }

}
