package io.github.lucaargolo.exporter.compat.create;

import com.jozufozu.flywheel.config.BackendType;
import io.github.lucaargolo.exporter.compat.Compat;

public class FlywheelCompat implements Compat {

    private BackendType lastBackend;

    @Override
    public void setupRenderState() {
        //FlwConfig config = FlwConfig.get();
        //this.lastBackend = config.getBackendType();
        //config.backend.set(BackendType.OFF);
        //Backend.reloadWorldRenderers();
    }

    @Override
    public void clearRenderState() {
        //FlwConfig config = FlwConfig.get();
        //config.backend.set(lastBackend);
        //Backend.reloadWorldRenderers();
    }

}
