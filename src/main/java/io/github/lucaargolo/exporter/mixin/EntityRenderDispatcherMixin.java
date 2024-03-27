package io.github.lucaargolo.exporter.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.lucaargolo.exporter.ExporterClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", shift = At.Shift.BEFORE), method = "render")
    public <E extends Entity> void test(E entity, double x, double y, double z, float rotationYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if(entity.getId() == ExporterClient.MARKED_ENTITY) {
            ExporterClient.INVERTED_POSE = poseStack.last().pose().invert(new Matrix4f());
            ExporterClient.INVERTED_NORMAL = poseStack.last().normal().invert(new Matrix3f());
            ExporterClient.MARKED_BUFFER = buffer;
            if(!ExporterClient.COMPLETE && ExporterClient.MARKED_BOX != null) {
                ExporterClient.VERTEX_POSITION = new Vector3f((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f).sub(ExporterClient.getCenter(ExporterClient.MARKED_BOX));
            }else{
                ExporterClient.VERTEX_POSITION = new Vector3f(0, 0, 0);
            }
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", shift = At.Shift.AFTER), method = "render")
    public <E extends Entity> void end(E entity, double x, double y, double z, float rotationYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if(entity.getId() == ExporterClient.MARKED_ENTITY) {
            ExporterClient.MARKED_ENTITY = -1;
            ExporterClient.INVERTED_POSE = null;
            ExporterClient.INVERTED_NORMAL = null;
            ExporterClient.MARKED_BUFFER = null;
            ExporterClient.MARKED_CONSUMERS.clear();

            if(ExporterClient.MARKED_BOX == null) {
                ExporterClient.writeCapturedNode(new Vector3f(0, 0, 0));
                ExporterClient.writeCapturedModel();
            }else if(ExporterClient.COMPLETE) {
                ExporterClient.writeCapturedNode(new Vector3f((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f).sub(ExporterClient.getCenter(ExporterClient.MARKED_BOX)));
            }else {
                ExporterClient.writeCapturedNode(new Vector3f(0, 0, 0));
            }
        }
    }

}
