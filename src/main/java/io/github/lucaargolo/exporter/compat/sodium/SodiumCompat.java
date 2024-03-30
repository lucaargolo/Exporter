package io.github.lucaargolo.exporter.compat.sodium;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.compat.Compat;

public abstract class SodiumCompat extends Compat.Impl {

    public abstract VertexConsumer collectBuffer(VertexConsumer buffer);

    public static class Impl extends SodiumCompat {

        @Override
        public VertexConsumer collectBuffer(VertexConsumer buffer) {
            return buffer;
        }

    }

}
