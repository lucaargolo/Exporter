package io.github.lucaargolo.exporter;

import com.mojang.logging.LogUtils;
import io.github.lucaargolo.exporter.utils.helper.RenderHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.slf4j.Logger;

public class ExporterClient implements ClientModInitializer {

    public static final Logger LOGGER = LogUtils.getLogger();

    /**TODO:
     *  - Lava exporting is glitchy for some reason?.
     *  - Fix fluid exporting when using complete. (I think its a Godot Issue)
     *  - Add user interface with options (Ambient Occlusion on/off, Entities on/off, File binary/embed/normal)
     *  - Find a way to export blocks outside player view? (Not priority)
     *  - Why didn't it export entities in all of fabric?
     *  - Create compatibility?
     */

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(ExporterCommand::register);
        WorldRenderEvents.BEFORE_ENTITIES.register(RenderHelper::renderMarked);
        WorldRenderEvents.END.register(context -> RenderHelper.onRender());
    }

}
