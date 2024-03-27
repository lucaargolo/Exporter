package io.github.lucaargolo.exporter.compat;

import io.github.lucaargolo.exporter.compat.create.FlywheelCompat;
import net.fabricmc.loader.api.FabricLoader;

import java.util.function.Supplier;

public interface Compat {

    Compat FLYWHEEL = getCompat("flywheel", () -> FlywheelCompat::new);

    void setupRenderState();

    void clearRenderState();

    static Compat getCompat(String modId, Supplier<Supplier<Compat>> target) {
        if(FabricLoader.getInstance().isModLoaded(modId)) {
            return target.get().get();
        }else{
            return new EmptyCompat();
        }
    }

}
