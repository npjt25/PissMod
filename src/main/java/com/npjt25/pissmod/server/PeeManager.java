package com.npjt25.pissmod.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.npjt25.pissmod.PeePhase;
import com.npjt25.pissmod.PissConfig;
import com.npjt25.pissmod.network.PeeInputPayload;
import com.npjt25.pissmod.network.PeeStatePayload;

/**
 * Server-authoritative pee state machine:
 * key down -> UNZIPPING -> PEEING (while held) -> key up -> FINISHING -> ZIPPING -> IDLE.
 */
public final class PeeManager {
	/** Ticks between stream hit checks while peeing. */
	private static final int HIT_INTERVAL = 3;
	private static final Map<UUID, State> STATES = new HashMap<>();

	private PeeManager() {
	}

	private static final class State {
		PeePhase phase = PeePhase.IDLE;
		int ticks;
		boolean holding;
		int power;
		int color;
	}

	public static void onInput(ServerPlayer player, PeeInputPayload payload) {
		State state = STATES.get(player.getUUID());

		if (state == null) {
			if (!payload.holding() || !player.isAlive() || player.isSpectator()) {
				return;
			}

			state = new State();
			STATES.put(player.getUUID(), state);
		}

		int power = Math.min(PissConfig.clampPower(payload.power()), PissConfig.get().maxStreamPower);
		int color = payload.color() & 0xFFFFFF;
		boolean settingsChanged = power != state.power || color != state.color;
		state.power = power;
		state.color = color;
		state.holding = payload.holding();

		if (state.holding) {
			switch (state.phase) {
				case IDLE, ZIPPING -> setPhase(player, state, PeePhase.UNZIPPING);
				case FINISHING -> setPhase(player, state, PeePhase.PEEING);
				case PEEING -> {
					if (settingsChanged) {
						broadcast(player, state);
					}
				}
				case UNZIPPING -> {
				}
			}
		} else if (state.phase == PeePhase.PEEING) {
			setPhase(player, state, PeePhase.FINISHING);
		}
	}

	public static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, State>> iterator = STATES.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, State> entry = iterator.next();
			State state = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

			if (player == null) {
				iterator.remove();
				continue;
			}

			if (!player.isAlive() || player.isSpectator()) {
				setPhase(player, state, PeePhase.IDLE);
				iterator.remove();
				continue;
			}

			state.ticks++;

			switch (state.phase) {
				case UNZIPPING, FINISHING -> {
					if (state.ticks >= state.phase.duration) {
						setPhase(player, state, state.holding ? PeePhase.PEEING : PeePhase.ZIPPING);
					}
				}
				case PEEING -> {
					if (state.ticks % HIT_INTERVAL == 0) {
						StreamHits.simulate(player, state.power, state.color);
					}
				}
				case ZIPPING -> {
					if (state.ticks >= state.phase.duration) {
						setPhase(player, state, PeePhase.IDLE);
						iterator.remove();
					}
				}
				case IDLE -> iterator.remove();
			}
		}
	}

	/** A player came into view of someone: tell them if that player is mid-pee. */
	public static void onStartTracking(Entity tracked, ServerPlayer viewer) {
		if (tracked instanceof ServerPlayer player) {
			State state = STATES.get(player.getUUID());

			if (state != null && state.phase != PeePhase.IDLE) {
				send(viewer, payload(player, state));
			}
		}
	}

	public static void reset(ServerPlayer player) {
		State state = STATES.remove(player.getUUID());

		if (state != null && state.phase != PeePhase.IDLE) {
			state.phase = PeePhase.IDLE;
			broadcast(player, state);
		}
	}

	public static void clear() {
		STATES.clear();
	}

	private static void setPhase(ServerPlayer player, State state, PeePhase phase) {
		if (state.phase == phase) {
			return;
		}

		state.phase = phase;
		state.ticks = 0;
		broadcast(player, state);
	}

	private static PeeStatePayload payload(ServerPlayer player, State state) {
		return new PeeStatePayload(player.getId(), state.phase, state.power, state.color);
	}

	private static void broadcast(ServerPlayer player, State state) {
		PeeStatePayload payload = payload(player, state);
		send(player, payload);

		for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
			if (viewer != player) {
				send(viewer, payload);
			}
		}
	}

	private static void send(ServerPlayer player, PeeStatePayload payload) {
		if (ServerPlayNetworking.canSend(player, PeeStatePayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
