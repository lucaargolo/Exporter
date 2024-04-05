package io.github.lucaargolo.exporter.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.utils.ModelBuilder;
import io.github.lucaargolo.exporter.utils.helper.BufferHelper;
import io.github.lucaargolo.exporter.utils.info.RenderInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BufferBuilder.class)
public class BufferBuilderMixin {

    @Inject(at = @At("HEAD"), method = "vertex(FFFFFFFFFIIFFF)V", locals = LocalCapture.CAPTURE_FAILSOFT)
    public void test(float x, float y, float z, float red, float green, float blue, float alpha, float texU, float texV, int overlayUV, int lightmapUV, float normalX, float normalY, float normalZ, CallbackInfo ci) {
        RenderInfo info = BufferHelper.getInfo((VertexConsumer) this);
        if(info != null) {
            ModelBuilder.captureVertex(info, x, y, z);
            ModelBuilder.captureUv(info, texU, texV);
            ModelBuilder.captureRgb(info, red, green, blue, alpha);
            ModelBuilder.captureNormal(info, normalX, normalY, normalZ);
        }
    }

    @Inject(at = @At("HEAD"), method = "endVertex", locals = LocalCapture.CAPTURE_FAILSOFT)
    public void test(CallbackInfo ci) {
        RenderInfo info = BufferHelper.getInfo((VertexConsumer) this);
        if(info != null) {
            ModelBuilder.endCapture(info);
        }
    }

}
