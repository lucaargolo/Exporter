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
import io.github.lucaargolo.exporter.ExporterClient;
import io.github.lucaargolo.exporter.mixin.flywheel.VirtualRenderWorldAccessor;
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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashMap;

@Mixin(ContraptionRenderDispatcher.class)
public class ContraptionRenderDispatcherMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/render/ContraptionRenderDispatcher;renderBlockEntities(Lnet/minecraft/world/level/Level;Lcom/jozufozu/flywheel/core/virtual/VirtualRenderWorld;Lcom/simibubi/create/content/contraptions/Contraption;Lcom/simibubi/create/content/contraptions/render/ContraptionMatrices;Lnet/minecraft/client/renderer/MultiBufferSource;)V"), method = "renderFromEntity", locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private static void captureCorrectMatrices(AbstractContraptionEntity entity, Contraption contraption, MultiBufferSource buffers, CallbackInfo ci, Level world, ContraptionRenderInfo renderInfo, ContraptionMatrices matrices, VirtualRenderWorld renderWorld) {
        if(ExporterClient.SETUP && entity.getId() == ExporterClient.MARKED_ENTITY) {
            var poseBackup = ExporterClient.INVERTED_POSE;
            var normalBackup = ExporterClient.INVERTED_NORMAL;
            var bufferBackup = ExporterClient.MARKED_BUFFER;
            var consumerBackup = new HashMap<>(ExporterClient.MARKED_CONSUMERS);
            var vertexBackup = ExporterClient.VERTEX_POSITION;

            var minecraft = Minecraft.getInstance();
            var blockEntityDispatcher = minecraft.getBlockEntityRenderDispatcher();
            var blockDispatcher = minecraft.getBlockRenderer();
            var markedBoxBackup = ExporterClient.MARKED_BOX;

            var size = 0;
            for (BlockPos blockPos : ((VirtualRenderWorldAccessor) renderWorld).getBlockStates().keySet()) {
                if (Math.abs(blockPos.getX()) > size) size = Math.abs(blockPos.getX());
                if (Math.abs(blockPos.getY()) > size) size = Math.abs(blockPos.getY());
                if (Math.abs(blockPos.getZ()) > size) size = Math.abs(blockPos.getZ());
            }
            ExporterClient.MARKED_BOX = new BoundingBox(-size, -size, -size, size, size, size);
            ExporterClient.EXTRA_POSE = matrices.getModel().last().pose();
            ExporterClient.EXTRA_NORMAL = matrices.getModel().last().normal();
            var center = markedBoxBackup != null ? ExporterClient.getCenter(markedBoxBackup) : entity.toGlobalVector(contraption.bounds.getCenter().with(Direction.Axis.Y, 0.5), ExporterClient.TICK_DELTA).toVector3f();

            //Render blocks
            for (BlockPos pos : ((VirtualRenderWorldAccessor) renderWorld).getBlockStates().keySet()) {
                Vec3 posInWorld = entity.toGlobalVector(Vec3.atCenterOf(pos), ExporterClient.TICK_DELTA).subtract(Vec3.atLowerCornerOf(pos));
                ExporterClient.EXTRA_POSITION = posInWorld.toVector3f().sub(center);
                ExporterClient.renderBlock(null, blockDispatcher, renderWorld, pos, new PoseStack(), buffers, ExporterClient.TICK_DELTA);
                ExporterClient.EXTRA_POSITION = new Vector3f();
            }

            //Render block entities
            for (BlockEntity blockEntity : contraption.getSpecialRenderedBEs()) {
                BlockPos pos = blockEntity.getBlockPos();
                Vec3 posInWorld = entity.toGlobalVector(Vec3.atCenterOf(pos), ExporterClient.TICK_DELTA).subtract(Vec3.atLowerCornerOf(pos));
                ExporterClient.EXTRA_POSITION = posInWorld.toVector3f().sub(center);
                blockEntity.setLevel(renderWorld);
                ExporterClient.renderBlockEntity(blockEntityDispatcher, blockEntity, pos, new PoseStack(), buffers, ExporterClient.TICK_DELTA, LightTexture.FULL_BRIGHT);
                blockEntity.setLevel(world);
                ExporterClient.EXTRA_POSITION = new Vector3f();
            }

            //Render actors
            for (Pair<StructureTemplate.StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
                BlockPos pos = actor.getLeft().pos();
                Vec3 posInWorld = entity.toGlobalVector(Vec3.atCenterOf(pos), ExporterClient.TICK_DELTA).subtract(Vec3.atLowerCornerOf(pos));
                ExporterClient.EXTRA_POSITION = posInWorld.toVector3f().sub(center);
                renderActor(world, contraption, renderWorld, actor, buffers);
                ExporterClient.EXTRA_POSITION = new Vector3f();
            }

            ExporterClient.EXTRA_POSE = null;
            ExporterClient.EXTRA_NORMAL = null;
            ExporterClient.MARKED_ENTITY = entity.getId();
            ExporterClient.MARKED_BOX = markedBoxBackup;

            ExporterClient.INVERTED_POSE = poseBackup;
            ExporterClient.INVERTED_NORMAL = normalBackup;
            ExporterClient.MARKED_BUFFER = bufferBackup;
            ExporterClient.MARKED_CONSUMERS.clear();
            ExporterClient.MARKED_CONSUMERS.putAll(consumerBackup);
            ExporterClient.VERTEX_POSITION = vertexBackup;
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
                if (ExporterClient.COMPLETE) {
                    ExporterClient.markEntity(Integer.MAX_VALUE);
                }
                ExporterClient.INVERTED_POSE = emptyMatrices.getModel().last().pose().invert(new Matrix4f());
                ExporterClient.INVERTED_NORMAL = emptyMatrices.getModel().last().normal().invert(new Matrix3f());
                ExporterClient.MARKED_BUFFER = bufferSource;
                if (!ExporterClient.COMPLETE && ExporterClient.MARKED_BOX != null) {
                    ExporterClient.VERTEX_POSITION = ExporterClient.getCenter(blockInfo.pos()).sub(ExporterClient.getCenter(ExporterClient.MARKED_BOX));
                } else {
                    ExporterClient.VERTEX_POSITION = new Vector3f(0, 0, 0);
                }
                movementBehaviour.renderInContraption(context, renderWorld, emptyMatrices, bufferSource);
                ExporterClient.INVERTED_POSE = null;
                ExporterClient.INVERTED_NORMAL = null;
                ExporterClient.MARKED_BUFFER = null;
                ExporterClient.MARKED_CONSUMERS.clear();
                if (ExporterClient.COMPLETE) {
                    ExporterClient.writeCapturedNode(ExporterClient.getCenter(blockInfo.pos()).sub(ExporterClient.getCenter(ExporterClient.MARKED_BOX)));
                    ExporterClient.MARKED_ENTITY = -1;
                }
            }
        }
    }

}
