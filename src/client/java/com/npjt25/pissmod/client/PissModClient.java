package com.npjt25.pissmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;

import com.npjt25.pissmod.ModParticles;
import com.npjt25.pissmod.PissConfig;
import com.npjt25.pissmod.PissMod;
import com.npjt25.pissmod.network.PeeInputPayload;
import com.npjt25.pissmod.network.PeeStatePayload;

public class PissModClient implements ClientModInitializer {
	private static KeyMapping peeKey;
	private static KeyMapping configKey;

	private static boolean sentHolding;
	private static int sentPower = -1;
	private static int sentColor = -1;
	private static boolean warnedMissingServer;

	public static KeyMapping peeKey() {
		return peeKey;
	}

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(PissMod.id("main"));
		peeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.pissmod.pee", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, category));
		configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.pissmod.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));

		ParticleProviderRegistry.getInstance().register(ModParticles.PEE_DROP, sprites -> new PeeDropParticle.Provider(sprites, false));
		ParticleProviderRegistry.getInstance().register(ModParticles.PEE_SPLASH, sprites -> new PeeDropParticle.Provider(sprites, true));

		ClientPlayNetworking.registerGlobalReceiver(PeeStatePayload.TYPE, (payload, context) -> ClientStreams.onState(context.client(), payload));
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
			ClientStreams.clear();
			sentHolding = false;
			warnedMissingServer = false;
		});
		ClientTickEvents.END_CLIENT_TICK.register(PissModClient::tick);
	}

	private static void tick(Minecraft client) {
		while (configKey.consumeClick()) {
			if (client.gui.screen() == null) {
				client.gui.setScreen(new PissConfigScreen(null));
			}
		}

		sendInput(client);
		ClientStreams.tick(client);
	}

	/** Tells the server when the pee key goes down/up (and when settings change mid-stream). */
	private static void sendInput(Minecraft client) {
		if (client.player == null || client.getConnection() == null) {
			sentHolding = false;
			return;
		}

		PissConfig config = PissConfig.get();
		boolean holding = peeKey.isDown();
		boolean settingsChanged = config.streamPower != sentPower || config.colorRgb() != sentColor;

		if (holding == sentHolding && !(holding && settingsChanged)) {
			return;
		}

		if (ClientPlayNetworking.canSend(PeeInputPayload.TYPE)) {
			ClientPlayNetworking.send(new PeeInputPayload(holding, config.streamPower, config.colorRgb()));
		} else if (holding && !warnedMissingServer) {
			warnedMissingServer = true;
			client.player.sendOverlayMessage(Component.translatable("message.pissmod.server_missing"));
		}

		sentHolding = holding;
		sentPower = config.streamPower;
		sentColor = config.colorRgb();
	}
}
