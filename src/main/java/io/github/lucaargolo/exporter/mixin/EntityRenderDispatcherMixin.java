package io.github.lucaargolo.exporter.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.lucaargolo.exporter.ExporterClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3i;
import org.joml.Vector4i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(at = @At("HEAD"), method = "render")
    public <E extends Entity> void test(E entity, double x, double y, double z, float rotationYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if(entity.getId() == ExporterClient.MARKED_ENTITY) {
            ExporterClient.MARKED_BUFFER = buffer;
        }
    }

    @Inject(at = @At("TAIL"), method = "render")
    public <E extends Entity> void end(E entity, double x, double y, double z, float rotationYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if(entity.getId() == ExporterClient.MARKED_ENTITY) {
            ExporterClient.MARKED_ENTITY = -1;
            ExporterClient.MARKED_BUFFER = null;
            ExporterClient.MARKED_CONSUMERS.clear();
            ExporterClient.writeCapturedModel();
        }
    }

}
