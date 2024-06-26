package io.github.lucaargolo.exporter.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.ExporterClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MultiBufferSource.BufferSource.class, priority = 500)
public class BufferSourceMixin {

    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    @Inject(at = @At("RETURN"), method = "getBuffer")
    public void test(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER == this) {
            ExporterClient.captureBuffer(renderType, cir.getReturnValue());
        }
    }

}
