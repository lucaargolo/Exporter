package io.github.lucaargolo.exporter.compat;

import io.github.lucaargolo.exporter.compat.iris.FullIrisCompat;
import io.github.lucaargolo.exporter.compat.iris.IrisCompat;
import io.github.lucaargolo.exporter.compat.sodium.FullSodiumCompat;
import io.github.lucaargolo.exporter.compat.sodium.SodiumCompat;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("StaticInitializerReferencesSubClass")
public abstract class Compat {

    private static final List<Compat> ALL = new ArrayList<>();

    public static final Compat MINECRAFT = addCompat("minecraft", () -> MinecraftCompat::new, () -> Impl::new);
    public static final Compat FLYWHEEL = addCompat("flywheel", () -> FlywheelCompat::new, () -> Impl::new);
    public static final SodiumCompat SODIUM = addCompat("sodium", () -> FullSodiumCompat::new, () -> SodiumCompat.Impl::new);
    public static final IrisCompat IRIS = addCompat("iris", () -> FullIrisCompat::new, () -> IrisCompat.Impl::new);

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

    private static <C extends Compat> C addCompat(String modId, Supplier<Supplier<C>> target, Supplier<Supplier<C>> empty) {
        C c;
        if(FabricLoader.getInstance().isModLoaded(modId)) {
            c = target.get().get();
        }else{
            c = empty.get().get();
        }
        ALL.add(c);
        return c;
    }

    public abstract void setup();

    public abstract void clear();

    public abstract boolean isPresent();

    public static class Impl extends Compat {

        @Override
        public void setup() {

        }

        @Override
        public void clear() {

        }

        @Override
        public boolean isPresent() {
            return false;
        }
    }

}
