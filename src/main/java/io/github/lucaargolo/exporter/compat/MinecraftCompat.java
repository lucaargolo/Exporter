package io.github.lucaargolo.exporter.compat;

import net.minecraft.client.Minecraft;

public class MinecraftCompat extends Compat.Impl {

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

    @Override
    public boolean isPresent() {
        return true;
    }

}
