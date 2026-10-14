package com.npjt25.pissmod;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;

public final class ModParticles {
	/** A single droplet of the stream. Flies ballistically and splashes on impact. */
	public static final ParticleType<ColorParticleOption> PEE_DROP = FabricParticleTypes.complex(ColorParticleOption::codec, ColorParticleOption::streamCodec);
	/** Small droplet thrown up where the stream lands. */
	public static final ParticleType<ColorParticleOption> PEE_SPLASH = FabricParticleTypes.complex(ColorParticleOption::codec, ColorParticleOption::streamCodec);

	private ModParticles() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, PissMod.id("pee_drop"), PEE_DROP);
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, PissMod.id("pee_splash"), PEE_SPLASH);
	}
}
