package com.npjt25.pissmod;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Shared ballistics so the stream the client draws and the stream the server uses for hits line up.
 * Units are blocks and ticks; the step order matches vanilla particle movement.
 */
public final class StreamPhysics {
	/** Downward acceleration per tick (same as most vanilla particles). */
	public static final double GRAVITY = 0.04;
	/** Velocity multiplier applied after every step. */
	public static final double DRAG = 0.99;
	/** How many ticks a droplet lives before it is discarded. */
	public static final int MAX_STEPS = 80;

	private StreamPhysics() {
	}

	/** 0..1 */
	public static float strength(int power) {
		return PissConfig.clampPower(power) / (float) PissConfig.MAX_POWER;
	}

	/** Initial droplet speed in blocks per tick: a gentle arc at 0, a firehose at 50. */
	public static double speed(int power) {
		return 0.3 + 1.05 * strength(power);
	}

	/** World position of the player's crotch. */
	public static Vec3 origin(LivingEntity entity, float partialTick) {
		float bodyYaw = Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot) * Mth.DEG_TO_RAD;
		double forwardX = -Mth.sin(bodyYaw);
		double forwardZ = Mth.cos(bodyYaw);
		Vec3 feet = entity.getPosition(partialTick);
		double height = entity.getBbHeight() * 0.375;
		return new Vec3(feet.x + forwardX * 0.22, feet.y + height, feet.z + forwardZ * 0.22);
	}

	/**
	 * Aim direction. Like the Gmod addon, the stream is lifted above where you look so that looking
	 * straight ahead gives a nice arc. Stronger streams are lifted less so they go where you aim.
	 */
	public static Vec3 direction(LivingEntity entity, float partialTick, int power) {
		Vec3 look = entity.getViewVector(partialTick);
		double lift = 0.5 - 0.35 * strength(power);
		return new Vec3(look.x, look.y + lift * (1.0 + look.y), look.z).normalize();
	}

	public static Vec3 initialVelocity(LivingEntity entity, float partialTick, int power) {
		return direction(entity, partialTick, power).scale(speed(power));
	}
}
