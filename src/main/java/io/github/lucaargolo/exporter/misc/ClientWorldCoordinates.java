package io.github.lucaargolo.exporter.misc;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ClientWorldCoordinates implements ClientCoordinates {
	private final WorldCoordinate x;
	private final WorldCoordinate y;
	private final WorldCoordinate z;

	public ClientWorldCoordinates(WorldCoordinate x, WorldCoordinate y, WorldCoordinate z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public Vec3 getPosition(FabricClientCommandSource source) {
		Vec3 vec3 = source.getPosition();
		return new Vec3(this.x.get(vec3.x), this.y.get(vec3.y), this.z.get(vec3.z));
	}

	@Override
	public Vec2 getRotation(FabricClientCommandSource source) {
		Vec2 vec2 = source.getRotation();
		return new Vec2((float)this.x.get(vec2.x), (float)this.y.get(vec2.y));
	}

	@Override
	public boolean isXRelative() {
		return this.x.isRelative();
	}

	@Override
	public boolean isYRelative() {
		return this.y.isRelative();
	}

	@Override
	public boolean isZRelative() {
		return this.z.isRelative();
	}

	public boolean equals(Object object) {
		if (this == object) {
			return true;
		} else if (!(object instanceof ClientWorldCoordinates worldCoordinates)) {
			return false;
		} else {
			if (!this.x.equals(worldCoordinates.x)) {
				return false;
			} else {
				return this.y.equals(worldCoordinates.y) && this.z.equals(worldCoordinates.z);
			}
		}
	}

	public static ClientWorldCoordinates parseInt(StringReader reader) throws CommandSyntaxException {
		int i = reader.getCursor();
		WorldCoordinate worldCoordinate = WorldCoordinate.parseInt(reader);
		if (reader.canRead() && reader.peek() == ' ') {
			reader.skip();
			WorldCoordinate worldCoordinate2 = WorldCoordinate.parseInt(reader);
			if (reader.canRead() && reader.peek() == ' ') {
				reader.skip();
				WorldCoordinate worldCoordinate3 = WorldCoordinate.parseInt(reader);
				return new ClientWorldCoordinates(worldCoordinate, worldCoordinate2, worldCoordinate3);
			} else {
				reader.setCursor(i);
				throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
			}
		} else {
			reader.setCursor(i);
			throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
		}
	}

	public static ClientWorldCoordinates parseDouble(StringReader reader, boolean centerCorrect) throws CommandSyntaxException {
		int i = reader.getCursor();
		WorldCoordinate worldCoordinate = WorldCoordinate.parseDouble(reader, centerCorrect);
		if (reader.canRead() && reader.peek() == ' ') {
			reader.skip();
			WorldCoordinate worldCoordinate2 = WorldCoordinate.parseDouble(reader, false);
			if (reader.canRead() && reader.peek() == ' ') {
				reader.skip();
				WorldCoordinate worldCoordinate3 = WorldCoordinate.parseDouble(reader, centerCorrect);
				return new ClientWorldCoordinates(worldCoordinate, worldCoordinate2, worldCoordinate3);
			} else {
				reader.setCursor(i);
				throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
			}
		} else {
			reader.setCursor(i);
			throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
		}
	}

	public static ClientWorldCoordinates absolute(double x, double y, double z) {
		return new ClientWorldCoordinates(new WorldCoordinate(false, x), new WorldCoordinate(false, y), new WorldCoordinate(false, z));
	}

	public static ClientWorldCoordinates absolute(Vec2 vector) {
		return new ClientWorldCoordinates(new WorldCoordinate(false, vector.x), new WorldCoordinate(false, vector.y), new WorldCoordinate(true, 0.0));
	}

	/**
	 * A location with a delta of 0 for all values (equivalent to <code>~ ~ ~</code> or <code>~0 ~0 ~0</code>)
	 */
	public static ClientWorldCoordinates current() {
		return new ClientWorldCoordinates(new WorldCoordinate(true, 0.0), new WorldCoordinate(true, 0.0), new WorldCoordinate(true, 0.0));
	}

	public int hashCode() {
		int i = this.x.hashCode();
		i = 31 * i + this.y.hashCode();
		return 31 * i + this.z.hashCode();
	}
}
