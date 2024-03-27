package io.github.lucaargolo.exporter.compat;

import io.github.lucaargolo.exporter.compat.create.FlywheelCompat;
import io.github.lucaargolo.exporter.compat.minecraft.AmbientOcclusionCompat;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class Compat {

    private static final List<Compat> ALL = new ArrayList<>();

    private static final Compat AMBIENT_OCCLUSION = addCompat("minecraft", () -> AmbientOcclusionCompat::new);
    private static final Compat FLYWHEEL = addCompat("flywheel", () -> FlywheelCompat::new);

    public abstract void setup();

    public abstract void clear();

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
            c = new EmptyCompat();
        }
        ALL.add(c);
        return c;
    }

}
