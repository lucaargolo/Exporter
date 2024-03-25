package io.github.lucaargolo.exporter.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ReferenceBlockDisplay extends Display.BlockDisplay {

    private BlockPos blockPos = BlockPos.ZERO;

    public ReferenceBlockDisplay(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

}
