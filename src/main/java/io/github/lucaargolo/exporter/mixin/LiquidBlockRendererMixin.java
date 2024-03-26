package io.github.lucaargolo.exporter.mixin;

import io.github.lucaargolo.exporter.ExporterClient;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {

    @Inject(at = @At("HEAD"), method = "shouldRenderFace", cancellable = true)
    private static void completeRender(BlockAndTintGetter level, BlockPos pos, FluidState fluidState, BlockState blockState, Direction side, FluidState neighborFluid, CallbackInfoReturnable<Boolean> cir) {
        if(ExporterClient.MARKED_BOX != null && (ExporterClient.COMPLETE || !ExporterClient.MARKED_BOX.isInside(pos.relative(side)))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(at = @At("HEAD"), method = "isFaceOccludedByState", cancellable = true)
    private static void completeRender(BlockGetter level, Direction face, float height, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if(ExporterClient.MARKED_BOX != null && !ExporterClient.MARKED_BOX.isInside(pos)) {
            cir.setReturnValue(false);
        }
    }

}
