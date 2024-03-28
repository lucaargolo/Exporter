package io.github.lucaargolo.exporter.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.compat.create.FlywheelCompat;
import io.github.lucaargolo.exporter.compat.minecraft.AmbientOcclusionCompat;
import io.github.lucaargolo.exporter.compat.sodium.SodiumCompat;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class Compat {

    private static final List<Compat> ALL = new ArrayList<>();

    public static final Compat AMBIENT_OCCLUSION = addCompat("minecraft", () -> AmbientOcclusionCompat::new);
    public static final Compat FLYWHEEL = addCompat("flywheel", () -> FlywheelCompat::new);
    public static final Compat SODIUM = addCompat("sodium", () -> SodiumCompat::new);

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

    }

}
