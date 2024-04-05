package io.github.lucaargolo.exporter.utils.helper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.compat.Compat;
import io.github.lucaargolo.exporter.utils.ModelBuilder;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Objects;

public class RenderHelper {

    private static boolean SETUP = false;

    private static boolean COMPLETE = false;
    private static boolean TRIM = false;

    private static BoundingBox MARKED_BOX;
    private static BoundingBox BACKUP_BOX;
    private static int MARKED_ENTITY = -1;

    public static void onRender() {
        if(SETUP) {
            if(MARKED_BOX == null && MARKED_ENTITY == -1) {
                Compat.clearAll();
                SETUP = false;
            }
        }else if(MARKED_BOX != null || MARKED_ENTITY != -1) {
            Compat.setupAll();
            SETUP = true;
        }
    }

    public static void config(boolean complete, boolean trim) {
        COMPLETE = complete;
        TRIM = trim;
    }

    public static void markEntity(int entityId) {
        MARKED_ENTITY = entityId;
    }

    public static void markBox(BoundingBox box) {
        MARKED_BOX = box;
    }

    public static boolean hasBox() {
        return MARKED_BOX != null;
    }

    public static boolean isOutsideBox(BlockPos pos) {
        return !MARKED_BOX.isInside(pos);
    }

    public static boolean hasBackupBox() {
        return BACKUP_BOX != null;
    }

    public static void backupBox() {
        BACKUP_BOX = MARKED_BOX;
    }

    public static void restoreBox() {
        MARKED_BOX = BACKUP_BOX;
        BACKUP_BOX = null;
    }

    public static boolean isSetup() {
        return SETUP;
    }

    public static boolean isComplete() {
        return COMPLETE;
    }

    public static boolean isTrim() {
        return TRIM;
    }

    public static boolean isMarkedEntity(Entity entity) {
        return entity.getId() == MARKED_ENTITY;
    }

    public static void renderMarked(WorldRenderContext context) {
        renderMarked(context.world(), context.matrixStack(), context.consumers(), context.tickDelta());
    }

    public static void renderMarked(ClientLevel level, PoseStack poseStack, MultiBufferSource bufferSource, float tickDelta) {
        if(SETUP) {
            var minecraft = Minecraft.getInstance();
            var entityDispatcher = minecraft.getEntityRenderDispatcher();
            if (MARKED_BOX != null) {
                var blockEntityDispatcher = minecraft.getBlockEntityRenderDispatcher();
                var blockDispatcher = minecraft.getBlockRenderer();
                markEntity(Integer.MAX_VALUE);
                ModelBuilder.clearMesh();
                for (BlockPos pos : BlockPos.betweenClosed(MARKED_BOX.minX(), MARKED_BOX.minY(), MARKED_BOX.minZ(), MARKED_BOX.maxX(), MARKED_BOX.maxY(), MARKED_BOX.maxZ())) {
                    renderBlock(blockEntityDispatcher, blockDispatcher, level, pos, poseStack, bufferSource, tickDelta);
                }
                if (!COMPLETE)
                    ModelBuilder.writeCapturedNode(new Vector3f(0, 0, 0));
                AABB aabb = AABB.of(MARKED_BOX);
                for (Entity entity : level.getEntities(null, aabb)) {
                    markEntity(entity.getId());
                    ModelBuilder.clearMesh();
                    float rot = Mth.lerp(tickDelta, entity.yRotO, entity.getYRot());
                    entityDispatcher.render(entity, entity.getX(), entity.getY(), entity.getZ(), rot, tickDelta, poseStack, Objects.requireNonNull(bufferSource), LightTexture.FULL_BRIGHT);
                }
                MARKED_ENTITY = -1;
                MARKED_BOX = null;
                ModelBuilder.writeCapturedModel();
            }else if(MARKED_ENTITY >= 0) {
                var entity = level.getEntities().get(MARKED_ENTITY);
                if(entity != null) {
                    float rot = Mth.lerp(tickDelta, entity.yRotO, entity.getYRot());
                    entityDispatcher.render(entity, entity.getX(), entity.getY(), entity.getZ(), rot, tickDelta, poseStack, Objects.requireNonNull(bufferSource), LightTexture.FULL_BRIGHT);
                }
            }
        }
    }

    public static void renderBlock(@Nullable BlockEntityRenderDispatcher blockEntityDispatcher, BlockRenderDispatcher blockDispatcher, BlockAndTintGetter level, BlockPos pos, PoseStack poseStack, MultiBufferSource bufferSource, float tickDelta) {
        var state = level.getBlockState(pos);
        if (!state.isAir()) {
            int packedLight = LevelRenderer.getLightColor(level, pos);
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null && blockEntityDispatcher != null) {
                renderBlockEntity(blockEntityDispatcher, blockEntity, pos, poseStack, bufferSource, tickDelta, packedLight);
            }
            if (state.getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
                tesselateBlock(blockDispatcher, level, pos, state, poseStack, bufferSource);
            }
            FluidState fluidState = state.getFluidState();
            if (!fluidState.isEmpty()) {
                tesselateFluid(blockDispatcher, level, pos, state, fluidState, bufferSource);
            }
        }
    }

    public static void renderBlockEntity(BlockEntityRenderDispatcher blockEntityDispatcher, BlockEntity blockEntity, BlockPos pos, PoseStack poseStack, MultiBufferSource bufferSource, float tickDelta, int packedLight) {
        var renderer = blockEntityDispatcher.getRenderer(blockEntity);
        if (renderer != null) {
            if (COMPLETE) {
                markEntity(Integer.MAX_VALUE);
                ModelBuilder.clearMesh();
            }
            BufferHelper.markBuffer(bufferSource);
            ModelBuilder.setupInvertedPose(poseStack);
            if (!COMPLETE && MARKED_BOX != null) {
                ModelBuilder.setPosition(getCenter(pos).sub(getMarkedCenter()));
            } else {
                ModelBuilder.setPosition(new Vector3f(0, 0, 0));
            }
            renderer.render(blockEntity, tickDelta, poseStack, Objects.requireNonNull(bufferSource), packedLight, OverlayTexture.NO_OVERLAY);
            ModelBuilder.clearInvertedPose();
            BufferHelper.clearBuffer();
            if (COMPLETE) {
                ModelBuilder.writeCapturedNode(getCenter(pos).sub(getMarkedCenter()));
                MARKED_ENTITY = -1;
            }
        }
    }

    private static void tesselateBlock(BlockRenderDispatcher blockDispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (COMPLETE) {
            markEntity(Integer.MAX_VALUE);
            ModelBuilder.clearMesh();
        }
        BufferHelper.markBuffer(bufferSource);
        ModelBuilder.setupInvertedPose(poseStack);
        ModelBuilder.setPosition(new Vector3f(getCenter(pos).sub(getMarkedCenter())));
        RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        blockDispatcher.renderBatched(state, pos, level, poseStack, consumer, !COMPLETE, RandomSource.create());
        ModelBuilder.clearInvertedPose();
        BufferHelper.clearBuffer();
        if (COMPLETE) {
            ModelBuilder.writeCapturedNode(new Vector3f(0, 0, 0));
            MARKED_ENTITY = -1;
        }
    }

    private static void tesselateFluid(BlockRenderDispatcher blockDispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, FluidState fluidState, MultiBufferSource bufferSource) {
        if (COMPLETE) {
            markEntity(Integer.MAX_VALUE);
            ModelBuilder.clearMesh();
        }
        BufferHelper.markBuffer(bufferSource);
        var offset = new Vector3f(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        ModelBuilder.setPosition(getCenter(pos).sub(offset).sub(getMarkedCenter()));
        RenderType renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        blockDispatcher.renderLiquid(pos, level, consumer, state, fluidState);
        ModelBuilder.clearInvertedPose();
        BufferHelper.clearBuffer();
        if (COMPLETE) {
            ModelBuilder.writeCapturedNode(new Vector3f(0, 0, 0));
            MARKED_ENTITY = -1;
        }
    }

    public static Vector3f getMarkedCenter() {
        return getCenter(MARKED_BOX);
    }

    public static Vector3f getBackupCenter() {
        return getCenter(BACKUP_BOX);
    }

    private static Vector3f getCenter(BoundingBox box) {
        return new Vector3f(box.minX() + (box.maxX() - box.minX() + 1f) / 2f, box.minY() + (box.maxY() - box.minY() + 1f) / 2f, box.minZ() + (box.maxZ() - box.minZ() + 1f) / 2f);
    }

    public static Vector3f getCenter(BlockPos pos) {
        return new Vector3f(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
    }

}
