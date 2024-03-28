package io.github.lucaargolo.exporter.compat.minecraft;

import io.github.lucaargolo.exporter.compat.Compat;
import net.minecraft.client.Minecraft;

public class AmbientOcclusionCompat extends Compat.Impl {

    private boolean lastAo;

    @Override
    public void setup() {
        var minecraft = Minecraft.getInstance();
        lastAo = minecraft.options.ambientOcclusion().get();
        minecraft.options.ambientOcclusion().set(false);
    }

    @Override
    public void clear() {
        var minecraft = Minecraft.getInstance();
        minecraft.options.ambientOcclusion().set(lastAo);
    }

}
