package io.github.lucaargolo.exporter.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class ClientLocalCoordinates implements ClientCoordinates {
	private final double left;
	private final double up;
	private final double forwards;

	public ClientLocalCoordinates(double left, double up, double forwards) {
		this.left = left;
		this.up = up;
		this.forwards = forwards;
	}

	@Override
	public Vec3 getPosition(FabricClientCommandSource source) {
		Vec2 vec2 = source.getRotation();
		Vec3 vec3 = source.getPosition();
		float f = Mth.cos((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
		float g = Mth.sin((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
		float h = Mth.cos(-vec2.x * (float) (Math.PI / 180.0));
		float i = Mth.sin(-vec2.x * (float) (Math.PI / 180.0));
		float j = Mth.cos((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
		float k = Mth.sin((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
		Vec3 vec32 = new Vec3(f * h, i, g * h);
		Vec3 vec33 = new Vec3(f * j, k, g * j);
		Vec3 vec34 = vec32.cross(vec33).scale(-1.0);
		double d = vec32.x * this.forwards + vec33.x * this.up + vec34.x * this.left;
		double e = vec32.y * this.forwards + vec33.y * this.up + vec34.y * this.left;
		double l = vec32.z * this.forwards + vec33.z * this.up + vec34.z * this.left;
		return new Vec3(vec3.x + d, vec3.y + e, vec3.z + l);
	}

	@Override
	public Vec2 getRotation(FabricClientCommandSource source) {
		return Vec2.ZERO;
	}

	@Override
	public boolean isXRelative() {
		return true;
	}

	@Override
	public boolean isYRelative() {
		return true;
	}

	@Override
	public boolean isZRelative() {
		return true;
	}

	public static ClientLocalCoordinates parse(StringReader reader) throws CommandSyntaxException {
		int i = reader.getCursor();
		double d = readDouble(reader, i);
		if (reader.canRead() && reader.peek() == ' ') {
			reader.skip();
			double e = readDouble(reader, i);
			if (reader.canRead() && reader.peek() == ' ') {
				reader.skip();
				double f = readDouble(reader, i);
				return new ClientLocalCoordinates(d, e, f);
			} else {
				reader.setCursor(i);
				throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
			}
		} else {
			reader.setCursor(i);
			throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
		}
	}

	private static double readDouble(StringReader reader, int start) throws CommandSyntaxException {
		if (!reader.canRead()) {
			throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(reader);
		} else if (reader.peek() != LocalCoordinates.PREFIX_LOCAL_COORDINATE) {
			reader.setCursor(start);
			throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(reader);
		} else {
			reader.skip();
			return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
		}
	}

	public boolean equals(Object object) {
		if (this == object) {
			return true;
		} else if (!(object instanceof ClientLocalCoordinates localCoordinates)) {
			return false;
		} else {
			return this.left == localCoordinates.left && this.up == localCoordinates.up && this.forwards == localCoordinates.forwards;
		}
	}

	public int hashCode() {
		return Objects.hash(this.left, this.up, this.forwards);
	}
}
