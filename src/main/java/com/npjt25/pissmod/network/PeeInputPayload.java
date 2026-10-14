package com.npjt25.pissmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.npjt25.pissmod.PissMod;

/** Client -> server: whether the pee key is held, plus the player's stream settings. */
public record PeeInputPayload(boolean holding, int power, int color) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PeeInputPayload> TYPE = new CustomPacketPayload.Type<>(PissMod.id("input"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PeeInputPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, PeeInputPayload::holding,
			ByteBufCodecs.VAR_INT, PeeInputPayload::power,
			ByteBufCodecs.INT, PeeInputPayload::color,
			PeeInputPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
