package com.npjt25.pissmod.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.npjt25.pissmod.ModParticles;
import com.npjt25.pissmod.ModSounds;
import com.npjt25.pissmod.PeePhase;
import com.npjt25.pissmod.StreamPhysics;
import com.npjt25.pissmod.network.PeeStatePayload;

/** Client-side view of everyone who is currently peeing: plays their sounds and draws their streams. */
public final class ClientStreams {
	private static final Int2ObjectMap<Stream> STREAMS = new Int2ObjectOpenHashMap<>();
	/** pee_start.ogg is 1.3 s long; start the loop a little before it ends so they crossfade. */
	private static final int LOOP_DELAY_TICKS = 20;

	private ClientStreams() {
	}

	private static final class Stream {
		PeePhase phase = PeePhase.IDLE;
		int phaseTicks;
		int power;
		int color;
		PeeLoopSound loop;

		void startLoop(Minecraft client, LivingEntity entity, int delayTicks) {
			if (this.loop != null && !this.loop.isStopped() && this.loop.entity() == entity) {
				return;
			}

			stopLoop();
			this.loop = new PeeLoopSound(entity, this.power);
			client.getSoundManager().playDelayed(this.loop, delayTicks);
		}

		void stopLoop() {
			if (this.loop != null) {
				this.loop.fadeOut();
				this.loop = null;
			}
		}
	}

	public static void onState(Minecraft client, PeeStatePayload payload) {
		if (client.level == null) {
			return;
		}

		Stream stream = STREAMS.get(payload.entityId());

		if (payload.phase() == PeePhase.IDLE) {
			if (stream != null) {
				stream.stopLoop();
				STREAMS.remove(payload.entityId());
			}

			return;
		}

		if (stream == null) {
			stream = new Stream();
			STREAMS.put(payload.entityId(), stream);
		}

		stream.power = payload.power();
		stream.color = payload.color();

		if (stream.loop != null) {
			stream.loop.setPower(stream.power);
		}

		PeePhase previous = stream.phase;

		if (previous == payload.phase()) {
			return;
		}

		stream.phase = payload.phase();
		stream.phaseTicks = 0;
		Entity entity = client.level.getEntity(payload.entityId());

		if (!(entity instanceof LivingEntity living)) {
			return;
		}

		float strength = StreamPhysics.strength(stream.power);
		float streamVolume = 0.8F + 0.4F * strength;
		float streamPitch = 0.95F + 0.3F * strength;

		switch (stream.phase) {
			case UNZIPPING -> play(entity, ModSounds.UNZIP, 0.9F, 0.95F + entity.getRandom().nextFloat() * 0.1F);
			case PEEING -> {
				if (previous == PeePhase.FINISHING) {
					stream.startLoop(client, living, 0);
				} else {
					// The start sound ramps up into a full stream; the loop takes over near its end.
					play(entity, ModSounds.PEE_START, streamVolume, streamPitch);
					stream.startLoop(client, living, LOOP_DELAY_TICKS);
				}
			}
			case FINISHING -> {
				stream.stopLoop();
				play(entity, ModSounds.PEE_END, streamVolume, streamPitch);
			}
			case ZIPPING -> play(entity, ModSounds.ZIP, 0.9F, 0.95F + entity.getRandom().nextFloat() * 0.1F);
			case IDLE -> {
			}
		}
	}

	public static void tick(Minecraft client) {
		ClientLevel level = client.level;

		if (level == null) {
			clear();
			return;
		}

		if (client.isPaused()) {
			return;
		}

		ObjectIterator<Int2ObjectMap.Entry<Stream>> iterator = STREAMS.int2ObjectEntrySet().iterator();

		while (iterator.hasNext()) {
			Int2ObjectMap.Entry<Stream> entry = iterator.next();
			Stream stream = entry.getValue();
			Entity entity = level.getEntity(entry.getIntKey());

			if (!(entity instanceof LivingEntity living) || entity.isRemoved()) {
				stream.stopLoop();
				iterator.remove();
				continue;
			}

			stream.phaseTicks++;

			if (stream.phase == PeePhase.PEEING) {
				stream.startLoop(client, living, 0);
				spawnStream(level, living, stream, stream.power, 1.0F, 1.0F);
			} else if (stream.phase == PeePhase.FINISHING) {
				// Taper off, then a few last dribbles.
				float t = stream.phaseTicks / (float) PeePhase.FINISHING.duration;

				if (t < 0.55F) {
					float fade = 1.0F - t / 0.55F;
					spawnStream(level, living, stream, Mth.floor(stream.power * fade * fade), 0.35F + 0.65F * fade, 0.3F + 0.7F * fade);
				} else if (living.getRandom().nextInt(3) == 0) {
					spawnStream(level, living, stream, 0, 0.15F, 0.01F);
				}
			}
		}
	}

	private static void spawnStream(ClientLevel level, LivingEntity entity, Stream stream, int power, float speedScale, float countScale) {
		RandomSource random = entity.getRandom();
		float strength = StreamPhysics.strength(power);
		double speed = StreamPhysics.speed(power) * speedScale;
		int count = Math.max(1, Mth.ceil(speed / 0.06 * countScale));
		double jitter = 0.006 + 0.01 * (1.0F - strength);
		ColorParticleOption options = ColorParticleOption.create(ModParticles.PEE_DROP, 0xFF000000 | stream.color);

		for (int i = 0; i < count; i++) {
			// Spread the droplets over the tick so the stream looks continuous instead of beaded.
			float partial = i / (float) count;
			Vec3 origin = StreamPhysics.origin(entity, partial);
			Vec3 velocity = StreamPhysics.direction(entity, partial, power).scale(speed)
					.add(random.nextGaussian() * jitter, random.nextGaussian() * jitter, random.nextGaussian() * jitter);
			Vec3 pos = origin.add(velocity.scale(1.0F - partial));
			level.addParticle(options, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
		}
	}

	private static void play(Entity entity, SoundEvent sound, float volume, float pitch) {
		Minecraft.getInstance().getSoundManager().play(new EntityBoundSoundInstance(sound, SoundSource.PLAYERS, volume, pitch, entity, entity.getRandom().nextLong()));
	}

	/** Number of players this client is currently showing a stream (or unzip/zip) for. */
	public static int activeCount() {
		return STREAMS.size();
	}

	public static void clear() {
		for (Stream stream : STREAMS.values()) {
			stream.stopLoop();
		}

		STREAMS.clear();
	}
}
