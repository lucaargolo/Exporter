package io.github.lucaargolo.exporter.compat.sodium;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.ExtendedBufferBuilder;
import net.minecraft.client.Minecraft;

public class FullSodiumCompat extends SodiumCompat.Impl {

    private boolean lastEntityCulling;

    @Override
    public VertexConsumer collectBuffer(VertexConsumer buffer) {
        if(buffer instanceof ExtendedBufferBuilder extendedBuffer) {
            buffer = extendedBuffer.sodium$getDelegate();
        }
        return buffer;
    }

    @Override
    public void setup() {
        var options = SodiumClientMod.options();
        lastEntityCulling = options.performance.useEntityCulling;
        options.performance.useEntityCulling = false;
        var minecraft = Minecraft.getInstance();
        minecraft.levelRenderer.needsUpdate();
    }

    @Override
    public void clear() {
        var options = SodiumClientMod.options();
        options.performance.useEntityCulling = lastEntityCulling;
        var minecraft = Minecraft.getInstance();
        minecraft.levelRenderer.needsUpdate();
    }

    @Override
    public boolean isPresent() {
        return true;
    }

}