package io.github.lucaargolo.exporter.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.lucaargolo.exporter.compat.iris.IrisCompat;
import io.github.lucaargolo.exporter.utils.ModelBuilder;
import io.github.lucaargolo.exporter.utils.helper.BufferHelper;
import io.github.lucaargolo.exporter.utils.helper.RenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", shift = At.Shift.BEFORE), method = "render")
    public <E extends Entity> void test(E entity, double x, double y, double z, float rotationYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if(RenderHelper.isSetup() && RenderHelper.isMarkedEntity(entity)) {

            //Iris creates a lambda buffer source inside a mixin, so we have to do this.
            MultiBufferSource realBuffer;
            if(IrisCompat.IRIS.isPresent()) {
                Class<?> thisClass = this.getClass();
                Class<?> bufferClass = buffer.getClass();
                if (bufferClass.getName().contains(thisClass.getName() + "$$Lambda")) {
                    try {
                        Field backingField = bufferClass.getDeclaredField("arg$1");
                        realBuffer = (MultiBufferSource) backingField.get(buffer);
                    } catch (NoSuchFieldException | IllegalAccessException ignored) {
                        realBuffer = buffer;
                    }
                } else {
                    realBuffer = buffer;
                }
            }else{
                realBuffer = buffer;
            }
            BufferHelper.markBuffer(realBuffer);
            ModelBuilder.setupInvertedPose(poseStack);

            if(!RenderHelper.isComplete() && RenderHelper.hasBox()) {
                ModelBuilder.setPosition(new Vector3f((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f).sub(RenderHelper.getMarkedCenter()));
            }else{
                ModelBuilder.setPosition(new Vector3f(0, 0, 0));
            }
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", shift = At.Shift.AFTER), method = "render")
    public <E extends Entity> void end(E entity, double x, double y, double z, float rotationYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if(RenderHelper.isSetup() && RenderHelper.isMarkedEntity(entity)) {
            RenderHelper.markEntity(-1);
            ModelBuilder.clearInvertedPose();
            BufferHelper.clearBuffer();

            if(!RenderHelper.hasBox()) {
                ModelBuilder.writeCapturedNode(new Vector3f(0, 0, 0));
                ModelBuilder.writeCapturedModel();
            }else if(RenderHelper.isComplete()) {
                ModelBuilder.writeCapturedNode(new Vector3f((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f).sub(RenderHelper.getMarkedCenter()));
            }else {
                ModelBuilder.writeCapturedNode(new Vector3f(0, 0, 0));
            }
        }
    }

}
