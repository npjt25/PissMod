package com.npjt25.pissmod.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import com.npjt25.pissmod.ModSounds;
import com.npjt25.pissmod.StreamPhysics;

/** The continuous stream sound. Follows the player, fades in, and fades out when they stop. */
public class PeeLoopSound extends AbstractTickableSoundInstance {
	private final LivingEntity entity;
	private float targetVolume;
	private boolean fadingOut;

	public PeeLoopSound(LivingEntity entity, int power) {
		super(ModSounds.PEE_LOOP, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
		this.entity = entity;
		this.looping = true;
		this.delay = 0;
		this.volume = 0.05F;
		this.setPower(power);
		this.updatePosition();
	}

	public LivingEntity entity() {
		return this.entity;
	}

	public void setPower(int power) {
		float strength = StreamPhysics.strength(power);
		this.targetVolume = 0.7F + 0.5F * strength;
		this.pitch = 0.95F + 0.3F * strength;
	}

	public void fadeOut() {
		this.fadingOut = true;
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public boolean canPlaySound() {
		return !this.entity.isSilent();
	}

	@Override
	public void tick() {
		if (this.entity.isRemoved()) {
			this.stop();
			return;
		}

		this.updatePosition();

		if (this.fadingOut) {
			this.volume -= 0.15F;

			if (this.volume <= 0.0F) {
				this.volume = 0.0F;
				this.stop();
			}
		} else {
			this.volume = Math.min(this.targetVolume, this.volume + 0.12F);
		}
	}

	private void updatePosition() {
		this.x = this.entity.getX();
		this.y = this.entity.getY() + this.entity.getBbHeight() * 0.375;
		this.z = this.entity.getZ();
	}
}
