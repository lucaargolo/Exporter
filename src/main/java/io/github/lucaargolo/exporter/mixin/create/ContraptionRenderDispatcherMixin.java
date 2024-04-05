package io.github.lucaargolo.exporter.mixin.create;

import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.contraptions.render.ContraptionRenderInfo;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import io.github.lucaargolo.exporter.mixin.flywheel.VirtualRenderWorldAccessor;
import io.github.lucaargolo.exporter.utils.ModelBuilder;
import io.github.lucaargolo.exporter.utils.helper.BufferHelper;
import io.github.lucaargolo.exporter.utils.helper.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ContraptionRenderDispatcher.class)
public class ContraptionRenderDispatcherMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/render/ContraptionRenderDispatcher;renderBlockEntities(Lnet/minecraft/world/level/Level;Lcom/jozufozu/flywheel/core/virtual/VirtualRenderWorld;Lcom/simibubi/create/content/contraptions/Contraption;Lcom/simibubi/create/content/contraptions/render/ContraptionMatrices;Lnet/minecraft/client/renderer/MultiBufferSource;)V"), method = "renderFromEntity", locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private static void captureCorrectMatrices(AbstractContraptionEntity entity, Contraption contraption, MultiBufferSource buffers, CallbackInfo ci, Level world, ContraptionRenderInfo renderInfo, ContraptionMatrices matrices, VirtualRenderWorld renderWorld) {
        if(RenderHelper.isSetup() && RenderHelper.isMarkedEntity(entity)) {
            BufferHelper.backupBuffers();
            ModelBuilder.backupPose();
            ModelBuilder.setupExtraPose(matrices.getModel());

            var minecraft = Minecraft.getInstance();
            var blockEntityDispatcher = minecraft.getBlockEntityRenderDispatcher();
            var blockDispatcher = minecraft.getBlockRenderer();

            var size = 0;
            for (BlockPos blockPos : ((VirtualRenderWorldAccessor) renderWorld).getBlockStates().keySet()) {
                if (Math.abs(blockPos.getX()) > size) size = Math.abs(blockPos.getX());
                if (Math.abs(blockPos.getY()) > size) size = Math.abs(blockPos.getY());
                if (Math.abs(blockPos.getZ()) > size) size = Math.abs(blockPos.getZ());
            }

            RenderHelper.backupBox();
            RenderHelper.markBox(new BoundingBox(-size, -size, -size, size, size, size));

            var center = RenderHelper.hasBackupBox() ? RenderHelper.getBackupCenter() : entity.toGlobalVector(contraption.bounds.getCenter().with(Direction.Axis.Y, 0.5), AnimationTickHolder.getPartialTicks()).toVector3f();

            //Render blocks
            for (BlockPos pos : ((VirtualRenderWorldAccessor) renderWorld).getBlockStates().keySet()) {
                Vec3 posInWorld = entity.toGlobalVector(Vec3.atCenterOf(pos), AnimationTickHolder.getPartialTicks()).subtract(Vec3.atLowerCornerOf(pos));
                ModelBuilder.setExtraPosition(posInWorld.toVector3f().sub(center));
                RenderHelper.renderBlock(null, blockDispatcher, renderWorld, pos, new PoseStack(), buffers, AnimationTickHolder.getPartialTicks());
                ModelBuilder.setExtraPosition(new Vector3f());
            }

            //Render block entities
            for (BlockEntity blockEntity : contraption.getSpecialRenderedBEs()) {
                BlockPos pos = blockEntity.getBlockPos();
                Vec3 posInWorld = entity.toGlobalVector(Vec3.atCenterOf(pos), AnimationTickHolder.getPartialTicks()).subtract(Vec3.atLowerCornerOf(pos));
                ModelBuilder.setExtraPosition(posInWorld.toVector3f().sub(center));
                blockEntity.setLevel(renderWorld);
                RenderHelper.renderBlockEntity(blockEntityDispatcher, blockEntity, pos, new PoseStack(), buffers, AnimationTickHolder.getPartialTicks(), LightTexture.FULL_BRIGHT);
                blockEntity.setLevel(world);
                ModelBuilder.setExtraPosition(new Vector3f());
            }

            //Render actors
            for (Pair<StructureTemplate.StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
                BlockPos pos = actor.getLeft().pos();
                Vec3 posInWorld = entity.toGlobalVector(Vec3.atCenterOf(pos), AnimationTickHolder.getPartialTicks()).subtract(Vec3.atLowerCornerOf(pos));
                ModelBuilder.setExtraPosition(posInWorld.toVector3f().sub(center));
                renderActor(world, contraption, renderWorld, actor, buffers);
                ModelBuilder.setExtraPosition(new Vector3f());
            }

            RenderHelper.markEntity(entity.getId());
            RenderHelper.restoreBox();
            ModelBuilder.clearExtraPose();
            ModelBuilder.restorePose();
            BufferHelper.restoreBuffer();
            ci.cancel();
        }
    }


    @Unique
    private static void renderActor(Level world, Contraption contraption, VirtualRenderWorld renderWorld, Pair<StructureTemplate.StructureBlockInfo, MovementContext> actor, MultiBufferSource bufferSource) {
        ContraptionMatrices emptyMatrices = new ContraptionMatrices();
        emptyMatrices.setup(new PoseStack(), contraption.entity);
        MovementContext context = actor.getRight();
        if (context != null) {
            if (context.world == null) context.world = world;
            StructureTemplate.StructureBlockInfo blockInfo = actor.getLeft();
            MovementBehaviour movementBehaviour = AllMovementBehaviours.getBehaviour(blockInfo.state());
            if (movementBehaviour != null && !contraption.isHiddenInPortal(blockInfo.pos())) {
                if (RenderHelper.isComplete()) {
                    RenderHelper.markEntity(Integer.MAX_VALUE);
                    ModelBuilder.clearMesh();
                }
                ModelBuilder.setupInvertedPose(emptyMatrices.getModel());
                BufferHelper.markBuffer(bufferSource);
                if (!RenderHelper.isComplete() && RenderHelper.hasBox()) {
                    ModelBuilder.setPosition(RenderHelper.getCenter(blockInfo.pos()).sub(RenderHelper.getMarkedCenter()));
                } else {
                    ModelBuilder.setPosition(new Vector3f(0, 0, 0));
                }
                movementBehaviour.renderInContraption(context, renderWorld, emptyMatrices, bufferSource);
                ModelBuilder.clearInvertedPose();
                BufferHelper.clearBuffer();
                if (RenderHelper.isComplete()) {
                    ModelBuilder.writeCapturedNode(RenderHelper.getCenter(blockInfo.pos()).sub(RenderHelper.getMarkedCenter()));
                    RenderHelper.markEntity(-1);
                }
            }
        }
    }

}
