package io.github.lucaargolo.exporter.mixin.sodium;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.compat.Compat;
import io.github.lucaargolo.exporter.utils.ModelBuilder;
import io.github.lucaargolo.exporter.utils.helper.BufferHelper;
import io.github.lucaargolo.exporter.utils.info.RenderInfo;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.ExtendedBufferBuilder;
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
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = SodiumBufferBuilder.class)
public class SodiumBufferBuilderMixin {

    @Shadow(remap = false) @Final private ExtendedBufferBuilder builder;

    @Inject(at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/vertex/buffer/ExtendedBufferBuilder;sodium$moveToNextVertex()V", shift = At.Shift.BEFORE, remap = false), method = "endVertex", locals = LocalCapture.CAPTURE_FAILSOFT)
    public void test(CallbackInfo ci) {
        RenderInfo info = BufferHelper.getInfo((VertexConsumer) this);
        if(info != null) {
            long pointer = MemoryUtil.memAddress(this.builder.sodium$getBuffer(), this.builder.sodium$getElementOffset());
            var format = this.builder.sodium$getFormatDescription();
            if(format.containsElement(CommonVertexAttribute.POSITION)) {
                long positionOffset = format.getElementOffset(CommonVertexAttribute.POSITION);
                float x = PositionAttribute.getX(pointer + positionOffset);
                float y = PositionAttribute.getY(pointer + positionOffset);
                float z = PositionAttribute.getZ(pointer + positionOffset);
                ModelBuilder.captureVertex(info, x, y, z);
            }
            if(format.containsElement(CommonVertexAttribute.NORMAL)) {
                long normalOffset = format.getElementOffset(CommonVertexAttribute.NORMAL);
                int normal = NormalAttribute.get(pointer + normalOffset);
                ModelBuilder.captureNormal(info, NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal));
            }
            Compat.IRIS.captureTangent(info, format, pointer);
            if(format.containsElement(CommonVertexAttribute.TEXTURE)) {
                long textureOffset = format.getElementOffset(CommonVertexAttribute.TEXTURE);
                float u = TextureAttribute.getU(pointer + textureOffset);
                float v = TextureAttribute.getV(pointer + textureOffset);
                ModelBuilder.captureUv(info, u, v);
            }
            if(format.containsElement(CommonVertexAttribute.COLOR)) {
                long colorOffset = format.getElementOffset(CommonVertexAttribute.COLOR);
                int color = ColorAttribute.get(pointer + colorOffset);
                ModelBuilder.captureRgb(info, FastColor.ABGR32.red(color)/255f, FastColor.ABGR32.green(color)/255f, FastColor.ABGR32.blue(color)/255f, FastColor.ABGR32.alpha(color)/255f);
            }
            ModelBuilder.endCapture(info);
        }
    }

    @Inject(at = @At("HEAD"), method = "push", remap = false)
    public void test(MemoryStack stack, long ptr, int count, VertexFormatDescription format, CallbackInfo ci) {
        RenderInfo info = BufferHelper.getInfo((VertexConsumer) this);
        if(info != null) {
            long pointer = ptr;
            long stride = format.stride();
            for(int i = 0; i < count; i++) {
                if(format.containsElement(CommonVertexAttribute.POSITION)) {
                    long positionOffset = format.getElementOffset(CommonVertexAttribute.POSITION);
                    float x = PositionAttribute.getX(pointer + positionOffset);
                    float y = PositionAttribute.getY(pointer + positionOffset);
                    float z = PositionAttribute.getZ(pointer + positionOffset);
                    ModelBuilder.captureVertex(info, x, y, z);
                }
                if(format.containsElement(CommonVertexAttribute.NORMAL)) {
                    long normalOffset = format.getElementOffset(CommonVertexAttribute.NORMAL);
                    int normal = NormalAttribute.get(pointer + normalOffset);
                    ModelBuilder.captureNormal(info, NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal));
                }
                Compat.IRIS.captureTangent(info, format, pointer);
                if(format.containsElement(CommonVertexAttribute.TEXTURE)) {
                    long textureOffset = format.getElementOffset(CommonVertexAttribute.TEXTURE);
                    float u = TextureAttribute.getU(pointer + textureOffset);
                    float v = TextureAttribute.getV(pointer + textureOffset);
                    ModelBuilder.captureUv(info, u, v);
                }
                if(format.containsElement(CommonVertexAttribute.COLOR)) {
                    long colorOffset = format.getElementOffset(CommonVertexAttribute.COLOR);
                    int color = ColorAttribute.get(pointer + colorOffset);
                    ModelBuilder.captureRgb(info, FastColor.ABGR32.red(color)/255f, FastColor.ABGR32.green(color)/255f, FastColor.ABGR32.blue(color)/255f, FastColor.ABGR32.alpha(color)/255f);
                }
                ModelBuilder.endCapture(info);
                pointer += stride;
            }
        }
    }

}
