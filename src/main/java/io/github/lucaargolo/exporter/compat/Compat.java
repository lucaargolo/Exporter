package io.github.lucaargolo.exporter.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.compat.custom.FlywheelCompat;
import io.github.lucaargolo.exporter.compat.custom.IrisCompat;
import io.github.lucaargolo.exporter.compat.custom.MinecraftCompat;
import io.github.lucaargolo.exporter.compat.custom.SodiumCompat;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class Compat {

    private static final List<Compat> ALL = new ArrayList<>();

    public static final Compat MINECRAFT = addCompat("minecraft", () -> MinecraftCompat::new);
    public static final Compat FLYWHEEL = addCompat("flywheel", () -> FlywheelCompat::new);
    public static final Compat SODIUM = addCompat("sodium", () -> SodiumCompat::new);
    public static final Compat IRIS = addCompat("iris", () -> IrisCompat::new);

    public static VertexConsumer collectAllBuffers(VertexConsumer buffer) {
        for(Compat c : ALL) {
            buffer = c.collectBuffer(buffer);
        }
        return buffer;
    }

    public static void setupAll() {
        for(Compat c : ALL) {
            c.setup();
        }
    }

    public static void clearAll() {
        for(Compat c : ALL) {
            c.clear();
        }
    }

    public static int collectNormalTexture(int glId) {
        for(Compat c : ALL) {
            int normalGlId = c.normalTexture(glId);
            if(normalGlId >= 0) {
                return normalGlId;
            }
        }
        return -1;
    }

    public static int collectSpecularTexture(int glId) {
        for(Compat c : ALL) {
            int specularGlId = c.specularTexture(glId);
            if(specularGlId >= 0) {
                return specularGlId;
            }
        }
        return -1;
    }

    private static Compat addCompat(String modId, Supplier<Supplier<Compat>> target) {
        Compat c;
        if(FabricLoader.getInstance().isModLoaded(modId)) {
            c = target.get().get();
        }else{
            c = new Impl();
        }
        ALL.add(c);
        return c;
    }

    public abstract VertexConsumer collectBuffer(VertexConsumer buffer);

    public abstract void setup();

    public abstract void clear();

    public abstract int normalTexture(int glId);

    public abstract int specularTexture(int glId);

    public static class Impl extends Compat {

        @Override
        public VertexConsumer collectBuffer(VertexConsumer buffer) {
            return buffer;
        }

        @Override
        public void setup() {

        }

        @Override
        public void clear() {

        }

        @Override
        public int normalTexture(int glId) {
            return -1;
        }

        @Override
        public int specularTexture(int glId) {
            return -1;
        }
    }

}
