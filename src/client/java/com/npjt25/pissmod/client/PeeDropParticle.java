package com.npjt25.pissmod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;

import com.npjt25.pissmod.ModParticles;
import com.npjt25.pissmod.StreamPhysics;

/** One droplet of the stream. Follows the same ballistics as the server and splashes on whatever it hits. */
public class PeeDropParticle extends SingleQuadParticle {
	private final boolean splash;
	private final float red;
	private final float green;
	private final float blue;

	protected PeeDropParticle(ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ,
			TextureAtlasSprite sprite, ColorParticleOption color, boolean splash) {
		super(level, x, y, z, velocityX, velocityY, velocityZ, sprite);
		this.splash = splash;
		// The base constructor randomizes velocity; the stream needs it exact.
		this.xd = velocityX;
		this.yd = velocityY;
		this.zd = velocityZ;
		this.gravity = 1.0F;
		this.friction = (float) StreamPhysics.DRAG;
		this.hasPhysics = true;
		this.lifetime = splash ? 5 + this.random.nextInt(6) : StreamPhysics.MAX_STEPS;
		this.quadSize = splash ? 0.03F + this.random.nextFloat() * 0.02F : 0.05F + this.random.nextFloat() * 0.025F;
		this.red = color.getRed();
		this.green = color.getGreen();
		this.blue = color.getBlue();
		float shade = 0.88F + this.random.nextFloat() * 0.12F;
		this.setColor(this.red * shade, this.green * shade, this.blue * shade);
		this.alpha = 0.85F;
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	@Override
	public void tick() {
		// Where the droplet would end up this tick if nothing were in the way.
		double expectedX = this.xd;
		double expectedY = this.yd - 0.04 * this.gravity;
		double expectedZ = this.zd;
		super.tick();

		if (!this.isAlive() || this.splash) {
			return;
		}

		boolean blocked = Math.abs(this.x - this.xo - expectedX) > 1.0E-4
				|| Math.abs(this.y - this.yo - expectedY) > 1.0E-4
				|| Math.abs(this.z - this.zo - expectedZ) > 1.0E-4;

		if (!this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).isEmpty()) {
			this.remove();
		} else if (this.onGround || blocked) {
			if (this.random.nextInt(3) == 0) {
				ColorParticleOption options = ColorParticleOption.create(ModParticles.PEE_SPLASH, this.red, this.green, this.blue);
				this.level.addParticle(options, this.x, this.y + 0.02, this.z,
						(this.random.nextDouble() - 0.5) * 0.12, 0.04 + this.random.nextDouble() * 0.08, (this.random.nextDouble() - 0.5) * 0.12);
			}

			this.remove();
		}
	}

	public record Provider(FabricSpriteSet sprites, boolean splash) implements ParticleProvider<ColorParticleOption> {
		@Override
		public Particle createParticle(ColorParticleOption options, ClientLevel level, double x, double y, double z,
				double velocityX, double velocityY, double velocityZ, RandomSource random) {
			return new PeeDropParticle(level, x, y, z, velocityX, velocityY, velocityZ, this.sprites.get(random), options, this.splash);
		}
	}
}
