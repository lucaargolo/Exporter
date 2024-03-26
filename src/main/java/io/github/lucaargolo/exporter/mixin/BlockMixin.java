package io.github.lucaargolo.exporter.mixin;

import io.github.lucaargolo.exporter.ExporterClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(at = @At("HEAD"), method = "shouldRenderFace", cancellable = true)
    private static void completeRender(BlockState state, BlockGetter level, BlockPos offset, Direction face, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if(ExporterClient.MARKED_BOX != null && !ExporterClient.MARKED_BOX.isInside(pos)) {
            cir.setReturnValue(true);
        }
    }

}
