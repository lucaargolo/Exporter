package io.github.lucaargolo.exporter.utils.helper;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.compat.Compat;
import io.github.lucaargolo.exporter.utils.info.RenderInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public class BufferHelper {

    private static MultiBufferSource MARKED_BUFFER;
    private static final Map<VertexConsumer, RenderInfo> MARKED_CONSUMERS = new HashMap<>();

    private static MultiBufferSource BACKUP_BUFFER;
    private static final Map<VertexConsumer, RenderInfo> BACKUP_CONSUMERS = new HashMap<>();

    public static RenderInfo getInfo(VertexConsumer consumer) {
        return MARKED_BUFFER == null ? null : MARKED_CONSUMERS.get(consumer);
    }

    public static void markBuffer(MultiBufferSource bufferSource) {
        MARKED_BUFFER = bufferSource;
    }

    public static boolean isMarked(MultiBufferSource bufferSource) {
        return MARKED_BUFFER == bufferSource;
    }

    public static void clearBuffer() {
        MARKED_BUFFER = null;
        MARKED_CONSUMERS.clear();
    }

    public static void backupBuffers() {
        BACKUP_BUFFER = MARKED_BUFFER;
        BACKUP_CONSUMERS.clear();
        BACKUP_CONSUMERS.putAll(MARKED_CONSUMERS);
    }

    public static void restoreBuffer() {
        MARKED_BUFFER = BACKUP_BUFFER;
        MARKED_CONSUMERS.clear();
        MARKED_CONSUMERS.putAll(BACKUP_CONSUMERS);
    }

    public static void captureBuffer(RenderType renderType, VertexConsumer buffer) {
        renderType = Compat.IRIS.unwrapRenderType(renderType);
        if(renderType instanceof RenderType.CompositeRenderType composite) {
            var cullShard = composite.state().cullState;
            var emptyShard = composite.state().textureState;
            var transparencyShard = composite.state().transparencyState;

            int glId = -1;
            int normalGlId = -1;
            int specularGlId = -1;
            if(emptyShard instanceof RenderStateShard.TextureStateShard textureShard && textureShard.texture.isPresent()) {
                Minecraft minecraft = Minecraft.getInstance();
                TextureManager manager = minecraft.getTextureManager();
                AbstractTexture texture = manager.getTexture(textureShard.texture.get());
                glId = texture.getId();
                normalGlId = Compat.IRIS.normalTexture(glId);
                specularGlId = Compat.IRIS.specularTexture(glId);
            }

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
            RenderInfo render = new RenderInfo(glId, normalGlId, specularGlId, renderType.mode(), RenderInfo.Type.fromName(name), cull, blend);
            buffer = Compat.SODIUM.collectBuffer(buffer);
            MARKED_CONSUMERS.put(buffer, render);

            //TODO: Add support to MultiTextureShard
        }
    }

}
