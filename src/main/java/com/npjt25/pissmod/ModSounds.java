package com.npjt25.pissmod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final SoundEvent UNZIP = create("unzip");
	public static final SoundEvent ZIP = create("zip");
	public static final SoundEvent PEE_START = create("pee_start");
	public static final SoundEvent PEE_LOOP = create("pee_loop");
	public static final SoundEvent PEE_END = create("pee_end");

	private ModSounds() {
	}

	private static SoundEvent create(String name) {
		return SoundEvent.createVariableRangeEvent(PissMod.id(name));
	}

	public static void register() {
		for (SoundEvent sound : new SoundEvent[] {UNZIP, ZIP, PEE_START, PEE_LOOP, PEE_END}) {
			Identifier id = sound.location();
			Registry.register(BuiltInRegistries.SOUND_EVENT, id, sound);
		}
	}
}
