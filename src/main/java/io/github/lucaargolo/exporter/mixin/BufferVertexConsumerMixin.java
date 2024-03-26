package io.github.lucaargolo.exporter.mixin;

import com.mojang.blaze3d.vertex.BufferVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.ExporterClient;
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
            int glId = ExporterClient.MARKED_CONSUMERS.getInt(this);
            ExporterClient.captureVertex(glId, (float) x, (float) y, (float) z);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "uv")
    default void captureVertex(float u, float v, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            int glId = ExporterClient.MARKED_CONSUMERS.getInt(this);
            ExporterClient.captureUv(glId, u, v);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "color")
    default void captureVertex(int red, int green, int blue, int alpha, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            int glId = ExporterClient.MARKED_CONSUMERS.getInt(this);
            ExporterClient.captureRgb(glId, red/255f, green/255f, blue/255f, alpha/255f);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "normal")
    default void captureVertex(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            int glId = ExporterClient.MARKED_CONSUMERS.getInt(this);
            ExporterClient.captureNormal(glId, x, y, z);
        }
    }

}
