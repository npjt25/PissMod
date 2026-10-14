package com.npjt25.pissmod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Stored in {@code config/pissmod.json}.
 *
 * <p>Client options are the player's own preferences; server options are enforced by whoever hosts the world
 * (in singleplayer that is you, so both halves apply).
 */
public final class PissConfig {
	public static final int MAX_POWER = 50;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static PissConfig instance = new PissConfig();

	// ---- client ----
	/** 0 = normal peeing, 50 = firehose. */
	public int streamPower = 0;
	/** Stream color as a hex RGB string. */
	public String streamColor = "#F2D53C";

	// ---- server ----
	/** Highest power any player is allowed to use on this server. */
	public int maxStreamPower = MAX_POWER;
	/** Stream power at which it starts hurting whoever gets hit. */
	public int damageThreshold = 30;
	/** Damage per hit (in half-hearts) at exactly {@link #damageThreshold}. */
	public float damageAtThreshold = 1.0F;
	/** Damage per hit (in half-hearts) at power 50. */
	public float damageAtMaxPower = 4.0F;
	/** Whether a strong stream shoves mobs and players around. */
	public boolean knockback = true;
	/** Whether the stream puts out fire, burning mobs and campfires. */
	public boolean extinguishFire = true;

	public static PissConfig get() {
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(PissMod.MOD_ID + ".json");
	}

	public static void load() {
		Path path = path();

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				PissConfig loaded = GSON.fromJson(reader, PissConfig.class);

				if (loaded != null) {
					instance = loaded;
				}
			} catch (Exception e) {
				PissMod.LOGGER.error("Failed to read {}, using defaults", path, e);
			}
		}

		instance.sanitize();
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(path().getParent());

			try (Writer writer = Files.newBufferedWriter(path())) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			PissMod.LOGGER.error("Failed to write {}", path(), e);
		}
	}

	private void sanitize() {
		streamPower = clampPower(streamPower);
		maxStreamPower = clampPower(maxStreamPower);
		damageThreshold = Math.max(0, Math.min(MAX_POWER + 1, damageThreshold));
		damageAtThreshold = Math.max(0, damageAtThreshold);
		damageAtMaxPower = Math.max(0, damageAtMaxPower);
		streamColor = String.format("#%06X", colorRgb());
	}

	public static int clampPower(int power) {
		return Math.max(0, Math.min(MAX_POWER, power));
	}

	public int colorRgb() {
		try {
			return Integer.parseInt(streamColor.replace("#", "").trim(), 16) & 0xFFFFFF;
		} catch (Exception e) {
			return 0xF2D53C;
		}
	}

	public void setColorRgb(int rgb) {
		streamColor = String.format("#%06X", rgb & 0xFFFFFF);
	}

	/** Damage dealt per hit for a stream of the given power, or 0 if it is below the threshold. */
	public float damageFor(int power) {
		if (power < damageThreshold) {
			return 0.0F;
		}

		if (damageThreshold >= MAX_POWER) {
			return damageAtMaxPower;
		}

		float t = (power - damageThreshold) / (float) (MAX_POWER - damageThreshold);
		return damageAtThreshold + (damageAtMaxPower - damageAtThreshold) * t;
	}
}
