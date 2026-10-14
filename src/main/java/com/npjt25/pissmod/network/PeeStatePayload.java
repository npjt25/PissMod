package com.npjt25.pissmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.npjt25.pissmod.PeePhase;
import com.npjt25.pissmod.PissMod;

/** Server -> clients: a player's pee phase changed (drives sounds and the visible stream). */
public record PeeStatePayload(int entityId, PeePhase phase, int power, int color) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PeeStatePayload> TYPE = new CustomPacketPayload.Type<>(PissMod.id("state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PeeStatePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PeeStatePayload::entityId,
			PeePhase.STREAM_CODEC, PeeStatePayload::phase,
			ByteBufCodecs.VAR_INT, PeeStatePayload::power,
			ByteBufCodecs.INT, PeeStatePayload::color,
			PeeStatePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
