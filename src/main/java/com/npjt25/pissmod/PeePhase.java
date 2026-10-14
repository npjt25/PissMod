package com.npjt25.pissmod;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum PeePhase {
	/** Zipped up, nothing happening. */
	IDLE(0),
	/** Unzip sound playing, stream about to start. */
	UNZIPPING(12),
	/** Stream active. */
	PEEING(0),
	/** Stream tapering off with the last few dribbles. */
	FINISHING(26),
	/** Zip sound playing. */
	ZIPPING(10);

	public static final StreamCodec<ByteBuf, PeePhase> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[Math.floorMod(i, values().length)], PeePhase::ordinal);

	/** How long the phase lasts in ticks (0 = until the player lets go of the key). */
	public final int duration;

	PeePhase(int duration) {
		this.duration = duration;
	}
}
