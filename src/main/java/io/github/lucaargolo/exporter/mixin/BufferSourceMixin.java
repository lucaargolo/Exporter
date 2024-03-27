package io.github.lucaargolo.exporter.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.ExporterClient;
import io.github.lucaargolo.exporter.RenderInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL11;
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
                var cullShard = composite.state().cullState;
                var emptyShard = composite.state().textureState;
                var transparencyShard = composite.state().transparencyState;
                if(emptyShard instanceof RenderStateShard.TextureStateShard textureShard) {
                    int oldId = RenderSystem.getShaderTexture(0);
                    textureShard.setupRenderState();
                    int glId = RenderSystem.getShaderTexture(0);

                    boolean oldCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
                    cullShard.setupRenderState();
                    boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
                    if(oldCull) {
                        RenderSystem.enableCull();
                    }else{
                        RenderSystem.disableCull();
                    }

                    boolean oldBlend = GL11.glIsEnabled(GL11.GL_BLEND);
                    transparencyShard.setupRenderState();
                    boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
                    if(oldBlend) {
                        RenderSystem.enableBlend();
                    }else{
                        RenderSystem.disableBlend();
                    }

                    String name = composite.toString();
                    RenderInfo info = new RenderInfo(glId, RenderInfo.Type.fromName(name), cull, blend);
                    ExporterClient.MARKED_CONSUMERS.put(cir.getReturnValue(), info);
                    RenderSystem.setShaderTexture(0, oldId);
                }
                //TODO: Add support to MultiTextureShard
            }
        }
    }

}
