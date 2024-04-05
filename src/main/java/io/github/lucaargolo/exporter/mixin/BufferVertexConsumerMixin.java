package io.github.lucaargolo.exporter.mixin;

import com.mojang.blaze3d.vertex.BufferVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.utils.ModelBuilder;
import io.github.lucaargolo.exporter.utils.helper.BufferHelper;
import io.github.lucaargolo.exporter.utils.info.RenderInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BufferVertexConsumer.class)
public interface BufferVertexConsumerMixin {

    @Inject(at = @At("HEAD"), method = "vertex")
    default void captureVertex(double x, double y, double z, CallbackInfoReturnable<VertexConsumer> cir) {
        RenderInfo info = BufferHelper.getInfo((VertexConsumer) this);
        if(info != null) {
            ModelBuilder.captureVertex(info, (float) x, (float) y, (float) z);
        }
    }

    @Inject(at = @At("HEAD"), method = "uv")
    default void captureUv(float u, float v, CallbackInfoReturnable<VertexConsumer> cir) {
        RenderInfo info = BufferHelper.getInfo((VertexConsumer) this);
        if(info != null) {
            ModelBuilder.captureUv(info, u, v);
        }
    }

    @Inject(at = @At("HEAD"), method = "color")
    default void captureColor(int red, int green, int blue, int alpha, CallbackInfoReturnable<VertexConsumer> cir) {
        RenderInfo info = BufferHelper.getInfo((VertexConsumer) this);
        if(info != null) {
            ModelBuilder.captureRgb(info, red/255f, green/255f, blue/255f, alpha/255f);
        }
    }

    @Inject(at = @At("HEAD"), method = "normal")
    default void captureNormal(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        RenderInfo info = BufferHelper.getInfo((VertexConsumer) this);
        if(info != null) {
            ModelBuilder.captureNormal(info, x, y, z);
        }
    }

}
