package com.npjt25.pissmod.server;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.npjt25.pissmod.ModParticles;
import com.npjt25.pissmod.PissConfig;
import com.npjt25.pissmod.PissMod;
import com.npjt25.pissmod.StreamPhysics;

/** Traces the stream's arc on the server to find what it lands on. */
public final class StreamHits {
	public static final ResourceKey<DamageType> PISS_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, PissMod.id("piss"));
	/** The stream can't hit its owner until it has travelled this many ticks (so it can come back down on you). */
	private static final int SELF_IMMUNE_STEPS = 10;

	private StreamHits() {
	}

	public static void simulate(ServerPlayer shooter, int power, int color) {
		ServerLevel level = (ServerLevel) shooter.level();
		Vec3 pos = StreamPhysics.origin(shooter, 1.0F);
		Vec3 velocity = StreamPhysics.initialVelocity(shooter, 1.0F, power);

		for (int step = 0; step < StreamPhysics.MAX_STEPS; step++) {
			velocity = velocity.add(0.0, -StreamPhysics.GRAVITY, 0.0);
			Vec3 next = pos.add(velocity);
			BlockHitResult blockHit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, shooter));
			boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
			Vec3 end = hitBlock ? blockHit.getLocation() : next;

			EntityHit entityHit = findEntity(level, shooter, pos, end, step >= SELF_IMMUNE_STEPS);

			if (entityHit != null) {
				onEntityHit(level, shooter, entityHit, velocity, power, color);
				return;
			}

			if (hitBlock) {
				onBlockHit(level, blockHit);
				return;
			}

			pos = next;
			velocity = velocity.scale(StreamPhysics.DRAG);
		}
	}

	private record EntityHit(LivingEntity entity, Vec3 location) {
	}

	private static @Nullable EntityHit findEntity(ServerLevel level, ServerPlayer shooter, Vec3 from, Vec3 to, boolean canHitSelf) {
		AABB area = new AABB(from, to).inflate(0.5);
		EntityHit best = null;
		double bestDistance = Double.MAX_VALUE;

		for (Entity entity : level.getEntities((Entity) null, area, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
			if (entity == shooter && !canHitSelf) {
				continue;
			}

			AABB box = entity.getBoundingBox().inflate(0.1);
			Optional<Vec3> clip = box.clip(from, to);
			Vec3 location = clip.orElse(box.contains(from) ? from : null);

			if (location != null) {
				double distance = from.distanceToSqr(location);

				if (distance < bestDistance) {
					bestDistance = distance;
					best = new EntityHit((LivingEntity) entity, location);
				}
			}
		}

		return best;
	}

	private static void onEntityHit(ServerLevel level, ServerPlayer shooter, EntityHit hit, Vec3 velocity, int power, int color) {
		PissConfig config = PissConfig.get();
		LivingEntity target = hit.entity();

		if (config.extinguishFire && target.isOnFire()) {
			target.clearFire();
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.NEUTRAL, 0.7F, 1.6F + level.getRandom().nextFloat() * 0.4F);
		}

		if (target != shooter) {
			float damage = config.damageFor(power);

			if (damage > 0.0F) {
				DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(PISS_DAMAGE), shooter);
				target.hurtServer(level, source, damage);
			}

			if (config.knockback && power > 20) {
				Vec3 push = new Vec3(velocity.x, 0.0, velocity.z).normalize().scale((power - 20) / 30.0 * 0.12);
				target.push(push.x, 0.01, push.z);
				target.hurtMarked = true;
			}
		}

		Vec3 at = hit.location();
		level.sendParticles(ColorParticleOption.create(ModParticles.PEE_SPLASH, 0xFF000000 | color), at.x, at.y, at.z, 2 + power / 10, 0.1, 0.1, 0.1, 0.05);
	}

	private static void onBlockHit(ServerLevel level, BlockHitResult hit) {
		if (!PissConfig.get().extinguishFire) {
			return;
		}

		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);

		if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
			level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), 11);
			fizz(level, pos);
		}

		BlockPos side = pos.relative(hit.getDirection());

		if (level.getBlockState(side).is(BlockTags.FIRE)) {
			level.removeBlock(side, false);
			fizz(level, side);
		}
	}

	private static void fizz(ServerLevel level, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.8F);
	}
}
