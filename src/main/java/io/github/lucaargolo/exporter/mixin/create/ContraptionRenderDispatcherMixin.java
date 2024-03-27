package io.github.lucaargolo.exporter.mixin.create;

import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.contraptions.render.ContraptionRenderInfo;
import io.github.lucaargolo.exporter.ExporterClient;
import io.github.lucaargolo.exporter.mixin.flywheel.VirtualRenderWorldAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ContraptionRenderDispatcher.class)
public class ContraptionRenderDispatcherMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/render/ContraptionRenderDispatcher;renderBlockEntities(Lnet/minecraft/world/level/Level;Lcom/jozufozu/flywheel/core/virtual/VirtualRenderWorld;Lcom/simibubi/create/content/contraptions/Contraption;Lcom/simibubi/create/content/contraptions/render/ContraptionMatrices;Lnet/minecraft/client/renderer/MultiBufferSource;)V"), method = "renderFromEntity", locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private static void captureCorrectMatrices(AbstractContraptionEntity entity, Contraption contraption, MultiBufferSource buffers, CallbackInfo ci, Level world, ContraptionRenderInfo renderInfo, ContraptionMatrices matrices, VirtualRenderWorld renderWorld) {
        if(entity.getId() == ExporterClient.MARKED_ENTITY) {
            var minecraft = Minecraft.getInstance();
            var blockEntityDispatcher = minecraft.getBlockEntityRenderDispatcher();
            var blockDispatcher = minecraft.getBlockRenderer();
            var markedBoxBackup = ExporterClient.MARKED_BOX;

            var minPos = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            var maxPos = new BlockPos.MutableBlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
            ((VirtualRenderWorldAccessor) renderWorld).getBlockStates().keySet().forEach(pos -> {
                if(pos.getX() < minPos.getX()) minPos.setX(pos.getX());
                if(pos.getY() < minPos.getY()) minPos.setY(pos.getY());
                if(pos.getZ() < minPos.getZ()) minPos.setZ(pos.getZ());
                if(pos.getX() > maxPos.getX()) maxPos.setX(pos.getX());
                if(pos.getY() > maxPos.getY()) maxPos.setY(pos.getY());
                if(pos.getZ() > maxPos.getZ()) maxPos.setZ(pos.getZ());
            });
            ExporterClient.MARKED_BOX = BoundingBox.fromCorners(minPos, maxPos);
            ExporterClient.EXTRA_POSE = matrices.getModel().last().pose();
            ExporterClient.EXTRA_NORMAL = matrices.getModel().last().normal();
            var center = markedBoxBackup != null ? ExporterClient.getCenter(markedBoxBackup) : new Vector3f();

            for (BlockPos pos : ((VirtualRenderWorldAccessor) renderWorld).getBlockStates().keySet()) {
                Vec3 posInWorld = entity.toGlobalVector(Vec3.atCenterOf(pos), ExporterClient.TICK_DELTA)
                        .subtract(Vec3.atLowerCornerOf(pos));
                ExporterClient.EXTRA_POSITION = posInWorld.toVector3f().sub(center);
                ExporterClient.renderBlock(blockEntityDispatcher, blockDispatcher, renderWorld, pos, new PoseStack(), buffers, ExporterClient.TICK_DELTA);
                ExporterClient.EXTRA_POSITION = new Vector3f();
            }
            ExporterClient.EXTRA_POSE = null;
            ExporterClient.EXTRA_NORMAL = null;
            ExporterClient.MARKED_BOX = markedBoxBackup;
            ci.cancel();
        }
    }


}
