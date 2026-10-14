package com.npjt25.pissmod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.npjt25.pissmod.network.PeeInputPayload;
import com.npjt25.pissmod.network.PeeStatePayload;
import com.npjt25.pissmod.server.PeeManager;

public class PissMod implements ModInitializer {
	public static final String MOD_ID = "pissmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PissConfig.load();
		ModSounds.register();
		ModParticles.register();

		PayloadTypeRegistry.serverboundPlay().register(PeeInputPayload.TYPE, PeeInputPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PeeStatePayload.TYPE, PeeStatePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(PeeInputPayload.TYPE, (payload, context) ->
				PeeManager.onInput(context.player(), payload));

		ServerTickEvents.END_SERVER_TICK.register(PeeManager::tick);
		EntityTrackingEvents.START_TRACKING.register(PeeManager::onStartTracking);
		ServerPlayerEvents.LEAVE.register(PeeManager::reset);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> PeeManager.reset(oldPlayer));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> PeeManager.clear());

		LOGGER.info("PissMod loaded. Unzip responsibly.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
