package io.github.lucaargolo.exporter.mixin.iris;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.utils.helper.BufferHelper;
import net.coderbot.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.coderbot.batchedentityrendering.impl.OldFullyBufferedMultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FullyBufferedMultiBufferSource.class, OldFullyBufferedMultiBufferSource.class})
public class BufferSourceMixin {

    @Inject(at = @At("RETURN"), method = "getBuffer")
    public void test(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
        if(BufferHelper.isMarked((MultiBufferSource) this)) {
            BufferHelper.captureBuffer(renderType, cir.getReturnValue());
        }
    }

}
