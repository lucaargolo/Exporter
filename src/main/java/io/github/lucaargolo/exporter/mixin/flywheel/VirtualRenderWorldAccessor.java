package io.github.lucaargolo.exporter.mixin.flywheel;

import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(VirtualRenderWorld.class)
public interface VirtualRenderWorldAccessor {

    @Accessor(remap = false)
    Map<BlockPos, BlockState> getBlockStates();

}
