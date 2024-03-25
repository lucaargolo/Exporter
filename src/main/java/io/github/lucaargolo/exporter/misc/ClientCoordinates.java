package io.github.lucaargolo.exporter.misc;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public interface ClientCoordinates {
	Vec3 getPosition(FabricClientCommandSource source);

	Vec2 getRotation(FabricClientCommandSource source);

	default BlockPos getBlockPos(FabricClientCommandSource source) {
		return BlockPos.containing(this.getPosition(source));
	}

	boolean isXRelative();

	boolean isYRelative();

	boolean isZRelative();
}
