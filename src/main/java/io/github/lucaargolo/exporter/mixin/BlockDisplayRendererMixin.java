package io.github.lucaargolo.exporter.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.lucaargolo.exporter.ExporterClient;
import io.github.lucaargolo.exporter.entities.ReferenceBlockDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisplayRenderer.BlockDisplayRenderer.class)
public class BlockDisplayRendererMixin {

    @Shadow @Final private BlockRenderDispatcher blockRenderer;

    @Inject(at = @At("HEAD"), method = "renderInner(Lnet/minecraft/world/entity/Display$BlockDisplay;Lnet/minecraft/world/entity/Display$BlockDisplay$BlockRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V", cancellable = true)
    public void test(Display.BlockDisplay blockDisplay, Display.BlockDisplay.BlockRenderState blockRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, float f, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = blockDisplay.level();
        BlockState state = blockRenderState.blockState();
        RenderShape renderShape = state.getRenderShape();
        if (renderShape == RenderShape.MODEL && blockDisplay instanceof ReferenceBlockDisplay reference && state.isCollisionShapeFullBlock(level, reference.getBlockPos())) {
            BakedModel bakedModel = this.blockRenderer.getBlockModel(state);
            int color = minecraft.getBlockColors().getColor(state, level, reference.getBlockPos(), 0);
            float r = (float) (color >> 16 & 0xFF) / 255.0F;
            float g = (float) (color >> 8 & 0xFF) / 255.0F;
            float b = (float) (color & 0xFF) / 255.0F;
            VertexConsumer consumer = multiBufferSource.getBuffer(ItemBlockRenderTypes.getRenderType(state, false));

            RandomSource randomSource = RandomSource.create();

            for(Direction direction : Direction.values()) {
                randomSource.setSeed(42L);
                BlockPos adjacentPos = reference.getBlockPos().offset(direction.getNormal());
                BlockState adjacentState = level.getBlockState(adjacentPos);
                if(ExporterClient.COMPLETE || (!adjacentState.isCollisionShapeFullBlock(level, adjacentPos) || (ExporterClient.MARKED_BOX != null && !ExporterClient.MARKED_BOX.isInside(adjacentPos))))
                    ModelBlockRenderer.renderQuadList(poseStack.last(), consumer, r, g, b, bakedModel.getQuads(state, direction, randomSource), i, OverlayTexture.NO_OVERLAY);
            }

            randomSource.setSeed(42L);
            ModelBlockRenderer.renderQuadList(poseStack.last(), consumer, r, g, b, bakedModel.getQuads(state, null, randomSource), i, OverlayTexture.NO_OVERLAY);
            ci.cancel();
        }
    }


}
