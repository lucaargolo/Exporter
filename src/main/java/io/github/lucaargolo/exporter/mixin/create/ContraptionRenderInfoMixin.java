package io.github.lucaargolo.exporter.mixin.create;

import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.simibubi.create.content.contraptions.render.ContraptionRenderInfo;
import io.github.lucaargolo.exporter.ExporterClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContraptionRenderInfo.class, remap = false)
public class ContraptionRenderInfoMixin {

    @Shadow private boolean visible;

    @Inject(at = @At("TAIL"), method = "beginFrame")
    public void makeContraptionsVisible(BeginFrameEvent event, CallbackInfo ci) {
        if(ExporterClient.SETUP) {
            this.visible = true;
        }
    }

}
