package io.github.lucaargolo.exporter.compat.iris;

import io.github.lucaargolo.exporter.RenderInfo;
import io.github.lucaargolo.exporter.compat.Compat;
import net.minecraft.client.renderer.RenderType;

public abstract class IrisCompat extends Compat.Impl {

    public abstract int normalTexture(int glId);

    public abstract int specularTexture(int glId);

    public abstract void captureTangent(RenderInfo info, Object format, long pointer);

    public abstract RenderType unwrapRenderType(RenderType renderType);

    public static class Impl extends IrisCompat {

        @Override
        public int normalTexture(int glId) {
            return -1;
        }

        @Override
        public int specularTexture(int glId) {
            return -1;
        }

        @Override
        public void captureTangent(RenderInfo info, Object object, long pointer) {

        }

        @Override
        public RenderType unwrapRenderType(RenderType renderType) {
            return renderType;
        }
    }

}
