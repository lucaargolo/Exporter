package io.github.lucaargolo.exporter.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.ExporterClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiBufferSource.BufferSource.class)
public class BufferSourceMixin {

    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    @Inject(at = @At("RETURN"), method = "getBuffer")
    public void test(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
        if(ExporterClient.MARKED_BUFFER == this) {
            if(renderType instanceof RenderType.CompositeRenderType composite) {
                if(composite.state().textureState instanceof RenderStateShard.TextureStateShard textureShard) {
                    int oldId = RenderSystem.getShaderTexture(0);
                    textureShard.setupRenderState();
                    int glId = RenderSystem.getShaderTexture(0);
                    ExporterClient.MARKED_CONSUMERS.put(cir.getReturnValue(), glId);
                    RenderSystem.setShaderTexture(0, oldId);
                }
            }
        }
    }

}
