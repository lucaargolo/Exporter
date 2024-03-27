package io.github.lucaargolo.exporter.compat.create;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.config.BackendType;
import com.jozufozu.flywheel.config.FlwConfig;
import io.github.lucaargolo.exporter.compat.Compat;

public class FlywheelCompat extends Compat {

    private BackendType lastBackend;

    @Override
    public void setup() {
        FlwConfig config = FlwConfig.get();
        this.lastBackend = config.getBackendType();
        config.backend.set(BackendType.OFF);
        Backend.reloadWorldRenderers();
    }

    @Override
    public void clear() {
        FlwConfig config = FlwConfig.get();
        config.backend.set(lastBackend);
        Backend.reloadWorldRenderers();
    }

}
