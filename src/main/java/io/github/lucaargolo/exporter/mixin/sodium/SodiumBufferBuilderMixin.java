package io.github.lucaargolo.exporter.mixin.sodium;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.ExporterClient;
import io.github.lucaargolo.exporter.RenderInfo;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.NormalAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.minecraft.util.FastColor;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = SodiumBufferBuilder.class, remap = false)
public class SodiumBufferBuilderMixin {

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "vertex(FFFFFFFFFIIFFF)V", locals = LocalCapture.CAPTURE_FAILSOFT)
    public void test(float x, float y, float z, float red, float green, float blue, float alpha, float texU, float texV, int overlayUV, int lightmapUV, float normalX, float normalY, float normalZ, CallbackInfo ci) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            RenderInfo info = ExporterClient.MARKED_CONSUMERS.get(this);
            ExporterClient.captureVertex(info, x, y, z);
            ExporterClient.captureUv(info, texU, texV);
            ExporterClient.captureRgb(info, red, green, blue, alpha);
            ExporterClient.captureNormal(info, normalX, normalY, normalZ);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "vertex(DDD)Lcom/mojang/blaze3d/vertex/VertexConsumer;")
    public void captureVertex(double x, double y, double z, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            RenderInfo info = ExporterClient.MARKED_CONSUMERS.get(this);
            ExporterClient.captureVertex(info, (float) x, (float) y, (float) z);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "uv")
    public void captureUv(float u, float v, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            RenderInfo info = ExporterClient.MARKED_CONSUMERS.get(this);
            ExporterClient.captureUv(info, u, v);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;")
    public void captureColor(int red, int green, int blue, int alpha, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            RenderInfo info = ExporterClient.MARKED_CONSUMERS.get(this);
            ExporterClient.captureRgb(info, red/255f, green/255f, blue/255f, alpha/255f);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "color(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;")
    public void captureColor(int argb, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            RenderInfo info = ExporterClient.MARKED_CONSUMERS.get(this);
            ExporterClient.captureRgb(info, FastColor.ARGB32.red(argb)/255f, FastColor.ARGB32.green(argb)/255f, FastColor.ARGB32.blue(argb)/255f, FastColor.ARGB32.alpha(argb)/255f);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "normal")
    public void captureNormal(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            RenderInfo info = ExporterClient.MARKED_CONSUMERS.get(this);
            ExporterClient.captureNormal(info, x, y, z);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "endVertex", locals = LocalCapture.CAPTURE_FAILSOFT)
    public void test(CallbackInfo ci) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            RenderInfo info = ExporterClient.MARKED_CONSUMERS.get(this);
            ExporterClient.endCapture(info);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(at = @At("HEAD"), method = "push")
    public void test(MemoryStack stack, long ptr, int count, VertexFormatDescription format, CallbackInfo ci) {
        if(ExporterClient.MARKED_BUFFER != null && ExporterClient.MARKED_CONSUMERS.containsKey(this)) {
            RenderInfo info = ExporterClient.MARKED_CONSUMERS.get(this);
            long pointer = ptr;
            long stride = format.stride();
            for(int i = 0; i < count; i++) {
                if(format.containsElement(CommonVertexAttribute.POSITION)) {
                    long positionOffset = format.getElementOffset(CommonVertexAttribute.POSITION);
                    float x = PositionAttribute.getX(pointer + positionOffset);
                    float y = PositionAttribute.getY(pointer + positionOffset);
                    float z = PositionAttribute.getZ(pointer + positionOffset);
                    ExporterClient.captureVertex(info, x, y, z);
                }
                if(format.containsElement(CommonVertexAttribute.TEXTURE)) {
                    long textureOffset = format.getElementOffset(CommonVertexAttribute.TEXTURE);
                    float u = TextureAttribute.getU(pointer + textureOffset);
                    float v = TextureAttribute.getV(pointer + textureOffset);
                    ExporterClient.captureUv(info, u, v);
                }
                if(format.containsElement(CommonVertexAttribute.COLOR)) {
                    long colorOffset = format.getElementOffset(CommonVertexAttribute.COLOR);
                    int color = ColorAttribute.get(pointer + colorOffset);
                    ExporterClient.captureRgb(info, FastColor.ABGR32.red(color)/255f, FastColor.ABGR32.green(color)/255f, FastColor.ABGR32.blue(color)/255f, FastColor.ABGR32.alpha(color)/255f);
                }
                if(format.containsElement(CommonVertexAttribute.NORMAL)) {
                    long normalOffset = format.getElementOffset(CommonVertexAttribute.NORMAL);
                    int normal = NormalAttribute.get(pointer + normalOffset);
                    ExporterClient.captureNormal(info, NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal));
                }
                ExporterClient.endCapture(info);
                pointer += stride;
            }
        }
    }

}
